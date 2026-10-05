# Command Manager

一个 JavaFX 写的桌面「命令 / 插件管理器」：左边是菜单，中间是页面，底部挂一个常驻的交互式 shell。

- **主页** —— 快捷入口
- **终端** —— 选插件 → 填参数 → 执行，命令直接写进常驻 shell；也可以手敲任意命令
- **插件** —— 查看 / 移除已加载的插件，打开插件目录
- **设置** —— shell 路径、启动参数、工作目录、输出编码、终端字号、回滚行数

## 运行

需要 JDK 21。

```bash
./gradlew run
```

> 如果 `JAVA_HOME` 指向的目录里没有 `bin/java`（常见于把 JDK 外层壳目录当成 JDK），
> Gradle 会直接报 `JAVA_HOME is set to an invalid directory`。把它改指向真正含
> `bin/` 的那一层即可。

## 插件

插件放在运行目录下的 `plugins/<任意名字>/manifest.json`。程序启动时扫描一次这个目录。

`manifest.json` 的字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | string | 插件唯一标识，移除插件时按它匹配 |
| `name` | string | 显示名 |
| `version` | string | 版本号 |
| `description` | string | 描述 |
| `type` | string | 插件类型标签，当前不参与调度 |
| `executable` | string | 拼装命令行时的开头部分 |
| `isExecutable` | boolean | 是否可执行 |
| `parameters` | array | 参数定义，见下 |

`parameters` 里每一项：

| 字段 | 类型 | 说明 |
|---|---|---|
| `name` | string | 参数名。**取值时按它做键**，必须唯一 |
| `label` | string | 界面上显示的标签 |
| `type` | string | `text` / `number` / `select` / `file` / `checkbox` / `flag` |
| `required` | boolean | 必填。留空时点执行会被拦下并提示 |
| `options` | string[] | 仅 `select` 用，下拉选项 |
| `flagValue` | string | 仅 `flag` 用，勾选后追加到命令行的字面量 |

示例：

```json
{
  "id": "demo.greet",
  "version": "1.0.0",
  "name": "打招呼",
  "description": "演示各种参数类型",
  "type": "shell",
  "executable": "echo",
  "isExecutable": true,
  "parameters": [
    { "name": "message", "label": "内容", "type": "text", "required": true },
    { "name": "mode", "label": "模式", "type": "select", "required": true, "options": ["plain", "loud"] },
    { "name": "repeat", "label": "次数", "type": "number", "required": false },
    { "name": "upper", "label": "转大写", "type": "flag", "required": false, "flagValue": "--upper" }
  ]
}
```

命令行拼装顺序：先 `executable`，再所有勾选上的 `flag` 参数（追加各自的 `flagValue`），
最后按表单顺序追加非 flag 参数的值。值里含空格或引号时会被双引号包起来。

## 配置

设置页保存的配置写到运行目录下的 `.command-manager/config.json`。文件损坏或缺失时
程序用默认值启动，并把问题打到控制台，不会因此起不来。

## 开发

```bash
./gradlew test    # 单元测试
./gradlew build   # 编译 + 测试
```

代码结构：

```
src/main/java/org/passerbya/
├── Main.java                入口
├── core/                    CommandManager（shell 进程）、PluginManager、AppSettings
├── tools/                   manifest 数据模型与加载器
├── debug/                   DebugLogger
└── ui/                      MainGUI、组件与各个页面
```

## License

见 [LICENSE](./LICENSE)。
