package model;

import java.util.List;
public class GeminiRequest {
    List<RequestContent> contents;
    public GeminiRequest(List<RequestContent> contents) {
        this.contents = contents;
    }
}