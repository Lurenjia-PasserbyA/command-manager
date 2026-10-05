package org.passerbya.ui.pages;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import org.passerbya.core.PluginManager;
import org.passerbya.debug.DebugLogger;
import org.passerbya.tools.PluginManifest;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class PluginsPage {

    public static BorderPane create(List<PluginManifest> pluginList,
                                    Consumer<PluginManifest> onSelect,
                                    PluginManager pluginManager) {
        BorderPane page = new BorderPane();
        page.getStyleClass().add("plugins-page");

        ListView<PluginManifest> pluginListView = new ListView<>();
        pluginListView.getItems().addAll(pluginList);
        pluginListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        pluginListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PluginManifest item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " (" + item.getId() + ")");
            }
        });

        pluginListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && onSelect != null) onSelect.accept(newVal);
        });

        Button deleteBtn = new Button("移除");
        deleteBtn.getStyleClass().addAll("action-button", "delete-plugin-button");
        deleteBtn.setOnAction(e -> {
            List<PluginManifest> selected =
                    List.copyOf(pluginListView.getSelectionModel().getSelectedItems());
            if (selected.isEmpty()) {
                info("请先选中要移除的插件。");
                return;
            }
            for (PluginManifest plugin : selected) {
                pluginManager.removePlugin(plugin);
            }
            pluginListView.getItems().removeAll(selected);
        });

        Button getNewBtn = new Button("获取新插件");
        getNewBtn.getStyleClass().addAll("action-button", "get-new-plugin-button");
        getNewBtn.setOnAction(e -> openPluginsFolder());

        HBox controlBar = new HBox(10, deleteBtn, getNewBtn);
        controlBar.getStyleClass().add("control-bar");

        page.setLeft(pluginListView);
        page.setRight(controlBar);
        return page;
    }

    /**
     * 打开插件目录，让用户把新插件文件夹拷进去。
     *
     * <p>这里刻意不去"下载"插件 —— 没有远端仓库可拉，装模作样地弹个进度条
     * 只会骗人。先把目录打开，比假装能获取更有用。
     */
    private static void openPluginsFolder() {
        File dir = PluginManager.pluginsDirectory();
        if (!dir.exists() && !dir.mkdirs()) {
            info("插件目录不存在，且无法创建：" + dir.getAbsolutePath());
            return;
        }

        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(dir);
            } else {
                info("请手动打开插件目录：\n" + dir.getAbsolutePath());
            }
        } catch (IOException | UnsupportedOperationException ex) {
            DebugLogger.error("Failed to open plugins folder", ex);
            info("无法自动打开插件目录，请手动前往：\n" + dir.getAbsolutePath());
        }
    }

    private static void info(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("插件");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
