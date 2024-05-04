package com.github.dizarc.streaming;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;

public class ServerController {

    @FXML
    private Label videosCreatedLabel;
    @FXML
    private Label ClientLabel;

    @FXML
    private ListView<String> videoListView;
    @FXML
    private ScrollBar videoScrollBar;

}