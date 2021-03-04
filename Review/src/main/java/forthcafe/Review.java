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


    @PrePersist
    public void onPrePersist(){
   // configMap 설정
//        String sysEnv = System.getenv("SYS_MODE");
//        if(sysEnv == null) sysEnv = "LOCAL";
//        System.out.println("################## SYSTEM MODE: " + sysEnv);


        final Reviewed reviewed = new Reviewed();
        BeanUtils.copyProperties(this, reviewed);
        reviewed.setReviewText("5 Stars");
        // kafka push
        reviewed.publishAfterCommit();
        // reviewed.publish();

        // delay test시 주석해제
        // try {
        // Thread.currentThread().sleep((long) (400 + Math.random() * 220));
        // } catch (InterruptedException e) {
        // e.printStackTrace();
        // }
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(final Double price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(final Integer quantity) {
        this.quantity = quantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getOrdererName() {
        return ordererName;
    }

    public void setOrdererName(final String ordererName) {
        this.ordererName = ordererName;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(final String menuName) {
        this.menuName = menuName;
    }

    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(final Long menuId) {
        this.menuId = menuId;
    }

    public void setReviewText(final String reviewText) {
        this.reviewText = reviewText;
    }

    public String getReviewText() {
        return reviewText;
    }


}
