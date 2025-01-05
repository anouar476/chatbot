package com.example.demo1;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Login {
    private static final Logger LOGGER = Logger.getLogger(Login.class.getName());

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    public void clickLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Empty Fields", "Input Required",
                    "Username and Password cannot be empty.");
            return;
        }

        try {
            LOGGER.info("Attempting login for user: " + username);

            if (MongoDBConnection.validateUser(username, password)) {
                LOGGER.info("Login successful for user: " + username);
                loadDashboard(username);
            } else {
                LOGGER.warning("Login failed for user: " + username);
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid Credentials",
                        "Please check your username and password.");
                passwordField.clear();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Login error", e);
            showAlert(Alert.AlertType.ERROR, "System Error", "Login Failed",
                    "An error occurred while processing your request: " + e.getMessage());
        }
    }

    private void loadDashboard(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/demo1/dashboard.fxml")
            );


            Parent dashboardRoot = loader.load();
            Stage stage = (Stage) usernameField.getScene().getWindow();

            Scene dashboardScene = new Scene(dashboardRoot);
            dashboardScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("stylesheet.css")).toExternalForm());

            stage.setScene(dashboardScene);
            stage.setTitle("Dashboard - " + username);
            stage.centerOnScreen();
            stage.show();

            LOGGER.info("Dashboard loaded successfully for user: " + username);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load dashboard", e);
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to load dashboard",
                    "Error details: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error while loading dashboard", e);
            showAlert(Alert.AlertType.ERROR, "System Error", "Unexpected Error",
                    "An unexpected error occurred: " + e.getMessage());
        }
    }

    @FXML
    public void clickRegister(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/demo1/register.fxml")
            );

            Parent registerRoot = loader.load();
            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene scene = new Scene(registerRoot, 1300, 700);

            stage.setScene(scene);
            stage.setTitle("Register");
            stage.centerOnScreen();
            stage.show();

            LOGGER.info("Registration form loaded successfully");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load registration form", e);
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to load registration form",
                    "Error details: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
}
}