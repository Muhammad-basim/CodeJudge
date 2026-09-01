package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Review {
    private String fileName;
    private LocalDateTime timeStamp;
    private String feedback;
    private int score;
    private int id;
    public Review(String feedback, LocalDateTime timeStamp, String fileName, int score) {
        this.feedback = feedback;
        this.timeStamp = timeStamp;
        this.fileName = fileName;
        this.score = score;
    }
    public Review(int id, String feedback, LocalDateTime timeStamp, String fileName, int score) {
        this.id = id;
        this.feedback = feedback;
        this.timeStamp = timeStamp;
        this.fileName = fileName;
        this.score = score;
    }
    public String getFileName() {
        return fileName;
    }
    public String getFileType() {
        return (fileName.lastIndexOf('.') == -1) ? "" : fileName.substring(fileName.lastIndexOf('.') + 1);
    }
    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }
    public String getFeedback() {
        return feedback;
    }
    public int getScore() {
        return score;
    }
    public int getId() {
        return id;
    }
    @Override
    public String toString() {
        return "["+fileName+"] ("+getFileType()+") - "+timeStamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))+"\nScore: "+score+"/10\nFeedback: "+feedback.substring(0, Math.min(feedback.length(),50))+"...\n";
    }
}