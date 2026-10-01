package org.passerbya.ui;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.passerbya.core.CommandManager;
import org.passerbya.core.PluginManager;
import org.passerbya.tools.PluginManifest;
import org.passerbya.ui.components.MenuBar;
import org.passerbya.ui.pages.HomePage;
import org.passerbya.ui.pages.PluginsPage;
import org.passerbya.ui.pages.SettingsPage;
import org.passerbya.ui.pages.TerminalPage;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MainGUI extends Application {

    private Stage primaryStage;
    private BorderPane root;
    private MenuBar menuBar;
    private CommandManager commandManager;
    private PluginManager pluginManager;
    private TextArea terminalOutput;
    private String currentPage;
    private PluginManifest selectedPlugin;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.pluginManager = new PluginManager();
        this.commandManager = new CommandManager();
        this.terminalOutput = new TextArea();
        terminalOutput.setEditable(false);

        pluginManager.load();

        initRoot();
        initMenu();
        initEvents();
        initSceneAndShow();
        loadCSS();

        commandManager.setOutputCallback(line -> Platform.runLater(() -> {
            if (terminalOutput != null) terminalOutput.appendText(line + "\n");
        }));
        commandManager.start();

        switchToPage("home", HomePage.create());
    }

    @Override
    public void stop() {
        if (commandManager != null) commandManager.stop();
    }

    // ---------- 初始化 ----------

    private void initRoot() {
        root = new BorderPane();
        root.getStyleClass().add("theme-dark");
    }

    private void initMenu() {
        menuBar = new MenuBar();
        root.setLeft(menuBar);
    }

    private void initEvents() {
        menuBar.aboutButton.setOnAction(e -> showAboutWindow());
        menuBar.homeButton.setOnAction(e -> switchToPage("home", HomePage.create()));
        menuBar.terminalButton.setOnAction(e ->
                switchToPage("terminal", TerminalPage.create(
                        pluginManager.getPlugins(), commandManager, terminalOutput, this::setSelectedPlugin)));
        menuBar.pluginsButton.setOnAction(e ->
                switchToPage("plugins", PluginsPage.create(
                        pluginManager.getPlugins(), this::setSelectedPlugin)));
        menuBar.settingsButton.setOnAction(e -> switchToPage("settings", SettingsPage.create()));
    }

    private void initSceneAndShow() {
        Scene scene = new Scene(root, 960, 540);
        primaryStage.setTitle("Command Manager");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadCSS() {
        Scene scene = primaryStage.getScene();
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());
    }

    // ---------- 页面切换动画 ----------

    private String getDirection(String target) {
        List<String> order = Arrays.asList("home", "terminal", "plugins", "settings");
        int cur = order.indexOf(currentPage);
        int tgt = order.indexOf(target);
        if (tgt > cur) return "up";
        if (tgt < cur) return "down";
        return "none";
    }

    private void animatePageIn(Node node, String direction) {
        node.setTranslateY(0);
        node.setOpacity(0);
        if ("up".equals(direction)) node.setTranslateY(30);
        else if ("down".equals(direction)) node.setTranslateY(-30);

        FadeTransition fade = new FadeTransition(Duration.millis(300), node);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(300), node);
        slide.setFromY(node.getTranslateY());
        slide.setToY(0);

        fade.play();
        slide.play();
    }

    private void switchToPage(String target, Node page) {
        if (currentPage != null && target.equals(currentPage)) return;
        String direction = getDirection(target);
        root.setCenter(page);
        animatePageIn(page, direction);
        currentPage = target;
    }

    // ---------- 其他 ----------

    private void setSelectedPlugin(PluginManifest plugin) {
        this.selectedPlugin = plugin;
    }

    private void showAboutWindow() {
        Stage aboutStage = new Stage();
        aboutStage.setTitle("关于");
        aboutStage.setWidth(400);
        aboutStage.setHeight(300);
        aboutStage.initModality(Modality.APPLICATION_MODAL);

        Label label = new Label(
                "Command Manager\n版本: 0.02.0000-alpha\n作者: 陈弘宇\n仓库：https://github.com/Lurenjia-PasserbyA/command-manager");
        label.getStyleClass().add("about-window");
        label.setAlignment(Pos.CENTER);

        VBox box = new VBox(label);
        box.setAlignment(Pos.CENTER);
        Scene scene = new Scene(box, 400, 300);
        aboutStage.setScene(scene);
        aboutStage.showAndWait();
    }
}