package forthcafe.external;

import org.springframework.stereotype.Service;

@Service
public class ReviewServiceImpl implements ReviewService {

    // fallback message
    @Override
    public void review(Review review) {
        System.out.println("!!!!!!!!!!!!!!!!!!!!! Pay service is BUSY !!!!!!!!!!!!!!!!!!!!!");
        System.out.println("!!!!!!!!!!!!!!!!!!!!!   Try again later   !!!!!!!!!!!!!!!!!!!!!");
    }

}
