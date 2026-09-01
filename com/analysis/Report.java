package analysis;

import model.Review;
import java.util.List;

public interface Report {
    void printReport(List<Review> reviews);
}
