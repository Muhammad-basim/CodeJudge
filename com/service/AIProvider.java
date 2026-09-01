package service;

import model.ReviewResult;
import java.io.IOException;

public interface AIProvider {
    ReviewResult reviewCode(String code, String focus, String priorFeedback) throws IOException, InterruptedException;
}
