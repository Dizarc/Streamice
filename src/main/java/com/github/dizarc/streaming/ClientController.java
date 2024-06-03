package com.github.dizarc.streaming;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class ClientController implements Initializable {

    @FXML
    private Label serverConnectLabel;

    @FXML
    private Label connectionTestLabel;

    @FXML
    private ProgressBar connectionProgress;

    @FXML
    private Label formatChoiceLabel;

    @FXML
    private ChoiceBox<String> formatChoiceBox;

    static final String[] FORMATS = {"avi", "mp4", "mkv"};

    @FXML
    private ListView<String> videoList;

    @FXML
    private Label videoLabel;

    private ClientLogic clientLogic;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        formatChoiceBox.getItems().addAll(FORMATS);
        ClientLogic.testSpeed(this);
    }

    public void setServerConnectLabel(String text) {
        serverConnectLabel.setText(text);
        serverConnectLabel.setStyle("-fx-text-fill: green");
    }

    public void setConnectionTestLabel(String text, String style) {
        connectionTestLabel.setText(text);
        connectionTestLabel.setStyle(style);
    }

    public void setTestProgressBar(float progress){
        connectionProgress.setProgress(progress);
    }

    public void setVisibility(){
        formatChoiceLabel.setVisible(true);
        formatChoiceBox.setVisible(true);
        videoList.setVisible(true);
        videoLabel.setVisible(true);
    }

    public void setVideoList(ArrayList<String> fileNames){
        for(String fileName : fileNames){
            videoList.getItems().add(fileName);
        }
    }

    public ListView<String> getVideoList() {
        return videoList;
    }

    public String getFormatChoice(){
       return formatChoiceBox.getValue();
    }
}
