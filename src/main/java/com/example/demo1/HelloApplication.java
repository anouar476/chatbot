package com.example.demo1;

import com.example.demo1.DataBaseConnection.MongoDBConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        try {
            MongoDBConnection.connect();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/demo1/login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(),1300,700);
        stage.setTitle("Login Form");
        stage.setScene(scene);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("stylesheet.css")).toExternalForm());

        stage.show();

        Runtime.getRuntime().addShutdownHook(new Thread(MongoDBConnection::close));
    }

    public static void main(String[] args) {
        launch();
    }
}
