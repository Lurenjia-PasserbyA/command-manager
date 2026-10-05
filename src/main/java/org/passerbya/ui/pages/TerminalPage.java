package org.passerbya.ui.pages;

import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.passerbya.core.CommandManager;
import org.passerbya.tools.PluginManifest;
import org.passerbya.ui.components.ParamPanel;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class TerminalPage {

    /**
     * @param initialPlugin 打开这个页面时预先选中的插件，可为 null
     */
    public static BorderPane create(List<PluginManifest> pluginList,
                                    CommandManager commandManager,
                                    TextArea terminalOutput,
                                    Consumer<PluginManifest> onSelect,
                                    PluginManifest initialPlugin) {

        BorderPane page = new BorderPane();
        page.getStyleClass().add("terminal-page");

        // ---- 左侧插件列表 ----
        ListView<PluginManifest> pluginListView = new ListView<>();
        pluginListView.getItems().addAll(pluginList);
        pluginListView.getStyleClass().add("terminal-page-panel");

        pluginListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PluginManifest item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " (" + item.getId() + ")");
            }
        });

        // ---- 右侧参数面板 ----
        ParamPanel paramPanel = new ParamPanel(null);

        pluginListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                paramPanel.refresh(newVal);
                if (onSelect != null) onSelect.accept(newVal);
            }
        });

        // 从别的页面带过来的预选项：先渲染表单，再把列表选中
        if (initialPlugin != null) {
            int index = indexOf(pluginList, initialPlugin);
            if (index >= 0) {
                paramPanel.refresh(initialPlugin);
                pluginListView.getSelectionModel().select(index);
                pluginListView.scrollTo(index);
            }
        }

        paramPanel.setOnExecute(() -> {
            String cmd = paramPanel.buildCommand();
            if (cmd == null) {
                terminalOutput.appendText("请先在左侧选择一个插件。\n");
                return;
            }

            List<String> problems = paramPanel.validate();
            if (!problems.isEmpty()) {
                for (String problem : problems) {
                    terminalOutput.appendText("[缺少参数] " + problem + "\n");
                }
                return;
            }

            terminalOutput.appendText("▶ " + cmd + "\n");
            commandManager.executeCommand(cmd);
        });

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(pluginListView, paramPanel);
        splitPane.setDividerPositions(0.35);
        splitPane.getStyleClass().add("terminal-page-box");

        // ---- 底部终端 ----
        terminalOutput.getStyleClass().addAll("terminal-page-panel", "terminal-output-area");

        TextField inputField = new TextField();
        inputField.setPromptText("输入命令...");
        inputField.getStyleClass().addAll("action-button", "input-field");

        Button sendButton = new Button("发送");
        sendButton.getStyleClass().addAll("action-button", "send-button");

        Button clearButton = new Button("清空输出");
        clearButton.getStyleClass().addAll("action-button", "clear-button");
        clearButton.setOnAction(e -> terminalOutput.clear());

        sendButton.setOnAction(e -> {
            String cmd = inputField.getText();
            if (cmd != null && !cmd.isBlank()) {
                terminalOutput.appendText("▶ " + cmd + "\n");
                commandManager.executeCommand(cmd);
                inputField.clear();
            }
        });
        inputField.setOnAction(e -> sendButton.fire());

        HBox controlBar = new HBox(10, sendButton, clearButton, inputField);
        controlBar.getStyleClass().add("control-bar");

        VBox terminalArea = new VBox(10, terminalOutput, controlBar);

        // ---- 组装 ----
        BorderPane topHalf = new BorderPane();
        topHalf.setCenter(splitPane);

        BorderPane bottomHalf = new BorderPane();
        bottomHalf.setCenter(terminalArea);

        SplitPane mainSplit = new SplitPane();
        mainSplit.setOrientation(Orientation.VERTICAL);
        mainSplit.getItems().addAll(topHalf, bottomHalf);
        mainSplit.setDividerPositions(0.5);
        mainSplit.getStyleClass().addAll("terminal-page-panel", "terminal-split-pane");

        page.setCenter(mainSplit);
        return page;
    }

    /** 按 id 找插件下标；id 为空时退化为按对象同一性比较。 */
    private static int indexOf(List<PluginManifest> plugins, PluginManifest target) {
        if (plugins == null || target == null) return -1;
        String id = target.getId();
        for (int i = 0; i < plugins.size(); i++) {
            PluginManifest candidate = plugins.get(i);
            if (id != null ? id.equals(candidate.getId()) : Objects.equals(candidate, target)) {
                return i;
            }
        }
        return -1;
    }
}
