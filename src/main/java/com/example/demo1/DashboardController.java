package com.example.demo1;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.input.KeyCode;
import okhttp3.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;

public class DashboardController {

    public BorderPane root1;
    @FXML
    private ListView<String> chatHistoryList;

    @FXML
    private ListView<String> messagesView;

    @FXML
    private TextArea messageInput;

    @FXML
    private Button newChatButton; // New Chat Button

    @FXML
    private VBox leftSidebar;

    private boolean isHistoryVisible = true;
    private static final String API_KEY = "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";
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    @FXML
    private void initialize() {
        setupListeners();
    }

    private void setupListeners() {
        messageInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                sendMessage();
                event.consume();
            }
        });

        // New Chat Button Event Handler
        newChatButton.setOnAction(event -> startNewChat());
    }

    @FXML
    private void sendMessage() {
        String message = messageInput.getText().trim();
        if (!message.isEmpty()) {
            // Add the message to the messages view
            messagesView.getItems().add("You: " + message);
            // Only add the first message sent by the user to the history
            if (messagesView.getItems().size() == 1) {
                chatHistoryList.getItems().add(message);
            }
            messageInput.clear();
            getAIResponse(message);
        }
    }

    @FXML
    private void startNewChat() {
        // Save the current conversation's first message to the history
        saveFirstUserMessageToHistory();

        // Clear the message history view and reset the input
        messagesView.getItems().clear();
        messageInput.clear();
    }

    private void saveFirstUserMessageToHistory() {
        if (!messagesView.getItems().isEmpty()) {
            // Only save the first message sent by the user to the chat history
            String firstMessage = messagesView.getItems().get(0).substring(4); // Remove the "You: " prefix
            chatHistoryList.getItems().add(firstMessage);
        }
    }

    private void getAIResponse(String prompt) {
        OkHttpClient client = new OkHttpClient();

        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("model", "gpt-3.5-turbo");
        JsonArray messages = new JsonArray();

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", prompt);

        messages.add(userMessage);
        jsonRequest.add("messages", messages);

        RequestBody body = RequestBody.create(
                Objects.requireNonNull(MediaType.parse("application/json")),
                jsonRequest.toString()
        );

        Request request = new Request.Builder()
                .url(OPENAI_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Platform.runLater(() -> messagesView.getItems().add("Error: " + e.getMessage()));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    assert response.body() != null;
                    String responseBody = response.body().string();
                    String aiResponse = extractAIResponse(responseBody);
                    Platform.runLater(() -> messagesView.getItems().add("AI: " + aiResponse));
                } else {
                    Platform.runLater(() -> messagesView.getItems().add("Error: " + response.message()));
                }
            }
        });
    }

    private String extractAIResponse(String responseBody) {
        JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonArray choices = jsonResponse.getAsJsonArray("choices");
        JsonObject firstChoice = choices.get(0).getAsJsonObject();
        return firstChoice.getAsJsonObject("message").get("content").getAsString().trim();
    }

    @FXML
    private void toggleHistoryVisibility() {
        isHistoryVisible = !isHistoryVisible;

        Platform.runLater(() -> {
            if (isHistoryVisible) {
                chatHistoryList.setVisible(true);
                leftSidebar.setPrefWidth(250);  // Set the original width of the sidebar
            } else {
                chatHistoryList.setVisible(false);
                leftSidebar.setPrefWidth(50);   // Shrink the sidebar width when history is hidden
            }
        });
    }
}
