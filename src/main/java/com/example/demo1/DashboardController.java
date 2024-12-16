package com.example.demo1;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import org.json.JSONObject;

public class DashboardController {

    @FXML
    private ListView<String> chatHistoryList; // List of previous chats
    @FXML
    private ListView<String> messagesView; // List of messages in the current chat
    @FXML
    private TextArea messageInput; // TextArea for inputting messages
    @FXML
    private Button sendButton; // Button to send a message

    private ObservableList<String> chatHistory = FXCollections.observableArrayList();
    private ObservableList<String> messages = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Initialize ListView for chat history and messages
        chatHistoryList.setItems(chatHistory);
        messagesView.setItems(messages);
    }

    // Method to handle the "New Chat" button click
    @FXML
    private void startNewChat() {
        // Clear the current messages and add to the chat history
        String newChatName = "Chat " + (chatHistory.size() + 1);
        chatHistory.add(newChatName);
        messages.clear();
        messageInput.clear();
    }

    // Method to handle the "Send" button click
    @FXML
    private void sendMessage() {
        String message = messageInput.getText().trim();
        if (!message.isEmpty()) {
            // Add the user's message to the messages list with a human emoji
            messages.add("You : " + message);

            // Send the message to the server (or ChatbotClient)
            String response = ChatbotClient.sendQuestion(message);

            // Check if the response is valid
            if (response != null && !response.isEmpty()) {
                // Try to parse the response as a JSON (assuming the response is a JSON string)
                try {
                    // Assuming the response is in JSON format, and contains an "answer" key
                    JSONObject jsonResponse = new JSONObject(response);
                    String answer = jsonResponse.getString("answer");

                    // Add the bot's response to the messages list with a machine emoji
                    messages.add("Bot 🤖: " + answer);
                } catch (Exception e) {
                    // If the response is not JSON, display it as a plain string with a machine emoji
                    messages.add("Bot 🤖: " + response);
                }
            } else {
                // If the response is empty or null, display a fallback message with a machine emoji
                messages.add("Bot 🤖: Sorry, I couldn't understand your question.");
            }

            // Clear the message input field after sending
            messageInput.clear();
        }
    }


    // Handle "Close" menu action
    @FXML
    private void handleClose() {
        // Close the application (or handle as needed)
        System.exit(0);
    }

    // Handle "Preferences" menu action
    @FXML
    private void handlePreferences() {
        // Add preferences logic if needed (e.g., settings or configuration)
    }

    // Handle "About" menu action
    @FXML
    private void handleAbout() {
        // Show About dialog or information
        System.out.println("About this Chatbot App...");
    }

    // Handle "Contact Support" menu action
    @FXML
    private void handleContactSupport() {
        // Add support contact logic if needed
        System.out.println("Contact support...");
    }
}
