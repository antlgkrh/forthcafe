# forthcafe
# 서비스 시나리오
### 기능적 요구사항
1. 고객이 메뉴를 주문한다.
2. 고객이 결재한다
3. 결재가 완료되면 주문 내역을 보낸다
4. 매장에서 메뉴 완성 후 배달을 시작한다
5. 주문 상태를 고객이 조회 할 수 있다
6. 고객이 주문을 취소 할 수 있다
7. 결재 취소시 배송이 같이 취소 되어야 한다
8. 고객이 리뷰를 작성한다. 
9. 작성한 리뷰를 고객이 볼 수 있다.


### 비기능적 요구사항
1. 트랜젝션
   1. 리뷰가 완료되지 않은 주문건은 아예 배송 완료가 성립되지 않아야 한다. (Sync 호출)
2. 장애격리
   1. 리뷰조회에서 장애가 발송해도 리뷰작성은 24시간 할 수 있어야 한다 →Async(event-driven), Eventual Consistency
   2. 배송이 많아 리뷰작성이 과중되면 리뷰작성을 잠시 후에 하도록 유도한다 → Circuit breaker, fallback
3. 성능
   1. 고객이 리뷰결과를 주문시스템(프론트엔드)에서 확인할 수 있어야 한다 → CQRS

# Event Storming 결과

![EventStormingV1](https://github.com/bigot93/forthcafe/blob/main/images/eventingstorming_forthcafe.png)
![image](https://user-images.githubusercontent.com/30856023/109804773-fa6e3600-7c65-11eb-9fa5-297d6add8532.png)


# 헥사고날 아키텍처 다이어그램 도출
![image](https://user-images.githubusercontent.com/30856023/109972156-5acea780-7d3a-11eb-8da0-9d1cc901d8f3.png)


# 구현
분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각각의 포트넘버는 8081 ~ 8085, 8088 이다)
```
cd Order
mvn spring-boot:run  

cd Pay
mvn spring-boot:run

cd Delivery
mvn spring-boot:run 

cd MyPage
mvn spring-boot:run  

cd Review
mvn spring-boot:run 

cd gateway
mvn spring-boot:run 

```

## DDD 의 적용
msaez.io를 통해 구현한 Aggregate 단위로 Entity를 선언 후, 구현을 진행하였다.

**Review 서비스의 Review.java**
```java 
package forthcafe;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="Review_table")
public class Review {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String ordererName;
    private String menuName;
    private Long menuId;
    private Double price;
    private Integer quantity;
    private String status;
    private String reviewText;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOrdererName() {
        return ordererName;
    }

    public void setOrdererName(String ordererName) {
        this.ordererName = ordererName;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    public String getReviewText() {
        return reviewText;
    }


}

```

* Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다

**Review 서비스의 ReviewRepository.java**
```java
package forthcafe;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ReviewRepository extends PagingAndSortingRepository<Review, Long>{

}
```

DDD 적용 후 REST API의 테스트를 통하여 정상적으로 동작하는 것을 확인할 수 있었다.
* Review 호출 후 결과
```
http localhost:8085/reviews ordererName="kim" menuName="americano" menuId=1  price=100 quantity=3 status="Review"
```
![image](https://user-images.githubusercontent.com/30856023/109994049-44cbe180-7d50-11eb-88b5-f4728cb8f925.png)

# GateWay 적용
API GateWay를 통하여 마이크로 서비스들의 집입점을 통일할 수 있다. 다음과 같이 GateWay를 적용하였다.

```
server:
  port: 8088

---

spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: Order
          uri: http://localhost:8081
          predicates:
            - Path=/orders/** 
        - id: Pay
          uri: http://localhost:8082
          predicates:
            - Path=/pays/** 
        - id: Delivery
          uri: http://localhost:8083
          predicates:
            - Path=/deliveries/** 
        - id: MyPage
          uri: http://localhost:8084
          predicates:
            - Path= /myPages/**
        - id: Review
          uri: http://localhost:8085
          predicates:
            - Path=/reviews/** 
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true


---

spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: Order
          uri: http://Order:8080
          predicates:
            - Path=/orders/** 
        - id: Pay
          uri: http://Pay:8080
          predicates:
            - Path=/pays/** 
        - id: Delivery
          uri: http://Delivery:8080
          predicates:
            - Path=/deliveries/** 
        - id: MyPage
          uri: http://MyPage:8080
          predicates:
            - Path= /myPages/**
        - id: Review
          uri: http://Review:8080
          predicates:
            - Path=/reviews/**             
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

server:
  port: 8080

```
8088 port로 Review서비스 정상 호출

```
http localhost:8088/reviews
```

![image](https://user-images.githubusercontent.com/30856023/109919741-8632a180-7cfc-11eb-93bf-b377200d33f4.png)

# CQRS/saga/correlation
Materialized View를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이)도 내 서비스의 화면 구성과 잦은 조회가 가능하게 구현해 두었다. 본 프로젝트에서 View 역할은 MyPages 서비스가 수행한다.

* 주문(ordered) 실행 
```
http http://20.194.5.178:8080/orders ordererName="kim1" menuName="americano" menuId=1  price=100 quantity=3 status="Order"
```
![image](https://user-images.githubusercontent.com/30856023/109965913-f3612980-7d32-11eb-94c0-a7bdec8260fa.png)


* 주문 후 MyPage 
```
http http://20.194.5.178:8080/myPages
```
![image](https://user-images.githubusercontent.com/30856023/109965844-dd536900-7d32-11eb-8cb5-75bd37673b28.png)


위와 같이 주문을 하게되면 Order > Pay > Delivery > Review > MyPage로 주문이 Assigned 된다

리뷰가 완료 되면 Status가 Reviewed로 Update 되는 것을 볼 수 있다.

또한 Correlation을 Key를 활용하여 Id를 Key값을 하고 원하는 주문하고 서비스간의 공유가 이루어 졌다.

위 결과로 서로 다른 마이크로 서비스 간에 트랜잭션이 묶여 있음을 알 수 있다.

# 폴리글랏
Review 서비스의 DB와 MyPage의 DB를 다른 DB를 사용하여 폴리글랏을 만족시키고 있다.

**Review 의 pom.xml DB 설정 코드**

![증빙5](https://github.com/bigot93/forthcafe/blob/main/images/db_conf1.png)

**MyPage의 pom.xml DB 설정 코드**

![증빙6](https://github.com/bigot93/forthcafe/blob/main/images/db_conf2.png)

# 동기식 호출 과 Fallback 처리

분석단계에서의 조건 중 하나로 Delivery 와 Review 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 Rest Repository에 의해 노출되어있는 REST 서비스를 FeignClient를 이용하여 호출하도록 한다.

**Delivery 서비스 내 external.ReviewService**
```java
package forthcafe.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@FeignClient(name="Review", url= "${api.url.review}", fallback = ReviewServiceImpl.class)
//@FeignClient(name="Review", url= "${api.url.review}")
public interface ReviewService {

    @RequestMapping(method= RequestMethod.GET, path="/reviews", consumes = "application/json")
    public void review(@RequestBody Review review);

```

**동작 확인**

* Review 서비스 중지 상태에서 Delivery 요청시 오류 발생  
![image](https://user-images.githubusercontent.com/30856023/110054127-e299cd80-7d9d-11eb-93d0-1f6f66518d59.png)


* Review 서비스 정상 상태에서 Delivery 정상 동작 
![image](https://user-images.githubusercontent.com/30856023/110054320-3b696600-7d9e-11eb-8017-7f0d3cea69a8.png)



* Fallback 설정 

```
ReviewService.java

@FeignClient(name="Review", url= "${api.url.review}", fallback = ReviewServiceImpl.class)
//@FeignClient(name="Review", url= "${api.url.review}")
public interface ReviewService {

    @RequestMapping(method= RequestMethod.GET, path="/reviews", consumes = "application/json")
    public void review(@RequestBody Review review);

```

```
ReviewServiceImpl.java


@Service
public class ReviewServiceImpl implements ReviewService {

    // fallback message
    @Override
    public void review(Review review) {
        System.out.println("!!!!!!!!!!!!!!!!!!!!! Pay service is BUSY !!!!!!!!!!!!!!!!!!!!!");
        System.out.println("!!!!!!!!!!!!!!!!!!!!!   Try again later   !!!!!!!!!!!!!!!!!!!!!");

        System.out.println("!!!!!!!!!!!!!!!!!!!!! Pay service is BUSY !!!!!!!!!!!!!!!!!!!!!");
        System.out.println("!!!!!!!!!!!!!!!!!!!!!   Try again later   !!!!!!!!!!!!!!!!!!!!!");

        System.out.println("!!!!!!!!!!!!!!!!!!!!! Pay service is BUSY !!!!!!!!!!!!!!!!!!!!!");
        System.out.println("!!!!!!!!!!!!!!!!!!!!!   Try again later   !!!!!!!!!!!!!!!!!!!!!");
        
        System.out.println("!!!!!!!!!!!!!!!!!!!!! Pay service is BUSY !!!!!!!!!!!!!!!!!!!!!");
        System.out.println("!!!!!!!!!!!!!!!!!!!!!   Try again later   !!!!!!!!!!!!!!!!!!!!!");

        System.out.println("!!!!!!!!!!!!!!!!!!!!! Pay service is BUSY !!!!!!!!!!!!!!!!!!!!!");
        System.out.println("!!!!!!!!!!!!!!!!!!!!!   Try again later   !!!!!!!!!!!!!!!!!!!!!");    
    }

}
```

* Fallback 결과 Review 서비스 중지 상태에서 Delivery 요청시 로그 BUSY 표시 확인

![image](https://user-images.githubusercontent.com/30856023/110061794-88076e00-7dab-11eb-9d4a-59af32c3185a.png)


# 운영

## CI/CD
* 카프카 설치
```
- 헬름 설치
참고 : http://msaschool.io/operation/implementation/implementation-seven/
curl https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 > get_helm.sh
chmod 700 get_helm.sh
./get_helm.sh

- Azure Only
kubectl patch storageclass managed -p '{"metadata": {"annotations":{"storageclass.kubernetes.io/is-default-class":"true"}}}'

- 카프카 설치
kubectl --namespace kube-system create sa tiller      # helm 의 설치관리자를 위한 시스템 사용자 생성
kubectl create clusterrolebinding tiller --clusterrole cluster-admin --serviceaccount=kube-system:tiller

helm repo add incubator https://charts.helm.sh/incubator
helm repo update
kubectl create ns kafka
helm install my-kafka --namespace kafka incubator/kafka

kubectl get po -n kafka -o wide
```
* Topic 생성
```
kubectl -n kafka exec my-kafka-0 -- /usr/bin/kafka-topics --zookeeper my-kafka-zookeeper:2181 --topic forthcafe --create --partitions 1 --replication-factor 1
```
* Topic 확인
```
kubectl -n kafka exec -ti my-kafka-0 -- /usr/bin/kafka-console-producer --broker-list my-kafka:9092 --topic forthcafe
```
* 이벤트 발행하기
```
kubectl -n kafka exec -ti my-kafka-0 -- /usr/bin/kafka-console-producer --broker-list my-kafka:9092 --topic forthcafe
```
* 이벤트 수신하기
```
kubectl -n kafka exec -ti my-kafka-0 -- /usr/bin/kafka-console-consumer --bootstrap-server my-kafka:9092 --topic forthcafe --from-beginning
```

* 소스 가져오기
```
git clone https://github.com/antlgkrh/forthcafe.git
```

## ConfigMap
* deployment.yml 파일에 설정
```
env:
   - name: SYS_MODE
     valueFrom:
       configMapKeyRef:
         name: systemmode
         key: sysmode
```
* Configmap 생성, 정보 확인
```
kubectl create configmap systemmode --from-literal=sysmode=PRODUCT
kubectl get configmap systemmode -o yaml
```

![image](https://user-images.githubusercontent.com/30856023/109979266-0deecf00-7d42-11eb-9dce-ce332d3d19f2.png)


```
Review.Java

    public void onPrePersist(){
   // configMap 설정
        String sysEnv = System.getenv("SYS_MODE");
        if(sysEnv == null) sysEnv = "LOCAL";
        System.out.println("################## SYSTEM MODE: " + sysEnv);
        
     }
```

* Review 1건 추가후 로그 확인
```
kubectl logs {pod ID}
```
![image](https://user-images.githubusercontent.com/30856023/109983968-b141e300-7d46-11eb-9aac-1d35f68dc94d.png)



## Deploy / Pipeline

* build 하기
```
cd /forthcafe

cd Order
mvn package 

cd ..
cd Pay
mvn package

cd ..
cd Delivery
mvn package

cd ..
cd gateway
mvn package

cd ..
cd MyPage
mvn package

cd ..
cd Review
mvn package
```

* Azure 레지스트리에 도커 이미지 push, deploy, 서비스생성(방법1 : yml파일 이용한 deploy)
```
cd .. 
cd Order
az acr build --registry skuser09 --image skuser09.azurecr.io/order:v1 .
kubectl apply -f kubernetes/deployment.yml 
kubectl expose deploy order --type=ClusterIP --port=8080

cd .. 
cd Pay
az acr build --registry skuser09 --image skuser09.azurecr.io/pay:v1 .
kubectl apply -f kubernetes/deployment.yml 
kubectl expose deploy pay --type=ClusterIP --port=8080

cd .. 
cd Delivery
az acr build --registry skuser09 --image skuser09.azurecr.io/delivery:v1 .
kubectl apply -f kubernetes/deployment.yml 
kubectl expose deploy delivery --type=ClusterIP --port=8080


cd .. 
cd MyPage
az acr build --registry skuser09 --image skuser09.azurecr.io/mypage:v1 .
kubectl apply -f kubernetes/deployment.yml 
kubectl expose deploy mypage --type=ClusterIP --port=8080

cd .. 
cd Review
az acr build --registry skuser09 --image skuser09.azurecr.io/review:v1 .
kubectl apply -f kubernetes/deployment.yml 
kubectl expose deploy review --type=ClusterIP --port=8080

cd .. 
cd gateway
az acr build --registry skuser09 --image skuser09.azurecr.io/gateway:v1 .
kubectl create deploy gateway --image=skuser09.azurecr.io/gateway:v1
kubectl expose deploy gateway --type=LoadBalancer --port=8080
```


* Azure 레지스트리에 도커 이미지 push, deploy, 서비스생성(방법2)
```
cd ..
cd Order
az acr build --registry skuser09 --image skuser09.azurecr.io/order:v1 .
kubectl create deploy order --image=skuser09.azurecr.io/order:v1
kubectl expose deploy order --type=ClusterIP --port=8080

cd .. 
cd Pay
az acr build --registry skuser09 --image skuser09.azurecr.io/pay:v1 .
kubectl create deploy pay --image=skuser09.azurecr.io/pay:v1
kubectl expose deploy pay --type=ClusterIP --port=8080


cd .. 
cd Delivery
az acr build --registry skuser09 --image skuser09.azurecr.io/delivery:v1 .
kubectl create deploy delivery --image=skuser09.azurecr.io/delivery:v1
kubectl expose deploy delivery --type=ClusterIP --port=8080


cd .. 
cd gateway
az acr build --registry skuser09 --image skuser09.azurecr.io/gateway:v1 .
kubectl create deploy gateway --image=skuser09.azurecr.io/gateway:v1
kubectl expose deploy gateway --type=LoadBalancer --port=8080

cd .. 
cd MyPage
az acr build --registry skuser09 --image skuser09.azurecr.io/mypage:v1 .
kubectl create deploy mypage --image=skuser09.azurecr.io/mypage:v1
kubectl expose deploy mypage --type=ClusterIP --port=8080

cd .. 
cd Review
az acr build --registry skuser09 --image skuser09.azurecr.io/review:v1 .
kubectl create deploy review --image=skuser09.azurecr.io/review:v1
kubectl expose deploy review --type=ClusterIP --port=8080

kubectl logs {pod명}
```

* Service, Pod, Deploy 상태 확인

![image](https://user-images.githubusercontent.com/30856023/109967530-e2191c80-7d34-11eb-821b-1f8b50f6432a.png)


* deployment.yml  참고
```
1. image 설정
2. env 설정 (config Map) 
3. readiness 설정 (무정지 배포)
4. liveness 설정 (self-healing)
5. resource 설정 (autoscaling)
```
![image](https://user-images.githubusercontent.com/30856023/109967774-2f958980-7d35-11eb-9180-682fb065cba7.png)


## 서킷 브레이킹
* 서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 옵션을 사용하여 구현함
* Delivery -> Review 와의 Req/Res 연결에서 요청이 과도한 경우 CirCuit Breaker 통한 격리
* Hystrix 를 설정: 요청처리 쓰레드에서 처리시간이 610 밀리가 넘어서기 시작하여 어느정도 유지되면 CB 회로가 닫히도록 (요청을 빠르게 실패처리, 차단) 설정

```
// Delivery 서비스 application.yml

feign:
  hystrix:
    enabled: true

hystrix:
  command:
    default:
      execution.isolation.thread.timeoutInMilliseconds: 610
```


```
// Review 서비스 Review.java

    @PrePersist
    public void onPrePersist(){
        Reviewed reviewed = new Reviewed();
        BeanUtils.copyProperties(this, reviewed);
        // kafka push
        reviewed.publishAfterCommit();

        try {
             Thread.currentThread().sleep((long) (400 + Math.random() * 220));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

```

* 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인: 동시사용자 15명 10초 동안 실시
```
siege -c15 -t10S  -v --content-type "application/json" 'http://localhost:8083/deliveries POST {"memuId":2, "quantity":1}'
```
![image](https://user-images.githubusercontent.com/30856023/110055813-0579b100-7da1-11eb-9fe4-f3ce8a283873.png)

![image](https://user-images.githubusercontent.com/30856023/110056058-6c976580-7da1-11eb-85de-b6a3f627914f.png)


## 오토스케일 아웃
* 앞서 서킷 브레이커(CB) 는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 한다.

* Review 서비스 deployment.yml 설정
```
 resources:
            limits:
              cpu: 500m
            requests:
              cpu: 200m
```
* 다시 배포해준다.
```
cd ../Review
mvn package
az acr build --registry skuser09 --image skuser09.azurecr.io/review:v7 .
kubectl apply -f kubernetes/deployment.yml 
kubectl expose deploy review --type=ClusterIP --port=8080

```

* Review 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 15프로를 넘어서면 replica 를 10개까지 늘려준다

```
kubectl autoscale deploy review --min=1 --max=10 --cpu-percent=15
```

* /home/project/team/forthcafe/yaml/siege.yaml
```
apiVersion: v1
kind: Pod
metadata:
  name: siege
spec:
  containers:
  - name: siege
    image: apexacme/siege-nginx
```

* siege pod 생성
```
/home/project/team/forthcafe/yaml/kubectl apply -f siege.yaml
```

* siege를 활용해서 워크로드를 1000명, 1분간 걸어준다. (Cloud 내 siege pod에서 부하줄 것)
```
kubectl exec -it pod/siege -c siege -- /bin/bash
siege -c200 -t60S  -v --content-type "application/json" 'http://Review:8080/reviews POST {"memuId":2, "quantity":1}'
```

* 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다
```
kubectl get deploy review -w
```
![image](https://user-images.githubusercontent.com/30856023/109987974-6cb84680-7d4a-11eb-8f66-26ed6f044050.png)

```
kubectl get pod
```
![image](https://user-images.githubusercontent.com/30856023/109988025-7772db80-7d4a-11eb-8b2f-9321fd2bd240.png)




## 무정지 재배포 (Readiness Probe)

* 서비스 확인을 위해 배포 전 siege로 review 요청을 보낸다. 
```
 siege -c15 -t180S  -v --content-type "application/json" 'http://Review:8080/reviews POST {"memuId":2, "quantity":1}'
```

* Readiness Probe 제외 시 
![image](https://user-images.githubusercontent.com/30856023/110058532-d1ed5580-7da5-11eb-85da-f639678df2d2.png)

![image](https://user-images.githubusercontent.com/30856023/110058765-54761500-7da6-11eb-8144-6da3d12a6bfd.png)



* Readiness Probe 설정
 
* 배포전
![image](https://user-images.githubusercontent.com/30856023/110057773-84bcb400-7da4-11eb-9994-16c559adf7a0.png)


* 배포중
![image](https://user-images.githubusercontent.com/30856023/110058177-3f4cb680-7da5-11eb-9fb5-02602947f025.png)


* 배포후
![image](https://user-images.githubusercontent.com/30856023/110058360-8c308d00-7da5-11eb-97a6-13ea18ac693a.png)


* 배포 진행 중 서비스 확인
![image](https://user-images.githubusercontent.com/30856023/110059097-e9790e00-7da6-11eb-9f7f-f812f6521812.png)



## Self-healing (Liveness Probe)
* Review 서비스 deployment.yml   livenessProbe 설정을 port 8089로 변경 후 배포 하여 liveness probe 가 동작함을 확인 
```
    livenessProbe:
      httpGet:
        path: '/actuator/health'
        port: 8089
      initialDelaySeconds: 5
      periodSeconds: 5
```
![image](https://user-images.githubusercontent.com/30856023/110059834-5b058c00-7da8-11eb-9345-d7973c8a9017.png)
![image](https://user-images.githubusercontent.com/30856023/110060254-f860c000-7da8-11eb-91eb-96113cfed3de.png)

