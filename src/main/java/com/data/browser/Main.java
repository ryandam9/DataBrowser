package com.data.browser;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;

import static com.data.browser.AppLogger.logger;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        logger.debug("Data Browser is getting started...");
        URL url = new File("resources/ui/browser_window.fxml").toURI().toURL();
        Parent root = FXMLLoader.load(url);

        Scene scene = new Scene(root, 1320, 1000);
        String cssFile = new File("resources/css/theme-1.css").toURI().toURL().toString();
        scene.getStylesheets().add(cssFile);
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setFullScreen(true);
        stage.setTitle("Data Browser");
        stage.getIcons().add(new Image(new File("resources/images/database.png").toURI().toURL().toString()));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}