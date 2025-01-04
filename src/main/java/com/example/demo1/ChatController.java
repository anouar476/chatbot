package com.example.demo1;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;

public class ChatController {

    @FXML
    private ListView<HBox> chatHistoryList;
    @FXML
    private ListView<HBox> messagesView;
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
        addMessage("Salut, je suis ici pour vous aider à savoir toutes les informations concernant ENSET.", Pos.CENTER_LEFT, Color.LIGHTGREEN);
        messageInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                sendMessage();
                event.consume();  // Prevent newline in TextArea
            }
        });

        newChatButton.setOnAction(event -> startNewChat());
    }

    @FXML
    private void sendMessage() {
        String message = messageInput.getText().trim();
        if (!message.isEmpty()) {
            addMessage("You: " + message, Pos.CENTER_RIGHT, Color.LIGHTBLUE);

            if (messagesView.getItems().size() == 1) {
                chatHistoryList.getItems().add(new HBox(new Text(message)));
            }

            // Immediately add the user message and clear the input
            messageInput.clear();

            // Send the question to the AI asynchronously
            new Thread(() -> getAIResponse(message)).start();
        }
    }

    @FXML
    private void startNewChat() {
        messagesView.getItems().clear();
        messageInput.clear();
    }

    private void getAIResponse(String prompt) {
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                "{\"question\":\"" + prompt + "\"}"
        );

        Request request = new Request.Builder()
                .url(BACKEND_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Platform.runLater(() -> showAlert("Error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();

                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String aiMessage = jsonResponse.getString("answer");

                        Platform.runLater(() -> addMessage("AI: " + formatMessage(aiMessage), Pos.CENTER_LEFT, Color.LIGHTGREEN));

                    } catch (Exception e) {
                        Platform.runLater(() -> showAlert("Error parsing the response: " + e.getMessage()));
                    }
                } else {
                    Platform.runLater(() -> showAlert("Error: " + response.message()));
                }
            }

        });
    }

    private void addMessage(String message, Pos alignment, Color color) {
        Text text = new Text(message);
        HBox hbox = new HBox(text);
        hbox.setAlignment(alignment);
        hbox.setStyle("-fx-padding: 10; -fx-background-radius: 10;");

        if (alignment == Pos.CENTER_RIGHT) {
            hbox.setStyle("-fx-background-color: lightblue; -fx-padding: 10; -fx-background-radius: 10;");
        } else {
            hbox.setStyle("-fx-background-color: lightgreen; -fx-padding: 10; -fx-background-radius: 10;");
        }

        Platform.runLater(() -> messagesView.getItems().add(hbox));
    }

    private String formatMessage(String message) {
        String[] words = message.split(" ");
        StringBuilder formattedMessage = new StringBuilder();
        int wordCount = 0;

        for (String word : words) {
            formattedMessage.append(word).append(" ");
            wordCount++;
            if (wordCount == 10) {
                formattedMessage.append("\n");
                wordCount = 0;
            }
        }
        return formattedMessage.toString().trim();
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