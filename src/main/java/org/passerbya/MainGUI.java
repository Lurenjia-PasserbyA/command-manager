package org.passerbya;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.StageStyle;

import java.net.URI;
import java.util.ArrayList;
import java.util.Objects;

public class MainGUI extends Application {

    private BorderPane root;
    private Stage primaryStage;


    @Override
    public void start(Stage primaryStage){

        // 初始化
        root = new BorderPane();

        this.primaryStage = primaryStage;

        primaryStage.initStyle(StageStyle.UNDECORATED);

        // 定义菜单按钮
        Button menuHomeButton = new Button("主页");
        Button menuTerminalButton = new Button("终端");
        Button menuPluginsButton = new Button("插件");
        Button menuSettingsButton = new Button("设置");
        Button menuAboutButton = new Button("关于");

        // 启用事件
        menuAboutButton.setOnAction(e -> showAboutWindow());
        menuHomeButton.setOnAction(e -> root.setCenter(createHomePage()));
        menuTerminalButton.setOnAction(e -> root.setCenter(createTerminalPage()));
        menuPluginsButton.setOnAction(e -> root.setCenter(createPluginsPage()));
        menuSettingsButton.setOnAction(e -> root.setCenter(createSettingsPage()));


        // 定义菜单容器
        VBox Menu = new VBox(menuHomeButton, menuTerminalButton, menuPluginsButton, menuSettingsButton, menuAboutButton);

        // 设定root
        root.setLeft(Menu);
        root.setCenter(createHomePage());

        // 定义主窗口
        Scene scene = new Scene(root, 960, 540);

        // 定义窗口标题、区域及显示这个窗口
        primaryStage.setTitle("Command Manager");
        primaryStage.setScene(scene);
        primaryStage.show();

        // 加载CSS
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



    private BorderPane createHomePage() {
        ListView<String> quickAccess = new ListView<>();
        quickAccess.getItems().addAll("GitHub仓库", "插件市场", "创建会话");

        BorderPane page = new BorderPane();
        page.getStyleClass().addAll("home-page");

        Label title = new Label("Command Manager - Home");
        title.getStyleClass().addAll("title");

        return page;
    }

    private BorderPane createTerminalPage() {
        return null;
    }

    private BorderPane createPluginsPage() {
        return null;
    }

    private BorderPane createSettingsPage() {
        return null;
    }

    private void cunstomTitleBar () {
        HBox titlebar = new HBox();
        titlebar.getStyleClass().addAll("title-bar");


    }
}
