package com.example.demo1;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.json.JSONObject;

public class DashboardController {

    @FXML
    private ListView<HBox> chatHistoryList;
    @FXML
    private ListView<HBox> messagesView;
    @FXML
    private TextArea messageInput;
    @FXML
    private Button sendButton;

    private ObservableList<HBox> chatHistory = FXCollections.observableArrayList();
    private ObservableList<HBox> messages = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        chatHistoryList.setItems(chatHistory);
        messagesView.setItems(messages);

        // Ajouter un message de bienvenue
        addMessage("Salut, je suis ici pour vous aider à savoir toutes les informations concernant ENSET.", Pos.CENTER_LEFT, Color.LIGHTGREEN);

        // Listener pour la touche Entrée
        messageInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                sendMessage();
                event.consume();  // Empêche un saut de ligne dans la TextArea
            }
        });
    }


    @FXML
    private void startNewChat() {
        String newChatName = "Chat " + (chatHistory.size() + 1);
        HBox chatBox = new HBox(new Text(newChatName));
        AnimationUtil.fadeIn(chatBox);
        chatHistory.add(chatBox);
        messages.clear();
        messageInput.clear();
    }

    @FXML
    private void sendMessage() {
        String message = messageInput.getText().trim();
        if (!message.isEmpty()) {
            addMessage(message, Pos.CENTER_RIGHT, Color.LIGHTBLUE);

            // Immediately add the user message and clear the input
            messageInput.clear();

            // Send the question to the AI asynchronously
            new Thread(() -> {
                String response = ChatbotClient.sendQuestion(message);
                if (response != null && !response.isEmpty()) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String answer = jsonResponse.getString("answer");
                        Platform.runLater(() -> addMessage(formatMessage(answer), Pos.CENTER_LEFT, Color.LIGHTGREEN));
                    } catch (Exception e) {
                        Platform.runLater(() -> addMessage(formatMessage(response), Pos.CENTER_LEFT, Color.LIGHTGREEN));
                    }
                } else {
                    Platform.runLater(() -> addMessage(formatMessage("Sorry, I couldn't understand your question."), Pos.CENTER_LEFT, Color.LIGHTGREEN));
                }
            }).start();
        }
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

        messages.add(hbox);
        AnimationUtil.fadeIn(hbox);
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

    @FXML
    private void handleClose() {
        System.exit(0);
    }

    @FXML
    private void handlePreferences() {
    }

    @FXML
    private void handleAbout() {
        System.out.println("About this Chatbot App...");
    }

    @FXML
    private void handleContactSupport() {
        System.out.println("Contact support...");
    }
}