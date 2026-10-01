package org.passerbya.ui.pages;

import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.passerbya.core.CommandManager;
import org.passerbya.tools.PluginManifest;
import org.passerbya.ui.components.ParamPanel;

import java.util.List;
import java.util.function.Consumer;

public class TerminalPage {

    public static BorderPane create(List<PluginManifest> pluginList,
                                    CommandManager commandManager,
                                    TextArea terminalOutput,
                                    Consumer<PluginManifest> onSelect) {

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

        paramPanel.setOnExecute(() -> {
            String cmd = paramPanel.buildCommand();
            if (cmd == null) {
                terminalOutput.appendText("Please select a plugin first.\n");
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
            if (!cmd.isEmpty()) {
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
}