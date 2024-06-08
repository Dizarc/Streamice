package com.github.dizarc.streaming;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class ServerApplication extends Application {

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ServerApplication.class.getResource("server-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.getIcons().add(new Image(Objects.requireNonNull(ServerApplication.class.getResourceAsStream("/icons/serverIcon.png"))));
        stage.setTitle("Streaming Server");
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(_ -> {
            Platform.exit();
            System.exit(0);
        });
    }


}