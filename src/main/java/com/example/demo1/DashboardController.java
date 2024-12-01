package com.example.demo1;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

public class DashboardController {
    @FXML
    private ListView<String> chatHistoryList;

    @FXML
    private ListView<String> messagesView;

    @FXML
    private TextArea messageInput;

    @FXML
    private Button closeHistoryButton;

    @FXML
    private VBox leftSidebar;

    private boolean isHistoryVisible = true;

    @FXML
    private void initialize() {
        setupListeners();
    }

    private void setupListeners() {
        messageInput.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER")) {
                sendMessage();
                event.consume();
            }
        });
    }

    @FXML
    private void sendMessage() {
        String message = messageInput.getText().trim();
        if (!message.isEmpty()) {
            messagesView.getItems().add("You: " + message);
            chatHistoryList.getItems().add(message);
            messageInput.clear();
        }
    }}
