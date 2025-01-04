module com.example.demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires org.mongodb.driver.sync.client;
    requires org.mongodb.bson;
    requires org.mongodb.driver.core;
    requires java.desktop;
    requires okhttp3;
    requires com.google.gson;
    requires java.logging;
    requires annotations;
    requires org.apache.pdfbox;
    requires json.simple;
    requires org.json;
    opens com.example.demo1 to javafx.fxml;
    exports com.example.demo1;
}