package com.github.dizarc.streaming;

import java.io.File;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;

import java.net.URL;
import java.util.ResourceBundle;

public class ServerController implements Initializable {

    @FXML
    private Label videosCreatedLabel;
    @FXML
    private Label ClientLabel;

    @FXML
    private ListView<String> videoListView;
    @FXML
    private ScrollBar videoScrollBar;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        initializeList();

    }

    private void initializeList() {

        File directory = new File("C:\\Users\\faruk\\Desktop\\sxoli\\6mina\\H8 mino\\polumesa\\Streaming\\src\\main\\resources\\videos");
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files)
                videoListView.getItems().add(file.getName());
        }
    }
}