module org.example.apppppppp {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.dlsc.formsfx;

    opens org.example.apppppppp to javafx.fxml;
    exports org.example.apppppppp;
}