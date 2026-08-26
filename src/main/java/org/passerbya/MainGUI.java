package org.passerbya;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.Objects;

public class MainGUI extends Application {

    private BorderPane root;
    private Stage primaryStage;


    @Override
    public void start(Stage primaryStage){

        // 初始化root
        root = new BorderPane();

        this.primaryStage = primaryStage;

        // 定义菜单按钮
        Button menuHomeButton = new Button("主页");
        Button menuTerminalButton = new Button("终端");
        Button menuPluginsButton = new Button("插件");
        Button menuSettingsButton = new Button("设置");
        Button menuAboutButton = new Button("关于");

        VBox Menu = new VBox(menuHomeButton, menuTerminalButton, menuPluginsButton, menuSettingsButton, menuAboutButton);

        Scene scene = new Scene(Menu, 960, 540);

        primaryStage.setTitle("Command Manager");

        primaryStage.setScene(scene);

        primaryStage.show();

        menuAboutButton.setOnAction(e -> showAboutWindow());
        menuHomeButton.setOnAction(e -> root.setCenter(createHomePage()));

        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());

    }

    private void showAboutWindow () {
        Stage aboutWindow = new Stage();
        aboutWindow.initOwner(primaryStage);
        aboutWindow.setTitle("关于");
        VBox content = new VBox(20);

        Scene scene = new Scene(content, 400, 300);

        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());

        aboutWindow.setScene(scene);
        aboutWindow.showAndWait();
    }

    private VBox createHomePage() {
        VBox page = new VBox(20);
        page.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());
        page.getChildren().addAll(
                new Label("欢迎使用 Command Manager"),
                new Label("左侧选择功能开始使用")
        );
        page.getStyleClass().add("home-page");
        return page;
    }



}
