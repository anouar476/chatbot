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
import org.bson.Document;
import org.json.JSONObject;

import java.util.List;

public class DashboardController {

    @FXML
    private ListView<String> chatHistoryList; // ListView for chat history
    @FXML
    private ListView<HBox> messagesView;
    @FXML
    private TextArea messageInput;
    @FXML
    private Button sendButton;

    private ObservableList<String> chatHistory = FXCollections.observableArrayList();
    private ObservableList<HBox> messages = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        chatHistoryList.setItems(chatHistory);
        messagesView.setItems(messages);

        chatHistoryList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                loadChat(newValue);
            }
        });

        addMessage("Hello, je suis ici pour vous aider à savoir toutes les informations necessaires concernant ENSET \uD83C\uDF93 .", Pos.CENTER_LEFT, Color.LIGHTGREEN);

        messageInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                sendMessage();
                event.consume();
            }
        });

        loadChatHistory(); // Load chat history
    }

    @FXML
    private void startNewChat() {
        String newChatName = "Chat " + (chatHistory.size() + 1);
        chatHistory.add(newChatName);
        chatHistoryList.getSelectionModel().select(newChatName);
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
                        Platform.runLater(() -> {
                            addMessage(formatMessage(answer), Pos.CENTER_LEFT, Color.LIGHTGREEN);
                            MongoDBConnection.storeConversation( message, answer, chatHistoryList.getSelectionModel().getSelectedItem()); // Store with chat name
                        });
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
        int lineLength = 0;
        int maxLineLength = 80;
        for (String word : words) {
            if (lineLength + word.length() + 1 > maxLineLength) {
                formattedMessage.append("\n");
                lineLength = 0;
            }
            formattedMessage.append(word).append(" ");
            lineLength += word.length() + 1;
        }
        return formattedMessage.toString().trim();
    }

    @FXML
    private void loadChatHistory() {
        // Load chat history from the database
        List<Document> conversations = MongoDBConnection.getConversations(); // Replace with actual username
        for (Document conversation : conversations) {
            String chatName = conversation.getString("chatName");
            if (chatName != null && !chatHistory.contains(chatName)) {
                chatHistory.add(chatName);
            }
        }
    }

    @FXML
    private void loadChat(String chatName) {
        // Load messages for the selected chat
        messages.clear();
        List<Document> conversations = MongoDBConnection.getConversations(); // Replace with actual username
        for (Document conversation : conversations) {
            if (chatName.equals(conversation.getString("chatName"))) {
                String message = conversation.getString("message");
                String response = conversation.getString("response");
                addMessage("You: " + message, Pos.CENTER_RIGHT, Color.LIGHTBLUE);
                addMessage("AI: " + response, Pos.CENTER_LEFT, Color.LIGHTGREEN);
            }
        }
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