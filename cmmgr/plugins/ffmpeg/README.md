# FFmpeg 转码（测试插件）

一个演示用的插件：把视频转码 / 缩放。它同时也是一份"这类插件该怎么写"的样板。

## 为什么需要 `run.ps1` 这层包装

宿主程序拼命令行的方式是：

```
executable + 空格 + 按表单顺序拼接的值
```

它**没有任何办法在某个值前面插入 flag**。所以如果 `executable` 直接写 `ffmpeg`，
你会得到：

```
ffmpeg input.mp4 output.mp4 1280:-1 medium
```

ffmpeg 根本不认这种写法，它要的是：

```
ffmpeg -i input.mp4 -vf scale=1280:-1 -crf 23 -preset medium -y output.mp4
```

于是把 flag 的编排收进 `run.ps1`，manifest 里只声明"值"。

## 参数顺序是一份契约

`manifest.json` 里 `parameters` 的顺序，与 `run.ps1` 里 `param()` 的位置参数
**严格一一对应**：

| # | manifest name | run.ps1 参数 | 说明 |
|---|---|---|---|
| 1 | `input` | `$InputPath` | 输入文件，必填 |
| 2 | `output` | `$OutputPath` | 输出文件，必填 |
| 3 | `resolution` | `$Resolution` | `1280:-1` 这种 `宽:高`，`original` 表示不缩放 |
| 4 | `quality` | `$Quality` | `high` / `medium` / `low` / `tiny` |

**改一个必须改另一个**，否则参数会错位（而且不会报错，只是行为不对）。

## 依赖

需要系统里有 `ffmpeg`。脚本按这个顺序找：

1. `PATH` 上的 `ffmpeg`
2. `%FFMPEG_HOME%\ffmpeg.exe`
3. winget / scoop / choco 的常见落点（`%LOCALAPPDATA%\Microsoft\WinGet\Links\`、
   `%USERPROFILE%\scoop\shims\`、`C:\ProgramData\chocolatey\bin\`、`C:\ffmpeg\bin\`）

找不到时会打印明确的提示并以退出码 1 结束，不会静默什么都不做。

安装：

```powershell
winget install --id Gyan.FFmpeg
```

装完**重开一个终端**让它进 PATH，再重启本程序。

## ⚠️ 两个坑

**1. `-ExecutionPolicy Bypass` 不能省**

`executable` 自己写全了参数：

```
powershell.exe -ExecutionPolicy Bypass -NoProfile -File "cmmgr/plugins/ffmpeg/run.ps1"
```

其中 `-ExecutionPolicy Bypass` 是必需的 —— 这台机器的 PowerShell 执行策略
禁止运行脚本，少了它会被直接拦下。

注意这些参数是写在**插件的 `executable` 里**，跟设置页里全局的「启动参数」
（默认 `-NoLogo -NoExit`）是两回事。设置页那个改乱了不影响本插件。

**2. `run.ps1` 用的是相对路径，所以依赖工作目录**

路径写的是 `cmmgr/plugins/ffmpeg/run.ps1`，相对的是**进程的工作目录**
（`gradlew run` 时就是项目根目录），和宿主找插件目录的基准一致。

所以：**如果开了设置页里的「启动后自动切到插件目录」，这个路径会失效**
（那时 shell 的当前目录已经是 `cmmgr/plugins/`，应该改成 `ffmpeg/run.ps1`）。
该选项默认关闭。

**3. `run.ps1` 必须带 UTF-8 BOM**

Windows PowerShell 5.1 读取**没有 BOM** 的 `.ps1` 时会按 ANSI（中文系统上是 GBK）
解码，脚本里的中文会被读乱，进而破坏语法解析、整个脚本报
`Missing closing '}' in statement block`。

**这和 Java 源文件的规则正好相反** —— Java 源文件带 BOM 会编译失败（`非法字符: '\ufeff'`）。
所以本仓库里：

| 文件类型 | BOM |
|---|---|
| `src/**/*.java` | ❌ 不能有 |
| `src/**/style.css` | ❌ 不需要 |
| `cmmgr/plugins/**/*.ps1` | ✅ 必须有 |

如果你用编辑器重存了 `run.ps1`，确认它没有把 BOM 去掉。

## 输出文件

`output` 是普通文本框，直接填完整路径，例如：

```
D:\videos\out.mp4
```

脚本会加 `-y` 让 ffmpeg 直接覆盖同名文件。这一步不能省：
ffmpeg 默认会停下问 y/n，而它的 stdin 跟宿主 shell 的 stdin 是同一个，
一问就会**把终端卡死**。
