package com.example.demo1;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;

public class ChatController {

    @FXML
    private ListView<String> chatHistoryList;

    @FXML
    private ListView<String> messagesView;

    @FXML
    private TextArea messageInput;

    @FXML
    private Button newChatButton;

    private static final String BACKEND_URL = "http://127.0.0.1:5000/ask";
    private final OkHttpClient client = new OkHttpClient();

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

        newChatButton.setOnAction(event -> startNewChat());
    }

    @FXML
    private void sendMessage() {
        String message = messageInput.getText().trim();
        if (!message.isEmpty()) {
            messagesView.getItems().add("You: " + message);
            if (messagesView.getItems().size() == 1) {
                chatHistoryList.getItems().add(message);
            }
            messageInput.clear();
            getAIResponse(message);
        }
    }

    @FXML
    private void startNewChat() {
        messagesView.getItems().clear();
        messageInput.clear();
    }

    private void getAIResponse(String prompt) {
        // Create the JSON body for the request
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                "{\"question\":\"" + prompt + "\"}"
        );

        // Create the request
        Request request = new Request.Builder()
                .url(BACKEND_URL)
                .post(body)
                .build();

        // Asynchronous request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle failure
                Platform.runLater(() -> showAlert("Error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();

                    try {
                        // Parse the JSON response
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String aiMessage = jsonResponse.getString("answer"); // Use "answer" from the JSON

                        // Update the UI with the message (without showing JSON structure)
                        Platform.runLater(() -> messagesView.getItems().add("AI: " + aiMessage));

                    } catch (Exception e) {
                        Platform.runLater(() -> showAlert("Error parsing the response: " + e.getMessage()));
                    }
                } else {
                    Platform.runLater(() -> showAlert("Error: " + response.message()));
                }
            }

        });
    }

    private void showAlert(String message) {
        // Show an alert box in case of errors
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
