module ru.gb.networkchat_v2 {
    requires javafx.controls;
    requires javafx.fxml;


    opens ru.gb.networkchat_v2 to javafx.fxml;
    exports ru.gb.networkchat_v2;
}