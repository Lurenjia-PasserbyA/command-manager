package org.passerbya.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
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

import java.util.Objects;

public class MainGUI extends Application {

    private Stage primaryStage;
    private BorderPane root;
    private MenuBar menuBar;
    private CommandManager commandManager;
    private PluginManager pluginManager;
    private AppSettings settings;
    private TextArea terminalOutput;
    private PageId currentPage;
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

        switchToPage(PageId.HOME, HomePage.create());
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
        menuBar.homeButton.setOnAction(e -> switchToPage(PageId.HOME, HomePage.create()));

        // 终端页带上当前选中的插件，这样在插件页点过某个插件后切过来是预选好的。
        // （之前 selectedPlugin 只写不读，跨页选择实际上没有任何效果。）
        menuBar.terminalButton.setOnAction(e -> switchToPage(PageId.TERMINAL, TerminalPage.create(
                pluginManager.getPlugins(),
                commandManager,
                terminalOutput,
                this::setSelectedPlugin,
                selectedPlugin)));

        menuBar.pluginsButton.setOnAction(e -> switchToPage(PageId.PLUGINS, PluginsPage.create(
                pluginManager.getPlugins(),
                this::setSelectedPlugin,
                pluginManager)));

        menuBar.settingsButton.setOnAction(e -> switchToPage(PageId.SETTINGS, SettingsPage.create(
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

    /** 动画时长，毫秒。 */
    private static final int ANIM_MILLIS = 300;

    /** 新页面入场时的纵向偏移量（正数表示从下方滑入）。 */
    private static final double SLIDE_OFFSET = 30;

    /**
     * 由两个页面的 id 差值决定滑动方向。
     *
     * <p>返回 "up" 表示目标页在菜单里更靠下 —— 视觉上像是整个内容往上走，
     * 所以新页面从下方滑入；"down" 反之。id 相同则不滑，只淡入。
     */
    private static String directionOf(PageId from, PageId to) {
        if (from == null) return "none";
        int delta = to.id() - from.id();
        if (delta > 0) return "up";
        if (delta < 0) return "down";
        return "none";
    }

    private void animatePageIn(Node node, String direction) {
        node.setOpacity(0);

        // "up" -> 从下方(+y)滑入；"down" -> 从上方(-y)滑入
        double fromY = 0;
        if ("up".equals(direction)) fromY = SLIDE_OFFSET;
        else if ("down".equals(direction)) fromY = -SLIDE_OFFSET;
        node.setTranslateY(fromY);

        // ease-out：起步快、收尾慢，切换看起来更跟手
        FadeTransition fade = new FadeTransition(Duration.millis(ANIM_MILLIS), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(Duration.millis(ANIM_MILLIS), node);
        slide.setFromY(fromY);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        fade.play();
        slide.play();
    }

    private void switchToPage(PageId target, Node page) {
        // 注意：同一页面重复点击时这里会直接返回，页面内容不会重建。
        // 目前各页面的内容都是无状态的，重建没有意义；等哪天有页面需要
        // 每次进入都刷新（比如插件列表），要改成重建而不是提前返回。
        if (target == currentPage) return;

        String direction = directionOf(currentPage, target);
        currentPage = target;

        root.setCenter(page);
        animatePageIn(page, direction);
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
