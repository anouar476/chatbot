package com.example.demo1;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ChatbotClient {

    private static final String SERVER_URL = "http://127.0.0.1:5000/ask";

    public static String sendQuestion(String question) {
        try {
            URL url = new URL(SERVER_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String jsonInput = String.format("{\"question\": \"%s\"}", question);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8);
                String response = scanner.useDelimiter("\\A").next();
                scanner.close();
                return response;
            } else {
                return "Error: Server responded with status code " + status;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: Could not connect to the server.";
        }
    }

    public static void main(String[] args) {
        String question = "resume the pdf ?";
        String response = sendQuestion(question);
        System.out.println("Response: " + response);
    }
}