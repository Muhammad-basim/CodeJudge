package model;

import java.util.List;
public class RequestContent {
    List<RequestPart> parts;
    public RequestContent(List<RequestPart> parts) {
        this.parts = parts;
    }
}
