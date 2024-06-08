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
    private Label serverLabel;

    @FXML
    private Label speedtestLabel;
    @FXML
    private ProgressBar connectionProgress;

    @FXML
    private Label formatLabel;
    @FXML
    private ChoiceBox<String> formatBox;

    static final String[] FORMATS = {"avi", "mp4", "mkv"};
    static final String[] PROTOCOLS = {"auto","tcp", "udp", "rtp"};

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

        new Thread(() -> ClientLogic.testSpeed(this)).start();

        //when the speedtest is finished start the connection
        speedtestLabel.textProperty().addListener((_, _, t1) ->
                new Thread(() -> ClientLogic.connectionHandler(ClientController.this)).start()
        );

    }

    public void reset(){
        formatBox.setValue(null);
        videoList.getItems().clear();
        protocolBox.setValue(null);
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
    public Label getSpeedtestLabel(){
        return speedtestLabel;
    }

    public void setServerLabel(String text, String style) {
        serverLabel.setText(text);
        serverLabel.setStyle(style);
    }
    public void setConnectionTestLabel(String text, String style) {
        speedtestLabel.setText(text);
        speedtestLabel.setStyle(style);
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
}
