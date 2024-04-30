module com.github.dizarc.streaming {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.github.dizarc.streaming to javafx.fxml;
    exports com.github.dizarc.streaming;
}