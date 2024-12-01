package com.example.demo1;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Initialize database connection when application starts
        try {
            MongoDBConnection.connect();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // Load the login form
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/demo1/dashboard.fxml"));
        Scene scene = new Scene(fxmlLoader.load(),1300,700);
        stage.setTitle("Login Form");
        stage.setScene(scene);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("stylesheet.css")).toExternalForm());

        stage.show();

        // Add shutdown hook to close database connection
        Runtime.getRuntime().addShutdownHook(new Thread(MongoDBConnection::close));
    }

    public static void main(String[] args) {
        launch();
    }
}
