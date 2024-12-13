package com.example.demo1;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class ChatbotDashboard extends Application {

    private TextArea chatArea;
    private TextField userInput;
    private PrintWriter out;
    private BufferedReader in;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Chatbot Dashboard");

        chatArea = new TextArea();
        chatArea.setEditable(false);
        userInput = new TextField();
        userInput.setPromptText("Type your question...");

        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendMessage());

        VBox vbox = new VBox(userInput, sendButton);
        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(chatArea);
        borderPane.setBottom(vbox);

        Scene scene = new Scene(borderPane, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Start the chatbot process
        startChatbotProcess();
    }

    private void startChatbotProcess() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("python", "path/to/your/python_script.py");
            Process process = processBuilder.start();

            in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(process.getOutputStream()), true);

            // Start a new thread to read the chatbot's output
            new Thread(() -> {
                String line;
                try {
                    while ((line = in.readLine()) != null) {
                        chatArea.appendText("Chatbot: " + line + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String message = userInput.getText();
        if (!message.isEmpty()) {
            chatArea.appendText("You: " + message + "\n");
            out.println(message); // Send the message to the Python script
            userInput.clear();
        }
    }
}