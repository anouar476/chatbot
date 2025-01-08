package com.example.demo1;

import com.example.demo1.DataBaseConnection.MongoDBConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;

    @FXML
    public void handleRegister(ActionEvent event) {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Empty Fields",
                    "Required Fields Empty",
                    "Username, email, and password are required.");
            return;
        }

        String result = MongoDBConnection.storeUser(username, email, phone, password);
        if (result.equals("success")) {
            showAlert(Alert.AlertType.INFORMATION, "Success",
                    "Registration Successful",
                    "You can now login.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Registration Failed", result, "");
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }


    public void clickgoback(ActionEvent event) throws IOException {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo1/login.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene scene = new Scene(loader.load(), 1300, 700); // Set window size here
            stage.setScene(scene);
            stage.setTitle("Login");
            stage.show();
        } finally {

        }
    }


}
