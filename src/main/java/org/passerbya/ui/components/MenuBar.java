package org.passerbya.ui.components;

import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

/**
 * 左侧菜单栏，只负责外观与回调绑定。
 */
public class MenuBar extends VBox {

    public final Button homeButton = new Button("主页");
    public final Button terminalButton = new Button("终端");
    public final Button pluginsButton = new Button("插件");
    public final Button settingsButton = new Button("设置");
    public final Button aboutButton = new Button("关于");

    public MenuBar() {
        super(0);
        homeButton.getStyleClass().add("menu-button");
        terminalButton.getStyleClass().add("menu-button");
        pluginsButton.getStyleClass().add("menu-button");
        settingsButton.getStyleClass().add("menu-button");
        aboutButton.getStyleClass().add("menu-button");
        getChildren().addAll(homeButton, terminalButton, pluginsButton, settingsButton, aboutButton);
    }
}