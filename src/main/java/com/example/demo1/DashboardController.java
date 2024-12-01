package com.example.demo1;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import javafx.util.Duration;

public class DashboardController {

    @FXML
    private TextArea chatArea;

    @FXML
    private TextArea inputField;

    @FXML
    private Button sendButton;

    private FadeTransition fadeTransitionChatArea;

    @FXML
    public void initialize() {
        // Initialize FadeTransition for chat area
        FadeTransition fadeTransitionChatArea = new FadeTransition(Duration.millis(1000), chatArea);
        fadeTransitionChatArea.setFromValue(0);
        fadeTransitionChatArea.setToValue(1);

        // Start the fade-in animation for the chat area
        fadeTransitionChatArea.play();
    }
    @FXML
    private void handleSendMessage() {
        String message = inputField.getText();
        if (!message.trim().isEmpty()) {
            appendMessage("You", message);
            inputField.clear();
            simulateBotResponse();
        }
    }

    private void appendMessage(String sender, String message) {
        String formattedMessage = sender + ": " + message + "\n";

        // Append the message to the chat area
        chatArea.appendText(formattedMessage);

        // Create a new FadeTransition for each message for smooth entry
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), chatArea);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setCycleCount(1);
        fadeIn.play();

        // Scroll to bottom of chat area after message appending
        chatArea.setScrollTop(Double.MAX_VALUE);
    }

    private void simulateBotResponse() {
        String botResponse = "Bot: Hello, how can I help you today?\n";
        appendMessage("Bot", botResponse);
    }

    @FXML
    private void handleExit() {
        Stage stage = (Stage) sendButton.getScene().getWindow();
        stage.close();
    }
}