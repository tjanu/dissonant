module com.github.tjanu.dissonant {
    requires javafx.controls;
    requires javafx.fxml;
    opens com.github.tjanu.dissonant to javafx.graphics, javafx.fxml;
    opens com.github.tjanu.dissonant.gui to javafx.graphics, javafx.fxml;
}