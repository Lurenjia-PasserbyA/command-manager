package org.passerbya;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainGUI extends Application {

    @Override
    public void start(Stage primaryStage){

        Button MenuHomeButton = new Button("主页");
        Button MenuTerminalButton = new Button("终端");

        VBox Menu = new VBox(MenuHomeButton, MenuTerminalButton);

        Scene scene = new Scene(Menu, 960, 540);

        primaryStage.setTitle("Command Manager");

        primaryStage.setScene(scene);

        primaryStage.show();
    }

}
