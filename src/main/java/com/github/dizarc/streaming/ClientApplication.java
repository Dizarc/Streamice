package com.github.dizarc.streaming;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ClientApplication extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ServerApplication.class.getResource("client1-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Streaming Client");
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(_ -> {
            Platform.exit();
            System.exit(0);
        });
    }

}
