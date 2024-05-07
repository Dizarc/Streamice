module com.github.dizarc.streaming {
    requires javafx.controls;
    requires javafx.fxml;
    requires ffmpeg;
    requires java.logging;
    requires org.apache.commons.lang3;
    requires jspeedtest;
    requires org.checkerframework.checker.qual;


    opens com.github.dizarc.streaming to javafx.fxml;
    exports com.github.dizarc.streaming;
}