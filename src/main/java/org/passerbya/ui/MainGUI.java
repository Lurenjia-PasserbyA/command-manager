package org.passerbya.ui;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.passerbya.core.AppSettings;
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
    private AppSettings settings;
    private TextArea terminalOutput;
    private String currentPage;
    private PluginManifest selectedPlugin;
    private int scrollbackLimit = 5000;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.settings = AppSettings.load();
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
        applyTerminalFont();
        setScrollbackLimit(settings.getScrollbackLimit());
        applyScrollbackCap();

        commandManager.setOutputCallback(line -> Platform.runLater(() -> {
            if (terminalOutput != null) terminalOutput.appendText(line + "\n");
        }));
        commandManager.start(settings);

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

        // 终端页带上当前选中的插件，这样在插件页点过某个插件后切过来是预选好的。
        // （之前 selectedPlugin 只写不读，跨页选择实际上没有任何效果。）
        menuBar.terminalButton.setOnAction(e -> switchToPage("terminal", TerminalPage.create(
                pluginManager.getPlugins(),
                commandManager,
                terminalOutput,
                this::setSelectedPlugin,
                selectedPlugin)));

        menuBar.pluginsButton.setOnAction(e -> switchToPage("plugins", PluginsPage.create(
                pluginManager.getPlugins(),
                this::setSelectedPlugin,
                pluginManager)));

        menuBar.settingsButton.setOnAction(e -> switchToPage("settings", SettingsPage.create(
                settings, this::onSettingsSaved)));
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

    // ---------- 设置生效 ----------

    /**
     * 设置页点了保存：落盘，并把能热更新的部分立刻应用。
     *
     * <p>shell 路径/参数/编码要重启才生效 —— 常驻进程已按旧配置起来了，
     * 这里如实告诉用户，而不是假装已经生效。
     */
    private void onSettingsSaved(AppSettings updated) {
        boolean saved = updated.save();
        this.settings = updated;

        applyTerminalFont();
        setScrollbackLimit(updated.getScrollbackLimit());
        applyScrollbackCap();

        showInfo(saved
                ? "设置已保存。\n\nShell 路径、启动参数、工作目录与编码的改动需要重启程序才会生效。"
                : "设置已在本次运行中更新，但写入配置文件失败，重启后会丢失。\n详见控制台日志。");
    }

    private void applyTerminalFont() {
        if (terminalOutput == null || settings == null) return;
        terminalOutput.setStyle("-fx-font-size: " + settings.getTerminalFontSize() + "px;");
    }

    private void setScrollbackLimit(int limit) {
        this.scrollbackLimit = Math.max(100, limit);
    }

    /** 文本超出上限时，从头部整行地丢弃。 */
    private void applyScrollbackCap() {
        terminalOutput.textProperty().addListener((obs, oldText, newText) -> {
            int lines = countLines(newText);
            if (lines <= scrollbackLimit) return;

            int excess = lines - scrollbackLimit;
            int cut = 0;
            for (int i = 0; i < excess; i++) {
                int next = newText.indexOf('\n', cut);
                if (next < 0) return;
                cut = next + 1;
            }
            terminalOutput.deleteText(0, cut);
        });
    }

    private static int countLines(String text) {
        if (text == null || text.isEmpty()) return 0;
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') lines++;
        }
        return lines;
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

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(primaryStage);
        alert.setTitle("设置");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAboutWindow() {
        Stage aboutStage = new Stage();
        aboutStage.setTitle("关于");
        aboutStage.setWidth(400);
        aboutStage.setHeight(300);
        aboutStage.initModality(Modality.APPLICATION_MODAL);
        aboutStage.initOwner(primaryStage);

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
