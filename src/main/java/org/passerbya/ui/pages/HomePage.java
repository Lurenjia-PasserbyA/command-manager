package org.passerbya.ui.pages;

import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;

public class HomePage {

    public static BorderPane create() {
        ListView<String> quickAccess = new ListView<>();
        quickAccess.getItems().addAll("GitHub仓库", "插件市场", "创建会话");

        Label title = new Label("Command Manager - Home");
        title.getStyleClass().addAll("title", "title-text");

        BorderPane page = new BorderPane();
        page.getStyleClass().add("home-page");
        page.setTop(title);
        page.setCenter(quickAccess);
        return page;
    }
}