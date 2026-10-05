package org.passerbya.ui.pages;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import org.passerbya.core.AppSettings;

import java.io.File;
import java.util.function.Consumer;

/**
 * 设置页：编辑 {@link AppSettings}。
 *
 * <p>这个类不碰持久化，也不碰 CommandManager。它只负责把表单读成一份 AppSettings，
 * 然后交给外部传入的 onSave 去落地并应用到运行时 —— 页面与副作用解耦。
 */
public class SettingsPage {

    private static final int MIN_FONT_SIZE = 8;
    private static final int MAX_FONT_SIZE = 32;
    private static final int MIN_SCROLLBACK = 100;
    private static final int MAX_SCROLLBACK = 100_000;

    /** 编码下拉的可选项。GBK / UTF-8 是 Windows 中文环境最常用的两个。 */
    private static final String[] CHARSETS = {"GBK", "UTF-8", "UTF-16LE", "ISO-8859-1"};

    public static BorderPane create(AppSettings settings, Consumer<AppSettings> onSave) {
        BorderPane page = new BorderPane();
        page.getStyleClass().add("settings-page");

        Label title = new Label("设置");
        title.getStyleClass().addAll("title", "title-text");

        // ---- 终端 ----
        TextField shellField = new TextField(settings.getShell());
        shellField.setPromptText("例如 powershell.exe 或 bash");
        shellField.getStyleClass().add("input-field");

        TextField argsField = new TextField(settings.getShellArgs());
        argsField.setPromptText("留空则按平台给默认值");
        argsField.getStyleClass().add("input-field");

        TextField workDirField = new TextField(settings.getWorkingDirectory());
        workDirField.setPromptText("留空则继承本程序的工作目录");
        workDirField.getStyleClass().add("input-field");

        Button browseWorkDir = new Button("浏览");
        browseWorkDir.getStyleClass().add("action-button");
        browseWorkDir.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooseInitialDirectory(workDirField.getText(), chooser);
            File dir = chooser.showDialog(windowOf(page));
            if (dir != null) workDirField.setText(dir.getAbsolutePath());
        });

        HBox workDirRow = new HBox(5, workDirField, browseWorkDir);
        HBox.setHgrow(workDirField, Priority.ALWAYS);

        ComboBox<String> charsetBox = new ComboBox<>();
        charsetBox.getItems().addAll(CHARSETS);
        charsetBox.setValue(settings.getCharset());
        charsetBox.getStyleClass().add("settings-combo");

        Spinner<Integer> fontSizeSpinner = new Spinner<>(
                MIN_FONT_SIZE, MAX_FONT_SIZE, clamp(settings.getTerminalFontSize(), MIN_FONT_SIZE, MAX_FONT_SIZE));
        fontSizeSpinner.setEditable(true);

        Spinner<Integer> scrollbackSpinner = new Spinner<>(
                MIN_SCROLLBACK, MAX_SCROLLBACK, clamp(settings.getScrollbackLimit(), MIN_SCROLLBACK, MAX_SCROLLBACK),
                500);
        scrollbackSpinner.setEditable(true);

        CheckBox cdToPlugins = new CheckBox("启动后自动切到插件目录");
        cdToPlugins.setSelected(settings.isCdToPluginsDirOnStart());

        GridPane grid = new GridPane();
        grid.getStyleClass().add("settings-grid");
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(4, 0, 4, 0));

        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(120);
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, fieldCol);

        int row = 0;
        addSection(grid, row++, "Shell");
        addField(grid, row++, "Shell 路径", shellField);
        addField(grid, row++, "启动参数", argsField);
        addField(grid, row++, "工作目录", workDirRow);
        addField(grid, row++, "输出编码", charsetBox);
        addField(grid, row++, "", cdToPlugins);
        addSection(grid, row++, "终端外观");
        addField(grid, row++, "字号", fontSizeSpinner);
        addField(grid, row++, "回滚行数上限", scrollbackSpinner);

        // ---- 底部操作条 ----
        Label status = new Label();
        status.getStyleClass().add("settings-status");

        Button saveBtn = new Button("保存");
        saveBtn.getStyleClass().addAll("action-button", "settings-save-button");
        Button resetBtn = new Button("恢复默认");
        resetBtn.getStyleClass().add("action-button");

        resetBtn.setOnAction(e -> {
            AppSettings defaults = new AppSettings();
            shellField.setText(defaults.getShell());
            argsField.setText(defaults.getShellArgs());
            workDirField.setText(defaults.getWorkingDirectory());
            charsetBox.setValue(defaults.getCharset());
            fontSizeSpinner.getValueFactory().setValue(defaults.getTerminalFontSize());
            scrollbackSpinner.getValueFactory().setValue(defaults.getScrollbackLimit());
            cdToPlugins.setSelected(defaults.isCdToPluginsDirOnStart());
            status.setText("已填入默认值，点保存生效");
        });

        saveBtn.setOnAction(e -> {
            String shell = shellField.getText().trim();
            if (shell.isEmpty()) {
                status.setText("Shell 路径不能为空");
                return;
            }

            String workDir = workDirField.getText().trim();
            if (!workDir.isEmpty() && !new File(workDir).isDirectory()) {
                status.setText("工作目录不存在：" + workDir);
                return;
            }

            settings.setShell(shell);
            settings.setShellArgs(argsField.getText());
            settings.setWorkingDirectory(workDir);
            settings.setCharset(charsetBox.getValue());
            settings.setTerminalFontSize(fontSizeSpinner.getValue());
            settings.setScrollbackLimit(scrollbackSpinner.getValue());
            settings.setCdToPluginsDirOnStart(cdToPlugins.isSelected());

            if (onSave != null) {
                onSave.accept(settings);
                status.setText("已保存");
            } else {
                status.setText("已更新（未提供保存回调）");
            }
        });

        HBox actions = new HBox(10, saveBtn, resetBtn, status);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getStyleClass().add("control-bar");

        Label hint = new Label("设置保存在 " + AppSettings.configFile().getAbsolutePath());
        hint.getStyleClass().add("settings-hint");

        VBox content = new VBox(10, title, grid, actions, hint);
        content.setPadding(new Insets(20));
        page.setCenter(content);
        return page;
    }

    // ---------- 内部辅助 ----------

    private static void addSection(GridPane grid, int row, String text) {
        Label section = new Label(text);
        section.getStyleClass().add("settings-section");
        grid.add(section, 0, row, 2, 1);
    }

    private static void addField(GridPane grid, int row, String labelText, javafx.scene.Node control) {
        if (!labelText.isEmpty()) {
            Label label = new Label(labelText);
            label.getStyleClass().add("settings-label");
            grid.add(label, 0, row);
        }
        grid.add(control, 1, row);
    }

    private static void chooseInitialDirectory(String path, DirectoryChooser chooser) {
        if (path == null || path.isBlank()) return;
        File dir = new File(path.trim());
        if (dir.isDirectory()) chooser.setInitialDirectory(dir);
    }

    /** 找到这个节点所属的窗口，用作对话框 owner；拿不到就返回 null。 */
    private static Window windowOf(javafx.scene.Node node) {
        return node.getScene() != null ? node.getScene().getWindow() : null;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
