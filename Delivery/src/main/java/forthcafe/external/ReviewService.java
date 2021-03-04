
package forthcafe.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@FeignClient(name="Review", url="http://Review:8088")
public interface ReviewService {

    @RequestMapping(method= RequestMethod.GET, path="/reviews")
    public void review(@RequestBody Review review);

}