package analysis;

import model.Review;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class TrendReport implements Report{
    public void printReport(List<Review> reviews){
        if(reviews.isEmpty()){
            System.out.println("No trend data yet, analyze some code first.");
            return;
        }
        for(Review review : reviews){
            System.out.println(review.getTimeStamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))+" -> Score: "+review.getScore()+"/10");
        }
    }
}
