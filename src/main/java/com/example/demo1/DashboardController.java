package com.example.demo1;

import com.example.demo1.Chat.ChatbotClient;
import com.example.demo1.DataBaseConnection.MongoDBConnection;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.bson.Document;
import org.json.JSONObject;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
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

        addMessage("Hello, je suis ici pour vous aider à savoir toutes les informations necessaires concernant ENSET \uD83C\uDF93 .", Pos.CENTER_LEFT, Color.LIGHTGREEN, false);

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
            addMessage(message, Pos.CENTER_RIGHT, Color.LIGHTBLUE, true); // User message

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
                            addMessage(answer, Pos.CENTER_LEFT, Color.LIGHTGREEN, false); // AI response
                            MongoDBConnection.storeConversation(message, answer, chatHistoryList.getSelectionModel().getSelectedItem()); // Store with chat name
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> addMessage(response, Pos.CENTER_LEFT, Color.LIGHTGREEN, false)); // AI response
                    }
                } else {
                    Platform.runLater(() -> addMessage("Sorry, I couldn't understand your question.", Pos.CENTER_LEFT, Color.LIGHTGREEN, false)); // AI response
                }
            }).start();
        }
    }

    private void addMessage(String message, Pos alignment, Color color, boolean isUserMessage) {
        Text text = new Text(message); // Initialize with the full message
        HBox hbox = new HBox(text);
        hbox.setAlignment(alignment);
        hbox.setStyle("-fx-padding: 10; -fx-background-radius: 10;");

        // Set background color based on alignment
        if (alignment == Pos.CENTER_RIGHT) {
            hbox.setStyle("-fx-background-color: lightblue; -fx-padding: 10; -fx-background-radius: 10;");
        } else {
            hbox.setStyle("-fx-background-color: lightgreen; -fx-padding: 10; -fx-background-radius: 10;");
        }

        messages.add(hbox);

        if (!isUserMessage) {
            animateMessageWordByWord(message, text);
        }
    }
    private void addMessage1(String message, Pos alignment, Color color, boolean isUserMessage) {
        Text text = new Text(formatMessage(message.split(" "), message.split(" ").length)); // Initialize with the formatted message
        HBox hbox = new HBox(text);
        hbox.setAlignment(alignment);
        hbox.setStyle("-fx-padding: 10; -fx-background-radius: 10;");

        // Set background color based on alignment
        if (alignment == Pos.CENTER_RIGHT) {
            hbox.setStyle("-fx-background-color: lightblue; -fx-padding: 10; -fx-background-radius: 10;");
        } else {
            hbox.setStyle("-fx-background-color: lightgreen; -fx-padding: 10; -fx-background-radius: 10;");
        }

        messages.add(hbox);
    }

    private void animateMessageWordByWord(String message, Text text) {
        String[] words = message.split(" ");
        Timeline timeline = new Timeline();

        // Create a KeyFrame for each word in the message
        for (int i = 0; i < words.length; i++) {
            final int index = i;
            KeyFrame keyFrame = new KeyFrame(Duration.seconds(0.1 * index), event -> {
                text.setText(formatMessage(words, index + 1)); // Ensure the full message is displayed
            });
            timeline.getKeyFrames().add(keyFrame);
        }

        // Start the animation
        timeline.play();
    }

    private String formatMessage(String[] words, int maxWords) {
        StringBuilder formattedMessage = new StringBuilder();
        int lineLength = 0;
        int maxLineLength = 80;  // maximum characters per line

        // Construct the message line by line with wrapping
        for (int i = 0; i < maxWords; i++) {
            String word = words[i];
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
                addMessage1("You: " + message, Pos.CENTER_RIGHT, Color.LIGHTBLUE, true);
                addMessage1("AI: " + response, Pos.CENTER_LEFT, Color.LIGHTGREEN, false);
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

    public void uploadphoto(ActionEvent actionEvent) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select a photo");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);

        // Add filter for image files
        fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Image files", "jpg", "png", "jpeg", "gif"));

        // Show the file chooser dialog
        int returnValue = fileChooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            JOptionPane.showMessageDialog(null, "Selected file: " + selectedFile.getName());
        }
    }

    public void openSettings(ActionEvent actionEvent) {
    }

        @FXML
private void logout(ActionEvent actionEvent) {
    // Close the current window
    ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();

    // Load the login page
    try {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/demo1/login.fxml"));
        Parent root = fxmlLoader.load();
        Stage stage = new Stage();
        stage.setTitle("Login");
        stage.setScene(new Scene(root));
        stage.show();
    } catch (IOException e) {
        e.printStackTrace();
    }
}

    }