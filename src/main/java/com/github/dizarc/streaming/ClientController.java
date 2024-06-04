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
    private Label formatLabel;
    @FXML
    private ChoiceBox<String> formatBox;

    static final String[] FORMATS = {"avi", "mp4", "mkv"};
    static final String[] PROTOCOLS = {"auto","TCP", "UDP", "RTP"};

    @FXML
    private Label videoLabel;
    @FXML
    private ListView<String> videoList;

    @FXML
    private Label protocolLabel;
    @FXML
    private ChoiceBox<String> protocolBox;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        formatBox.getItems().addAll(FORMATS);
        protocolBox.getItems().addAll(PROTOCOLS);

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

    public void setFormatDisable(boolean flag){
        formatLabel.setDisable(flag);
        formatBox.setDisable(flag);
    }

    public void setVideoDisable(boolean flag){
        videoLabel.setDisable(flag);
        videoList.setDisable(flag);
    }

    public void setProtocolDisable(boolean flag){
        protocolLabel.setDisable(flag);
        protocolBox.setDisable(flag);
    }

    public void setVideoList(ArrayList<String> fileNames){
        for(String fileName : fileNames){
            videoList.getItems().add(fileName);
        }
    }

    public ListView<String> getVideoList() {
        return videoList;
    }
    public ChoiceBox<String> getFormatBox(){
        return formatBox;
    }
    public ChoiceBox<String> getProtocolBox(){
        return protocolBox;
    }

}
