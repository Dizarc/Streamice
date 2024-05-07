package com.github.dizarc.streaming;

import java.io.File;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerController implements Initializable {

    @FXML
    private Label videosLabel;

    @FXML
    private Label clientLabel;

    @FXML
    private ListView<String> videoList;

    @FXML
    private ProgressBar videoCreationProgress;

    private ServerLogic serverLogic;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setList();
        new Thread(() -> ServerLogic.createFiles(this)).start();
    }

    public void setList() {

        File directory = new File(ServerLogic.VIDEOS_DIR);
        File[] files = directory.listFiles();

        String pattern = "^(\\w+)-(\\d+)p\\.(\\w+)$";
        Pattern fileNamePattern = Pattern.compile(pattern);

        if (files != null) {
            for (File file : files) {

                Matcher matcher = fileNamePattern.matcher(file.getName());
                if(matcher.matches()) {
                    if (!videoList.getItems().contains(file.getName()))

                        videoList.getItems().add(file.getName());
                }
            }
        }
        videoList.getItems().sort(String::compareTo);
        videoList.refresh();
    }

    public String[] getList(){
        return videoList.getItems().toArray(new String[0]);
    }
    public void setProgressBar(float progress){
        videoCreationProgress.setProgress(progress);
    }

    public void setVideosLabel(String text, String style) {
        videosLabel.setText(text);
        videosLabel.setStyle(style);
    }

    public void setClientLabel(String text, String style) {
        clientLabel.setText(text);
        clientLabel.setStyle(style);
    }
}