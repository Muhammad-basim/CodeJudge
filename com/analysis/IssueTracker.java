package analysis;

import model.Review;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IssueTracker implements Report{
    private static final List<String> KEYWORDS = List.of(
            "null","leak","unused","performance","buffered","concatenation"
    );
    public Map<String, Integer> countIssues(List<Review> reviews){
        Map<String, Integer> counts = new HashMap<>();
        for(Review review : reviews){
            for(String keyword : KEYWORDS){
                if(review.getFeedback().toLowerCase().contains(keyword)){
                    if(!counts.containsKey(keyword)){
                        counts.put(keyword, 0);
                    }
                    counts.put(keyword, counts.get(keyword) + 1);
                }
            }
        }
        return counts;
    }
    public void printReport(List<Review> reviews){
        Map<String, Integer> counts = countIssues(reviews);
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort((a,b) -> b.getValue() - a.getValue());
        if(entries.isEmpty()){
            System.out.println("No recurring issues found yet.");
            return;
        }
        System.out.println("=== Recurring Issues Report ===");
        for (Map.Entry<String, Integer> entry : entries){
            System.out.println(entry.getKey() + ": "+entry.getValue() + " occurence(s)");
        }
    }
}
