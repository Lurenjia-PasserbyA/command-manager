package org.passerbya.ui.pages;

import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import org.passerbya.tools.PluginManifest;

import java.util.List;
import java.util.function.Consumer;

public class PluginsPage {

    public static BorderPane create(List<PluginManifest> pluginList,
                                    Consumer<PluginManifest> onSelect) {
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

        Button deleteBtn = new Button();
        deleteBtn.getStyleClass().addAll("action-button", "delete-plugin-button");

        Button getNewBtn = new Button();
        getNewBtn.getStyleClass().addAll("action-button", "get-new-plugin-button");

        HBox controlBar = new HBox(10, deleteBtn, getNewBtn);
        controlBar.getStyleClass().add("control-bar");

        page.setLeft(pluginListView);
        page.setRight(controlBar);
        return page;
    }
}