package com.email.writer.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.email.writer.controller.EmailRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EmailGeneratorService {

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    //webclient is used to the call other apis in our application
    private WebClient webClient;

    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String generateEmailReply(EmailRequest emailRequest) {

        // Build prompt
        String prompt = buildPrompt(emailRequest);

        // craft the request

        // request need this format
        // {
        // "contents": [
        // {
        // "parts": [
        // {
        // "text": "Explain how AI works in a few words"
        // }
        // ]
        // }
        // ]
        // }

        Map<String, Object> requestBody = Map.of(
                "contents", new Object[] {
                        Map.of("parts", new Object[] {
                                Map.of("text", prompt)
                        })
                });

        // go request and get the response
        String response = webClient.post()
        		.uri(geminiApiUrl + "?key=" + geminiApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // extract and return response

        return extractResponseContent(response);
    }

    private String extractResponseContent(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);

            // Check if the 'candidates' array is present and has at least one item
            if (rootNode.path("candidates").isArray() && rootNode.path("candidates").size() > 0) {
                
                // Safely extract the text from the first candidate
                return rootNode.path("candidates")
                        .get(0)
                        .path("content")
                        .path("parts")
                        .get(0)
                        .path("text")
                        .asText();

            } else if (rootNode.has("promptFeedback")) {
                 // If no candidates but feedback is present, it means the prompt was blocked
                JsonNode safetyRatings = rootNode.path("promptFeedback").path("safetyRatings");
                
                if (safetyRatings.isArray() && safetyRatings.size() > 0) {
                    // Report the specific block reason
                    JsonNode firstRating = safetyRatings.get(0);
                    return "AI failed to generate reply due to safety policy: " 
                        + firstRating.path("category").asText() 
                        + " (Probability: " + firstRating.path("probability").asText() + ")";
                }
            }
            
            // Default fallback message for unhandled empty responses
            return "The AI returned an unexpected empty response. Check API usage or prompt content.";

        } catch (Exception e) {
            // Catches any underlying parsing issues (e.g., if the response is not valid JSON)
            return "Error Processing API Response: " + e.getMessage();
        }
    }

    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(
                "Generate a professional email reply to the message below. Do not include a subject line. ");

        // check tone
        if (emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
            prompt.append("Use a ").append(emailRequest.getTone()).append(" tone.");

        }

        prompt.append("\nOriginal email: \n").append(emailRequest.getEmailContent());

        return prompt.toString();
    }

}
