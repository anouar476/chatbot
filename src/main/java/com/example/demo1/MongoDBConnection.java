package com.example.demo1;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.mongodb.client.model.IndexOptions;
import javafx.scene.control.Label;
import org.bson.Document;

import java.util.regex.Pattern;

public class MongoDBConnection {
    static MongoClient mongoClient;
    private static MongoCollection<Document> usersCollection;
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@(.+)$";
    private static final String PHONE_REGEX = "^\\d{10}$";
    public Label userCountLabel;

    // Replace with your MongoDB Atlas connection string
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";

    public static void connect() {
        try {
            // Configure MongoDB Atlas connection
            ConnectionString connString = new ConnectionString(CONNECTION_STRING);
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connString)
                    .build();

            // Create MongoDB client
            mongoClient = MongoClients.create(settings);

            // Connect to the database and collection
            MongoDatabase database = mongoClient.getDatabase("UserDatabase");
            usersCollection = database.getCollection("Users");

            // Create unique indexes on "username" and "email"
            IndexOptions indexOptions = new IndexOptions().unique(true);
            usersCollection.createIndex(new Document("username", 1), indexOptions);
            usersCollection.createIndex(new Document("email", 1), indexOptions);

            System.out.println("Connected to MongoDB Atlas successfully.");
        } catch (MongoException e) {
            throw new RuntimeException("Failed to connect to MongoDB Atlas: " + e.getMessage());
        }
    }

    public static boolean isValidEmail(String email) {
        return Pattern.compile(EMAIL_REGEX).matcher(email).matches();
    }

    public static boolean isValidPhone(String phone) {
        return Pattern.compile(PHONE_REGEX).matcher(phone).matches();
    }

    public static String storeUser(String username, String email, String phone, String password) {
        if (!isValidEmail(email)) {
            return "Invalid email format";
        }
        if (!phone.isEmpty() && !isValidPhone(phone)) {
            return "Invalid phone format";
        }

        try {
            if (usersCollection.countDocuments(new Document("username", username)) > 0) {
                return "Username already exists";
            }
            if (usersCollection.countDocuments(new Document("email", email)) > 0) {
                return "Email already exists";
            }

            String hashedPassword = password + "_hashed";
            Document user = new Document()
                    .append("username", username)
                    .append("email", email)
                    .append("phone", phone)
                    .append("password", hashedPassword);

            usersCollection.insertOne(user);
            return "success";
        } catch (MongoException e) {
            return "Registration failed: " + e.getMessage();
        }
    }

    public static boolean validateUser(String username, String password) {
        try {
            Document user = usersCollection.find(new Document("username", username)).first();
            if (user != null) {
                String storedHash = user.getString("password");
                return (password + "_hashed").equals(storedHash);
            }
            return false;
        } catch (MongoException e) {
            System.err.println("Error validating user: " + e.getMessage());
            return false;
        }
    }

    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
