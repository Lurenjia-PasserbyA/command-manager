package org.passerbya.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.passerbya.tools.PluginManifest;
import org.passerbya.tools.PluginParameter;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件参数面板：根据选中的插件动态渲染表单，并负责收集/拼装命令行。
 */
public class ParamPanel extends VBox {

    private final Window owner;
    private PluginManifest selectedPlugin;
    private final VBox fieldsContainer = new VBox(10);
    private final Button executeBtn = new Button("执行");
    private Runnable onExecute;

    public ParamPanel(Window owner) {
        this.owner = owner;
        setSpacing(15);
        setPadding(new Insets(20));
        getStyleClass().add("terminal-page-panel");

        Label titleLabel = new Label("插件参数");
        titleLabel.getStyleClass().add("param-title");

        fieldsContainer.setId("terminalParamFields");

        executeBtn.getStyleClass().add("execute-btn");
        executeBtn.setOnAction(e -> {
            if (onExecute != null) onExecute.run();
        });

        getChildren().addAll(titleLabel, fieldsContainer, executeBtn);
        refresh(null);
    }

    public void setOnExecute(Runnable onExecute) {
        this.onExecute = onExecute;
    }

    public void refresh(PluginManifest plugin) {
        this.selectedPlugin = plugin;
        fieldsContainer.getChildren().clear();

        if (plugin == null) {
            Label empty = new Label("Please select a plugin from the list on the left");
            empty.getStyleClass().add("param-empty-label");
            fieldsContainer.getChildren().add(empty);
            return;
        }

        List<PluginParameter> params = plugin.getParameters();
        if (params == null || params.isEmpty()) {
            Label noParam = new Label("This plugin has no required parameters. You can execute it directly.");
            noParam.getStyleClass().add("param-empty-label");
            fieldsContainer.getChildren().add(noParam);
            return;
        }

        for (PluginParameter param : params) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);

            Label label = new Label(param.getLabel() + (param.isRequired() ? " *" : ""));
            label.getStyleClass().add("param-label");

            Node control = createControlForParameter(param);
            row.getChildren().addAll(label, control);
            fieldsContainer.getChildren().add(row);
        }
    }

    /**
     * 根据当前表单，拼装出完整命令行字符串。若无选中插件则返回 null。
     */
    public String buildCommand() {
        if (selectedPlugin == null) return null;

        StringBuilder cmd = new StringBuilder(selectedPlugin.getExecutable());

        Map<String, String> values = collectValues();

        // flag 类参数
        for (PluginParameter param : selectedPlugin.getParameters()) {
            if ("flag".equals(param.getType())) {
                if (isCheckboxChecked(param.getName()) && param.getFlagValue() != null) {
                    cmd.append(" ").append(param.getFlagValue());
                }
            }
        }

        // 非 flag 类参数
        for (PluginParameter param : selectedPlugin.getParameters()) {
            if ("flag".equals(param.getType())) continue;

            String label = param.getLabel();
            String value = values.get(label);
            if (value != null && !value.isEmpty()) {
                if (value.contains(" ")) {
                    value = "\"" + value + "\"";
                }
                cmd.append(" ").append(value);
            }
        }

        return cmd.toString();
    }

    // ---------- 内部辅助 ----------

    private Map<String, String> collectValues() {
        Map<String, String> values = new HashMap<>();
        for (Node child : fieldsContainer.getChildren()) {
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
        return values;
    }

    private Node createControlForParameter(PluginParameter param) {
        String type = param.getType() != null ? param.getType() : "text";

        switch (type) {
            case "select":
                ComboBox<String> comboBox = new ComboBox<>();
                if (param.getOptions() != null) comboBox.getItems().addAll(param.getOptions());
                if (!comboBox.getItems().isEmpty()) comboBox.getSelectionModel().selectFirst();
                comboBox.setPromptText("Please select " + param.getLabel());
                return comboBox;

            case "file":
                HBox fileRow = new HBox(5);
                TextField fileField = new TextField();
                fileField.setPromptText("Choose a file for " + param.getLabel());
                Button browseButton = new Button("浏览");
                browseButton.setOnAction(e -> {
                    FileChooser fc = new FileChooser();
                    File file = fc.showOpenDialog(owner);
                    if (file != null) fileField.setText(file.getAbsolutePath());
                });
                fileRow.getChildren().addAll(fileField, browseButton);
                return fileRow;

            case "number":
                TextField numberField = new TextField();
                numberField.setPromptText("Please enter a number for " + param.getLabel());
                return numberField;

            case "checkbox":
            case "flag":
                CheckBox cb = new CheckBox();
                cb.setText("On");
                return cb;

            case "text":
            default:
                TextField tf = new TextField();
                tf.setPromptText("Please enter " + param.getLabel());
                return tf;
        }
    }

    private String extractValueFromControl(Node control) {
        if (control instanceof TextField tf) return tf.getText();
        if (control instanceof ComboBox<?> cb) return cb.getValue() != null ? cb.getValue().toString() : "";
        if (control instanceof HBox fileRow) {
            for (Node c : fileRow.getChildren()) {
                if (c instanceof TextField tf) return tf.getText();
            }
            return "";
        }
        if (control instanceof CheckBox cb) return String.valueOf(cb.isSelected());
        return "";
    }

    private boolean isCheckboxChecked(String paramName) {
        for (Node child : fieldsContainer.getChildren()) {
            if (!(child instanceof HBox row)) continue;
            for (Node sub : row.getChildren()) {
                if (!(sub instanceof CheckBox cb)) continue;
                for (Node sibling : row.getChildren()) {
                    if (sibling instanceof Label label) {
                        String text = label.getText();
                        if (text.endsWith(" *")) text = text.substring(0, text.length() - 2);
                        if (text.equals(paramName)) return cb.isSelected();
                    }
                }
            }
        }
        return false;
    }
}