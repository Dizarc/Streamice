package com.github.dizarc.streaming;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.fxml.Initializable;
import javafx.scene.control.ProgressBar;

import java.net.URL;
import java.util.ResourceBundle;

public class ClientController implements Initializable {

    @FXML
    private Label serverConnectLabel;

    @FXML
    private Label connectionTestLabel;

    @FXML
    private ProgressBar connectionProgress;

    private ClientLogic clientLogic;

    public ClientController(){
        clientLogic = new ClientLogic();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        new Thread(() -> clientLogic.connectToServer(this)).start();
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
}
