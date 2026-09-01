package analysis;

import model.Review;

import java.util.*;

public class ReviewHistory {
    private List<Review> reviews = new ArrayList<>();
    public void addReview(Review review){
        reviews.add(review);
    }
    public List<Review> getAllReviews(){
        return new ArrayList<>(reviews);
    }
    public void printAll(){
        if(reviews.isEmpty()){
            System.out.println("No reviews yet.");
        }else{
            for(Review review : reviews){
                System.out.println(review);
            }
        }
    }
    public List<Review> searchByFileName(String query){
        List<Review> foundOccurences = new ArrayList<>();
        for (Review review : reviews){
            if(review.getFileName().toLowerCase().contains(query.toLowerCase())){
                foundOccurences.add(review);
            }
        }
        return foundOccurences;
    }

    public Map<String, List<Review>> groupByFileType(){
        Map<String, List<Review>> map = new HashMap<>();
        for(Review review : reviews){
            if(!map.containsKey(review.getFileType())){
                map.put(review.getFileType(), new ArrayList<>());
            }
            map.get(review.getFileType()).add(review);
        }
        return map;
    }

    public List<Review> sortByDate(boolean newestFirst){
        List<Review> revs = new ArrayList<>(reviews);
//        Comparator<model.Review> byDate = new Comparator<model.Review>() {
//            @Override
//            public int compare(model.Review a, model.Review b) {
//                if(newestFirst){
//                    return b.getTimeStamp().compareTo(a.getTimeStamp());
//                }else{
//                    return a.getTimeStamp().compareTo(b.getTimeStamp());
//                }
//            }
//        };
        Comparator<Review> byDate = Comparator.comparing(Review::getTimeStamp);
        if(newestFirst){
            byDate = byDate.reversed();
        }
        revs.sort(byDate);
        return revs;
    }

    public void loadFrom(List<Review> reviews){
        this.reviews.clear();
        this.reviews.addAll(reviews);
    }
}