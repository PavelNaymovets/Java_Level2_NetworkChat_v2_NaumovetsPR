module ru.gb.networkchat_v2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.slf4j;

    exports ru.gb.networkchat_v2.client;
    opens ru.gb.networkchat_v2.client to javafx.fxml;
}