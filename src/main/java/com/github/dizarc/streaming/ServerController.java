package com.github.dizarc.streaming;

import java.io.File;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;

import java.net.URL;
import java.util.ResourceBundle;

public class ServerController implements Initializable {

    @FXML
    private Label videosCreatedLabel;



    @FXML
    private Label clientLabel;

    @FXML
    private ListView<String> videoListView;

    @FXML
    private ProgressBar videoCreationProgress;

    private ServerLogic serverLogic;

    public ServerController(){
        serverLogic = new ServerLogic();
    }

    public void setClientLabel(String text) {
        clientLabel.setText(text);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setList();
        new Thread(() -> serverLogic.createFiles(this)).start();
    }

    public void setList() {

        File directory = new File(ServerLogic.VIDEOS_DIR);
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (!videoListView.getItems().contains(file.getName()))
                    videoListView.getItems().add(file.getName());
            }
        }

        videoListView.getItems().sort(String::compareTo);
        videoListView.refresh();
    }

    public void setProgressBar(double progress){
        videoCreationProgress.setProgress(progress);
    }

    public void setVideosCreatedLabel(String text) {
        videosCreatedLabel.setStyle("-fx-text-fill: green");
        videosCreatedLabel.setText(text);
    }
}