package org.passerbya;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Objects;

public class MainGUI extends Application {

    // 类变量
    private BorderPane root;
    private Stage primaryStage;
    private Button menuHomeButton;
    private Button menuTerminalButton;
    private Button menuPluginsButton;
    private Button menuSettingsButton;
    private Button menuAboutButton;

    // start()主方法
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        initRoot();
        initMenuButtons();
        initMenuContainer();
        initEvents();
        initSceneAndShow();
        loadCSS();
        customTitleBar();
    }

    // 初始化方法
    private void initRoot() {
        root = new BorderPane();
        primaryStage.initStyle(StageStyle.UNDECORATED);
    }

    private void initMenuButtons() {
        menuHomeButton = new Button("主页");
        menuHomeButton.getStyleClass().addAll("menu-home-button");
        menuTerminalButton = new Button("终端");
        menuTerminalButton.getStyleClass().addAll("menu-terminal-button");
        menuPluginsButton = new Button("插件");
        menuPluginsButton.getStyleClass().addAll("menu-plugins-button");
        menuSettingsButton = new Button("设置");
        menuSettingsButton.getStyleClass().addAll("menu-settings-button");
        menuAboutButton = new Button("关于");
        menuAboutButton.getStyleClass().addAll("menu-about-button");
    }

    private void initMenuContainer() {
        VBox Menu = new VBox(menuHomeButton, menuTerminalButton, menuPluginsButton, menuSettingsButton, menuAboutButton);
        root.setLeft(Menu);
        root.setCenter(createHomePage());
    }

    private void initEvents() {
        menuAboutButton.setOnAction(e -> showAboutWindow());
        menuHomeButton.setOnAction(e -> root.setCenter(createHomePage()));
        menuTerminalButton.setOnAction(e -> root.setCenter(createTerminalPage()));
        menuPluginsButton.setOnAction(e -> root.setCenter(createPluginsPage()));
        menuSettingsButton.setOnAction(e -> root.setCenter(createSettingsPage()));
    }

    private void initSceneAndShow() {
        Scene scene = new Scene(root, 960, 540);
        primaryStage.setTitle("Command Manager");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadCSS() {
        Scene scene = primaryStage.getScene();
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());
    }

    // 窗口方法
    private void showAboutWindow() {
        Stage aboutWindow = new Stage();
        aboutWindow.initOwner(primaryStage);
        aboutWindow.setTitle("关于");
        VBox content = new VBox(20);

        Scene scene = new Scene(content, 400, 300);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());

        aboutWindow.setScene(scene);
        aboutWindow.showAndWait();
    }

    // 页面创建方法
    private BorderPane createHomePage() {
        ListView<String> quickAccess = new ListView<>();
        quickAccess.getItems().addAll("GitHub仓库", "插件市场", "创建会话");

        Label title = new Label("Command Manager - Home");
        title.getStyleClass().addAll("title");
        title.getStyleClass().addAll("title-text");

        BorderPane page = new BorderPane();
        page.getStyleClass().addAll("home-page");

        page.setTop(title);
        page.setCenter(quickAccess);

        return page;
    }

    private BorderPane createTerminalPage() {
        BorderPane page = new BorderPane();
        page.getStyleClass().addAll("terminal-page");

        return page;
    }

    private BorderPane createPluginsPage() {
        return null;
    }

    private BorderPane createSettingsPage() {
        return null;
    }

    // 标题栏
    private void customTitleBar() {
        HBox titleBar = new HBox();
        titleBar.getStyleClass().addAll("title-bar");
        titleBar.getStyleClass().addAll("window-bar");

        Label titleLabel = new Label("Command Manager v1.00.0000-alpha");
        titleLabel.getStyleClass().addAll("title-label");

        Button maximizeButton = new Button("▢");
        maximizeButton.getStyleClass().addAll("maximize-window-button");
        maximizeButton.setOnAction(e -> primaryStage.setMaximized(!primaryStage.isMaximized()));

        Button minimizeButton = new Button("-");
        minimizeButton.getStyleClass().addAll("minimize-window-button");
        minimizeButton.setOnAction( e -> primaryStage.setIconified(true));

        Button closeButton = new Button("×");
        closeButton.setOnAction( e -> primaryStage.close());

        HBox leftArea = new HBox(titleLabel);
        leftArea.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(leftArea, Priority.ALWAYS);

        HBox rightArea = new HBox(10, minimizeButton, maximizeButton, closeButton);
        rightArea.setAlignment(Pos.CENTER_RIGHT);

        titleBar.getChildren().addAll(leftArea, rightArea);

        titleBar.setOnMousePressed(e -> {
            titleBar.setUserData(new double[]{e.getSceneX(), e.getSceneY()});
        });

        titleBar.setOnMouseDragged(e -> {
            double[] offset = (double[]) titleBar.getUserData();
            if (offset != null) {
                primaryStage.setX(e.getScreenX() - offset[0]);
                primaryStage.setY(e.getScreenY() - offset[1]);
            }
        });

        root.setTop(titleBar);
    }
}