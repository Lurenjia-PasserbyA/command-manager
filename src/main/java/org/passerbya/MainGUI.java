package org.passerbya;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javafx.geometry.Insets;
import javafx.stage.FileChooser;
import javafx.scene.control.SplitPane;
import java.io.File;

public class MainGUI extends Application {

    // 类变量
    private BorderPane root;
    private Stage primaryStage;
    private Button menuHomeButton;
    private Button menuTerminalButton;
    private Button menuPluginsButton;
    private Button menuSettingsButton;
    private Button menuAboutButton;
    private CommandManager commandManager;
    private TextArea terminalOutput;
    private String currentPage;
    private List<PluginManifest> pluginList;
    private PluginManifest selectedPlugin = null;
    private ListView<PluginManifest> terminalPluginListView;

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
        loadPlugins();
        commandManager = new CommandManager();
        terminalOutput = new TextArea();
        terminalOutput.setEditable(false);
        terminalOutput = new TextArea();
        terminalOutput.setEditable(false);

        commandManager.setOutputCallback(line -> Platform.runLater(() -> {
            if (terminalOutput != null) terminalOutput.appendText(line + "\n");
        }));

        commandManager.start();
    }

    // 初始化方法
    private void initRoot() {
        root = new BorderPane();
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
        currentPage = "home";
    }

    private void initEvents() {
        menuAboutButton.setOnAction(e -> showAboutWindow());
        menuHomeButton.setOnAction(e -> switchToPage("home", createHomePage()));
        menuTerminalButton.setOnAction(e -> switchToPage("terminal", createTerminalPage()));
        menuPluginsButton.setOnAction(e -> switchToPage("plugins", createPluginsPage()));
        menuSettingsButton.setOnAction(e -> switchToPage("settings", createSettingsPage()));
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

    // 页面切换动画方向设定
    private String getDirection(String target) {
        List<String> order = Arrays.asList("home", "terminal", "plugins", "settings");
        int currentIndex = order.indexOf(currentPage);
        int targetIndex = order.indexOf(target);

        if (targetIndex > currentIndex) {
            return "up";
        } else if (targetIndex < currentIndex) {
            return "down";
        } else {
            return "none";
        }
    }

    // 切换动画
    private void animatePageIn(Node node, String direction) {
        node.setTranslateY(0);
        node.setOpacity(0);

        if ("up".equals(direction)) {
            node.setTranslateY(30);
        } else if ("down".equals(direction)) {
            node.setTranslateY(-30);
        } else {
            node.setTranslateY(0);
        }

        // 淡入 + 滑入
        FadeTransition fade = new FadeTransition(Duration.millis(300), node);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(300), node);
        slide.setFromY(node.getTranslateY());
        slide.setToY(0);

        fade.play();
        slide.play();
    }

    // 整合
    private void switchToPage(String target, Node page) {
        if (currentPage != null && target.equals(currentPage)) return;

        String direction = getDirection(target);
        root.setCenter(page);
        animatePageIn(page, direction);
        currentPage = target;
    }

    // 窗口方法
    private void showAboutWindow() {
        Stage aboutStage = new Stage();
        aboutStage.setTitle("关于");
        aboutStage.setWidth(400);
        aboutStage.setHeight(300);
        aboutStage.initModality(Modality.APPLICATION_MODAL);

        Label label = new Label();
        label.setText("Command Manager\n版本: 0.02.0000-alpha\n作者: 陈弘宇\n仓库：https://github.com/Lurenjia-PasserbyA/command-manager");
        label.getStyleClass().addAll("about-window");
        label.setAlignment(Pos.CENTER);

        VBox root = new VBox(label);
        root.setAlignment(Pos.CENTER);
        Scene scene = new Scene(root, 400, 300);
        aboutStage.setScene(scene);
        aboutStage.showAndWait();
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

        // 插件列表
        terminalPluginListView = new ListView<>();
        terminalPluginListView.getItems().addAll(pluginList);

        terminalPluginListView.setCellFactory(lv -> new ListCell<PluginManifest>() {
            @Override
            protected void updateItem(PluginManifest item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " (" + item.getId() + ")");
                }
            }
        });

        terminalPluginListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedPlugin = newVal;
                refreshTerminalParamPanel();
            }
        });

        // 参数面板
        VBox rightPanel = new VBox(15);
        rightPanel.getStyleClass().add("right-panel");
        rightPanel.setPadding(new Insets(20));

        Label titleLabel = new Label("📦 插件参数");
        titleLabel.getStyleClass().add("param-title");

        VBox paramFieldsContainer = new VBox(10);
        paramFieldsContainer.setId("terminalParamFields");

        Button executeBtn = new Button("▶ 执行");
        executeBtn.getStyleClass().add("execute-btn");
        executeBtn.setOnAction(e -> executeSelectedPluginFromTerminal());

        rightPanel.getChildren().addAll(titleLabel, paramFieldsContainer, executeBtn);

        // 组装左右布局
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(terminalPluginListView, rightPanel);
        splitPane.setDividerPositions(0.35);

        // 终端输出区
        terminalOutput.getStyleClass().addAll("terminal-output-area");

        TextField inputField = new TextField();
        inputField.setPromptText("输入命令...");
        inputField.getStyleClass().addAll("input-field");

        Button sendButton = new Button("发送");
        sendButton.getStyleClass().addAll("send-button");

        Button clearButton = new Button("清空输出");
        clearButton.getStyleClass().addAll("clear-button");

        clearButton.setOnAction(e -> terminalOutput.clear());

        sendButton.setOnAction(e -> {
            String cmd = inputField.getText();
            if (!cmd.isEmpty()) {
                commandManager.executeCommand(cmd);
                inputField.clear();
            }
        });

        inputField.setOnAction(e -> sendButton.fire());

        HBox controlBar = new HBox(10, sendButton, clearButton, inputField);
        controlBar.getStyleClass().addAll("control-bar");

        VBox terminalArea = new VBox(10, terminalOutput, controlBar);

        // ========== 最终组装 ==========
        BorderPane topHalf = new BorderPane();
        topHalf.setCenter(splitPane);

        BorderPane bottomHalf = new BorderPane();
        bottomHalf.setCenter(terminalArea);

        SplitPane mainSplit = new SplitPane();
        mainSplit.setOrientation(javafx.geometry.Orientation.VERTICAL);
        mainSplit.getItems().addAll(topHalf, bottomHalf);
        mainSplit.setDividerPositions(0.5);

        page.setCenter(mainSplit);

        refreshTerminalParamPanel();

        return page;
    }

    private BorderPane createPluginsPage() {
        BorderPane page = new BorderPane();
        page.getStyleClass().addAll("plugins-page");

        ListView<PluginManifest> pluginListView = new ListView<>();

        pluginListView.getItems().addAll(pluginList);

        pluginListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        pluginListView.setCellFactory(lv -> new ListCell<PluginManifest>() {
            @Override
            protected void updateItem(PluginManifest item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " (" + item.getId() + ")");
                }
            }
        });

        pluginListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedPlugin = newVal;  // 存整个对象
                System.out.println("✅ 选中插件: " + selectedPlugin.getName());
                // 如果你想让终端页立即刷新参数表单，可以在这里调用刷新方法
                refreshTerminalParamPanel();
            }
        });

        Button deletePluginButton = new Button();
        deletePluginButton.getStyleClass().addAll("delete-plugin-button");

        Button getNewPluginButton = new Button();
        getNewPluginButton.getStyleClass().addAll("get-new-plugin-button");

        HBox controlBar = new HBox(10, deletePluginButton, getNewPluginButton);
        controlBar.getStyleClass().addAll("control-bar");

        page.setLeft(pluginListView);
        page.setRight(controlBar);
        return page;
    }

    private BorderPane createSettingsPage() {
        BorderPane page = new BorderPane();
        page.getStyleClass().addAll("settings-page");

        return null;
    }

    private void loadPlugins() {
        PluginsLoader loader = new PluginsLoader();
        loader.loadPlugins();

        pluginList = loader.getLoadedPlugins();

        System.out.print("✅ 成功加载 " + pluginList.size() + " 个插件");
    }

    public void stop() {
        if (commandManager != null) {
            commandManager.stop();
        }
    }

    private Node createControlForParameter(PluginParameter param) {
        String type = param.getType() != null ? param.getType() : "text";

        switch (type) {
            case "select":
                ComboBox<String> comboBox = new ComboBox<>();
                if (param.getOptions() != null) {
                    comboBox.getItems().addAll(param.getOptions());
                }
                if (!comboBox.getItems().isEmpty()) {
                    comboBox.getSelectionModel().selectFirst();
                }
                comboBox.setPromptText("请选择");
                return comboBox;

            case "file":
                HBox fileRow = new HBox(5);
                TextField fileField = new TextField();
                fileField.setPromptText("选择文件...");
                Button browseButton = new Button("浏览");
                browseButton.setOnAction(e -> {
                    FileChooser fileChooser = new FileChooser();
                    File file = fileChooser.showOpenDialog(primaryStage);
                    if (file != null) {
                        fileField.setText(file.getAbsolutePath());
                    }
                });
                fileRow.getChildren().addAll(fileField, browseButton);
                return fileRow;

            case "number":
                TextField numberField = new TextField();
                numberField.setPromptText("请输入数字");
                return numberField;

            case "checkbox":
                CheckBox checkBox = new CheckBox();
                checkBox.setText("启用");
                return checkBox;

            case "flag":
                CheckBox flagCheckBox = new CheckBox();
                flagCheckBox.setText("启用");
                return flagCheckBox;

            case "text":
            default:
                TextField textField = new TextField();
                textField.setPromptText("请输入 " + param.getLabel());
                return textField;
        }
    }

    private void executeSelectedPluginFromTerminal() {
        if (selectedPlugin == null) {
            terminalOutput.appendText("⚠️ 请先在左侧选择一个插件\n");
            return;
        }

        Node container = root.lookup("#terminalParamFields");
        if (!(container instanceof VBox vbox)) {
            return;
        }

        StringBuilder cmd = new StringBuilder();
        cmd.append(selectedPlugin.getExecutable());

        // 先收集所有参数的值
        java.util.Map<String, String> values = new java.util.HashMap<>();
        for (Node child : vbox.getChildren()) {
            if (!(child instanceof HBox row)) continue;
            if (row.getChildren().size() < 2) continue;

            Node control = row.getChildren().get(1);
            String value = extractValueFromControl(control);

            String paramName = "";
            if (row.getChildren().get(0) instanceof Label label) {
                paramName = label.getText();
                if (paramName.endsWith(" *")) {
                    paramName = paramName.substring(0, paramName.length() - 2);
                }
            }

            if (value != null && !value.isEmpty()) {
                values.put(paramName, value);
            }
        }

        // 遍历所有参数，处理 flag 类型
        for (PluginParameter param : selectedPlugin.getParameters()) {
            if ("flag".equals(param.getType())) {
                boolean checked = isCheckboxChecked(param.getName());
                if (checked && param.getFlagValue() != null) {
                    cmd.append(" ").append(param.getFlagValue());
                }
            }
        }

        // 拼接非 flag 参数
        for (PluginParameter param : selectedPlugin.getParameters()) {
            if ("flag".equals(param.getType())) continue;

            String paramName = param.getLabel();
            if (param.isRequired() && paramName.endsWith(" *")) {
                paramName = paramName.substring(0, paramName.length() - 2);
            }
            String value = values.get(paramName);
            if (value != null && !value.isEmpty()) {
                if (value.contains(" ")) {
                    value = "\"" + value + "\"";
                }
                cmd.append(" ").append(value);
            }
        }

        terminalOutput.appendText("▶ " + cmd.toString() + "\n");
        commandManager.executeCommand(cmd.toString());
    }

    private void refreshTerminalParamPanel() {
        Node node = root.lookup("#terminalParamFields");
        if (!(node instanceof VBox container)) {
            return;
        }

        container.getChildren().clear();

        if (selectedPlugin == null) {
            Label emptyLabel = new Label("⚠请从左侧列表选择一个插件");
            emptyLabel.getStyleClass().add("param-empty-label");
            container.getChildren().add(emptyLabel);
            return;
        }

        List<PluginParameter> params = selectedPlugin.getParameters();
        if (params == null || params.isEmpty()) {
            Label noParamLabel = new Label("此插件无需参数，直接点击执行即可");
            noParamLabel.getStyleClass().add("param-empty-label");
            container.getChildren().add(noParamLabel);
            return;
        }

        for (PluginParameter param : params) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);

            Label label = new Label(param.getLabel() + (param.isRequired() ? " *" : ""));
            label.getStyleClass().add("param-label");

            Node control = createControlForParameter(param);
            row.getChildren().addAll(label, control);
            container.getChildren().add(row);
        }
    }

    private String extractValueFromControl(Node control) {
        if (control instanceof TextField tf) {
            return tf.getText();
        } else if (control instanceof ComboBox<?> cb) {
            return cb.getValue() != null ? cb.getValue().toString() : "";
        } else if (control instanceof HBox fileRow) {
            for (Node child : fileRow.getChildren()) {
                if (child instanceof TextField tf) {
                    return tf.getText();
                }
            }
            return "";
        } else if (control instanceof CheckBox cb) {
            return String.valueOf(cb.isSelected());
        }
        return "";
    }

    private boolean isCheckboxChecked(String paramName) {
        Node container = root.lookup("#terminalParamFields");
        if (!(container instanceof VBox vbox)) return false;

        for (Node child : vbox.getChildren()) {
            if (!(child instanceof HBox row)) continue;
            for (Node subNode : row.getChildren()) {
                if (subNode instanceof CheckBox cb) {
                    // 找到这个 CheckBox 对应的 Label
                    for (Node sibling : row.getChildren()) {
                        if (sibling instanceof Label label) {
                            String text = label.getText();
                            if (text.endsWith(" *")) {
                                text = text.substring(0, text.length() - 2);
                            }
                            if (text.equals(paramName)) {
                                return cb.isSelected();
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}

