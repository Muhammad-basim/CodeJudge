package model;

import java.util.List;
public class GeminiResponse {
    List<Candidate> candidates;
    public List<Candidate> getCandidates() {
        return candidates;
    }
    public static class Candidate{
        Content content;
        public Content getContent() { return content; }
        public static class Content{
            List<Part> parts;
            public List<Part> getParts() {
                return parts;
            }
            public static class Part{
                String text;
                public String getText() {
                    return text;
                }
            }
        }
    }
}
