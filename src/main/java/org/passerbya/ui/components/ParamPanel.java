package org.passerbya.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.passerbya.tools.PluginManifest;
import org.passerbya.tools.PluginParameter;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件参数面板：根据选中的插件动态渲染表单，并负责收集/拼装命令行。
 *
 * <p>表单的每一行固定是 {@code HBox(Label, Control)}，读取时按位置取控件，
 * 按 Label 文本（去掉必填星号）回查参数名。这个约定是 UI 与取值逻辑之间的契约，
 * 改动 {@link #refresh} 里的行结构时必须同步改 {@link #collectValues}。
 */
public class ParamPanel extends VBox {

    /** 必填字段在选择器上显示的星号后缀。 */
    private static final String REQUIRED_SUFFIX = " *";

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

    public PluginManifest getSelectedPlugin() {
        return selectedPlugin;
    }

    /** 按插件重建表单；传 null 则显示"请先选插件"的占位提示。 */
    public void refresh(PluginManifest plugin) {
        this.selectedPlugin = plugin;
        fieldsContainer.getChildren().clear();

        if (plugin == null) {
            fieldsContainer.getChildren().add(hint("Please select a plugin from the list on the left"));
            return;
        }

        List<PluginParameter> params = plugin.getParameters();
        if (params == null || params.isEmpty()) {
            fieldsContainer.getChildren().add(
                    hint("This plugin has no required parameters. You can execute it directly."));
            return;
        }

        for (PluginParameter param : params) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);

            Label label = new Label(displayLabel(param));
            label.getStyleClass().add("param-label");

            Node control = createControlForParameter(param);
            row.getChildren().addAll(label, control);
            fieldsContainer.getChildren().add(row);
        }
    }

    /**
     * 校验必填项。返回人类可读的问题列表；为空表示可以执行。
     *
     * <p>之前这里直接静默跳过空值，用户点了执行却什么参数都没传，很难排查 ——
     * 所以把校验结果显式交回给调用方去展示。
     */
    public List<String> validate() {
        List<String> problems = new ArrayList<>();
        if (selectedPlugin == null) {
            problems.add("请先选择一个插件");
            return problems;
        }

        Map<String, String> values = collectValues();
        List<PluginParameter> params = selectedPlugin.getParameters();
        if (params == null) return problems;

        for (PluginParameter param : params) {
            String type = param.getType() != null ? param.getType() : "text";
            if ("flag".equals(type)) continue;
            if (!param.isRequired()) continue;

            String value = values.get(param.getName());
            if (value == null || value.isBlank()) {
                problems.add("缺少必填参数：" + displayLabel(param));
            }
        }
        return problems;
    }

    /**
     * 根据当前表单拼装命令行。未选中插件时返回 null。
     *
     * <p>不做必填校验 —— 先调 {@link #validate()} 再决定要不要执行。
     */
    public String buildCommand() {
        if (selectedPlugin == null) return null;

        StringBuilder cmd = new StringBuilder(selectedPlugin.getExecutable());
        Map<String, String> values = collectValues();
        List<PluginParameter> params = selectedPlugin.getParameters();
        if (params == null || params.isEmpty()) return cmd.toString();

        // flag 类参数：勾选了才追加它自己的 flagValue
        for (PluginParameter param : params) {
            if (!"flag".equals(param.getType())) continue;
            if (isCheckboxChecked(param.getName()) && param.getFlagValue() != null) {
                cmd.append(" ").append(param.getFlagValue());
            }
        }

        // 其余参数按表单顺序追加值
        for (PluginParameter param : params) {
            if ("flag".equals(param.getType())) continue;
            String value = values.get(param.getName());
            if (value != null && !value.isEmpty()) {
                cmd.append(" ").append(quoteIfNeeded(value));
            }
        }

        return cmd.toString();
    }

    // ---------- 取值 ----------

    /**
     * 按行读取表单，返回 参数名 -> 值。空值不进 map。
     *
     * <p>注意：这里按 {@link PluginParameter#getName()} 而不是 Label 文本做 key。
     * 之前用 Label 当 key，两个参数只要 Label 撞名就会互相覆盖。
     */
    private Map<String, String> collectValues() {
        Map<String, String> values = new LinkedHashMap<>();
        if (selectedPlugin == null) return values;

        List<PluginParameter> params = selectedPlugin.getParameters();
        if (params == null) return values;

        List<Node> rows = fieldsContainer.getChildren();
        for (int i = 0; i < params.size() && i < rows.size(); i++) {
            if (!(rows.get(i) instanceof HBox row)) continue;
            if (row.getChildren().size() < 2) continue;

            String value = extractValueFromControl(row.getChildren().get(1));
            if (value != null && !value.isEmpty()) {
                values.put(params.get(i).getName(), value);
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
        if (selectedPlugin == null) return false;
        List<PluginParameter> params = selectedPlugin.getParameters();
        if (params == null) return false;

        int index = -1;
        for (int i = 0; i < params.size(); i++) {
            if (paramName != null && paramName.equals(params.get(i).getName())) {
                index = i;
                break;
            }
        }
        if (index < 0 || index >= fieldsContainer.getChildren().size()) return false;

        Node row = fieldsContainer.getChildren().get(index);
        if (!(row instanceof HBox box)) return false;
        for (Node child : box.getChildren()) {
            if (child instanceof CheckBox cb) return cb.isSelected();
        }
        return false;
    }

    // ---------- 小工具 ----------

    /** 值里含空格或引号时用双引号包起来，并转义内部引号。 */
    private static String quoteIfNeeded(String value) {
        if (value == null) return "";
        if (!value.contains(" ") && !value.contains("\"")) return value;
        return "\"" + value.replace("\"", "\\\"") + "\"";
    }

    private static String displayLabel(PluginParameter param) {
        String label = param.getLabel() != null ? param.getLabel() : param.getName();
        if (label == null) label = "";
        return param.isRequired() ? label + REQUIRED_SUFFIX : label;
    }

    private static Label hint(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("param-empty-label");
        return label;
    }
}
