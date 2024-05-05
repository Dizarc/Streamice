module com.github.dizarc.streaming {
    requires javafx.controls;
    requires javafx.fxml;
    requires ffmpeg;


    opens com.github.dizarc.streaming to javafx.fxml;
    exports com.github.dizarc.streaming;
}