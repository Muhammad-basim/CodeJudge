package service;

import com.google.gson.Gson;
import model.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

public class AIService implements AIProvider {
    private String apiKey;
    private String url;
    public AIService(){
        this.apiKey = loadApiKey();
        this.url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=" + apiKey;
    }
    private String loadApiKey(){
        try{
            Path path = Path.of(System.getProperty("user.home"), ".codejudge", "config.properties");
            if(Files.exists(path)){
                Properties props = new Properties();
                try(InputStream in = Files.newInputStream(path)){
                    props.load(in);
                }
                return props.getProperty("gemini.api.key","");
            }
        }catch(IOException e){

        }
        return "";
    }

    public ReviewResult reviewCode(String code, String focus, String priorFeedback) throws IOException, InterruptedException {
        Gson gson = new Gson();
        String focusInstruction = switch (focus) {
            case "Security" -> "Pay special attention to security vulnerabilities — prioritize and elaborate on these even if other categories have less to say.";
            case "Performance" -> "Pay special attention to performance issues — prioritize and elaborate on these even if other categories have less to say.";
            case "Readability" -> "Pay special attention to readability and best-practice issues — prioritize and elaborate on these even if other categories have less to say.";
            default -> ""; // General — no special emphasis
        };
        String diffInstruction = "";
        if (priorFeedback != null && !priorFeedback.isBlank()) {
            diffInstruction = "This code was reviewed before. Here is the prior review:\n" + priorFeedback +
                    "\n\nCompare the new code against that prior review. Explicitly note what has improved, what has regressed, and what remains unchanged. "+
                    "When describing something that was FIXED or IMPROVED, do not reuse the original [CRITICAL]/[WARNING]/[SUGGESTION] tag in that sentence — instead prefix it with [IMPROVED]. Only use CRITICAL/WARNING/SUGGESTION tags for issues that still remain in the current code.\n\n";
        }

        String prompt = "You are a strict senior code reviewer. " + focusInstruction + " " + diffInstruction + " Review the following code and respond with ONLY a valid JSON object, no other text, no markdown code fences, in exactly this shape:\n\n" +
                "{\n" +
                "  \"score\": <integer 1-10, overall code quality>,\n" +
                "  \"bugs\": \"<real bugs found, each with concrete consequence, or 'None.' if none>\",\n" +
                "  \"performance\": \"<real performance issues with concrete cost, or 'None.' if none>\",\n" +
                "  \"readability\": \"<real readability/best-practice issues, or 'None.' if none>\"\n" +
                "}\n\n" +
                "Scoring guide:\n" +
                "9-10 = production-ready, no real concerns\n" +
                "7-8 = solid, only minor issues (small performance inefficiencies, minor readability nitpicks)\n" +
                "5-6 = works, but has real problems worth fixing (moderate bugs, notable inefficiencies)\n" +
                "3-4 = has significant bugs or risks that would likely cause failures\n" +
                "1-2 = broken or fundamentally unsafe\n\n" +
                "Do not invent minor issues just to fill a category. If a category is clean, say exactly \"None.\" Be direct and concrete — explain what breaks or what it costs, not just that something is \"bad practice.\"\n\n" +
                "For each issue you list within bugs/performance/readability, prefix it with its severity tag: [CRITICAL], [WARNING], or [SUGGESTION]. Example: \"[CRITICAL] Null pointer risk on line 5.\"\n\n" +
                "Code to review:\n" + code;

        RequestPart part = new RequestPart(prompt);
        RequestContent content = new RequestContent(List.of(part));
        GeminiRequest requestBody = new GeminiRequest(List.of(content));
        String jsonBody = gson.toJson(requestBody);

//        System.out.println("=== EXACT REQUEST BODY SENT TO AI ===");
//        System.out.println(jsonBody);
//        System.out.println("=== END REQUEST BODY ===");

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if(response.statusCode() == 429){
            throw new IOException("Rate limit reached, please wait a minute before analying again.");
        }
        if (response.statusCode() != 200) {
            throw new IOException("AI request failed (status " + response.statusCode() + "): " + response.body());
        }

        GeminiResponse geminiResponse = gson.fromJson(response.body(), GeminiResponse.class);

        if(geminiResponse == null || geminiResponse.getCandidates() == null || geminiResponse.getCandidates().isEmpty()){
            return null;
        }
        String rawText = geminiResponse.getCandidates().get(0).getContent().getParts().get(0).getText();
        String cleanText = rawText.trim();

        if (cleanText.startsWith("```") && cleanText.endsWith("```")) {
            int firstBrace = cleanText.indexOf('{');
            int lastBrace = cleanText.lastIndexOf('}');

            if (firstBrace != -1 && lastBrace != -1 && firstBrace < lastBrace) {
                cleanText = cleanText.substring(firstBrace, lastBrace + 1);
            }
        }
        ReviewResult revRes = gson.fromJson(cleanText, ReviewResult.class);
        return revRes;
    }
}
