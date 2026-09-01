package data;

import model.Review;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReviewRepository {
    public void save(Review review) throws SQLException{
        String sql = "INSERT INTO reviews (file_name, file_type, review_timestamp, feedback, quality_score) VALUES (?, ?, ?, ?, ?)";
        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setString(1, review.getFileName());
            stmt.setString(2, review.getFileType());
            stmt.setObject(3, review.getTimeStamp());
            stmt.setString(4, review.getFeedback());
            stmt.setInt(5, review.getScore());
            stmt.executeUpdate();
        }
    }

    public List<Review> findAll() throws SQLException{
        List<Review> reviews = new ArrayList<>();
        String sql = "SELECT * FROM reviews";
        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()){
            while(rs.next()){
                int id = rs.getInt("id");
                String feedback = rs.getString("feedback");
                LocalDateTime timestamp = rs.getObject("review_timestamp", LocalDateTime.class);
                String fileName = rs.getString("file_name");
                int score = rs.getInt("quality_score");
                Review review = new Review(id, feedback, timestamp, fileName, score);
                reviews.add(review);
            }
        }
        return reviews;
    }

    public Review findMostRecentByName(String fileName) throws SQLException{
        String sql = "SELECT * FROM reviews WHERE file_name = ? ORDER BY review_timestamp DESC LIMIT 1";
        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, fileName);
            try(ResultSet rs = stmt.executeQuery()){
                if(rs.next()){
                    int id = rs.getInt("id");
                    String feedback = rs.getString("feedback");
                    LocalDateTime timestamp = rs.getObject("review_timestamp", LocalDateTime.class);
                    String name = rs.getString("file_name");
                    int score = rs.getInt("quality_score");
                    return new Review(id, feedback, timestamp, name, score);
                }
                return null;
            }
        }
    }
    public void deleteById(int id) throws SQLException{
        String sql = "DELETE FROM reviews WHERE id = ?";
        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1,id);
            stmt.executeUpdate();
        }
    }
}
