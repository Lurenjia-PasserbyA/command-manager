<#
    ffmpeg 测试插件的包装脚本。

    为什么需要它：宿主程序拼命令行的方式是 "executable + 按表单顺序的值"，
    它没办法在某个值前面插 flag。而 ffmpeg 要的是

        ffmpeg -i in.mp4 -vf scale=1280:-1 -crf 23 -preset medium -y out.mp4

    所以把 flag 的编排收进这个脚本，manifest 里只声明"值"。

    参数顺序与 manifest.json 的 parameters 顺序严格一一对应，改一个必须改另一个。
#>
param(
    [Parameter(Position = 0)][string]$InputPath,
    [Parameter(Position = 1)][string]$OutputPath,
    [Parameter(Position = 2)][string]$Resolution,
    [Parameter(Position = 3)][string]$Quality
)

$ErrorActionPreference = 'Stop'

function Find-Ffmpeg {
    # 1) PATH 上现成的
    $onPath = Get-Command ffmpeg -ErrorAction SilentlyContinue
    if ($onPath) { return $onPath.Source }

    # 2) 环境变量
    if ($env:FFMPEG_HOME) {
        $candidate = Join-Path $env:FFMPEG_HOME 'ffmpeg.exe'
        if (Test-Path $candidate) { return $candidate }
    }

    # 3) winget / scoop / choco 的常见落点
    $candidates = @(
        (Join-Path $env:LOCALAPPDATA 'Microsoft\WinGet\Links\ffmpeg.exe'),
        (Join-Path $env:USERPROFILE 'scoop\shims\ffmpeg.exe'),
        'C:\ProgramData\chocolatey\bin\ffmpeg.exe',
        'C:\ffmpeg\bin\ffmpeg.exe'
    )
    foreach ($c in $candidates) {
        if ($c -and (Test-Path $c)) { return $c }
    }
    return $null
}

# ---------- 参数校验 ----------
if ([string]::IsNullOrWhiteSpace($InputPath)) {
    Write-Output '[ffmpeg] 错误：输入文件为空。请在参数面板里选一个输入文件。'
    exit 1
}
if (-not (Test-Path -LiteralPath $InputPath)) {
    Write-Output "[ffmpeg] 错误：找不到输入文件 -> $InputPath"
    exit 1
}
if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    Write-Output '[ffmpeg] 错误：输出文件为空。'
    exit 1
}

$ffmpeg = Find-Ffmpeg
if (-not $ffmpeg) {
    Write-Output '[ffmpeg] 错误：系统里找不到 ffmpeg。'
    Write-Output '         装一个再试：winget install --id Gyan.FFmpeg'
    Write-Output '         或者设置 FFMPEG_HOME 环境变量指向 ffmpeg 所在目录。'
    exit 1
}

# ---------- 拼 ffmpeg 参数 ----------
# 用数组而不是字符串拼接：路径里有空格或特殊字符时不会被拆坏。
$ffArgs = [System.Collections.Generic.List[string]]::new()

# -y 让 ffmpeg 直接覆盖输出，否则它会停下来问 y/n，
# 而这里的 stdin 跟宿主 shell 的 stdin 是同一个，一问就卡死。
$ffArgs.Add('-y')
$ffArgs.Add('-i')
$ffArgs.Add($InputPath)

# Resolution 形如 1280:-1；留空或 original 表示不缩放
if (-not [string]::IsNullOrWhiteSpace($Resolution) -and $Resolution -ne 'original') {
    $ffArgs.Add('-vf')
    $ffArgs.Add("scale=$Resolution")
}

# Quality: crf + preset 两个维度一起定
$presets = @{
    'high'   = @('18', 'slow')
    'medium' = @('23', 'medium')
    'low'    = @('28', 'veryfast')
    'tiny'   = @('32', 'veryfast')
}
$key = if ($Quality) { $Quality.Trim().ToLowerInvariant() } else { 'medium' }
if (-not $presets.ContainsKey($key)) { $key = 'medium' }
$crf, $preset = $presets[$key]

$ffArgs.Add('-crf');    $ffArgs.Add($crf)
$ffArgs.Add('-preset'); $ffArgs.Add($preset)
$ffArgs.Add($OutputPath)

Write-Output ("[ffmpeg] {0}" -f $ffmpeg)
Write-Output ("[ffmpeg] 输入   : {0}" -f $InputPath)
Write-Output ("[ffmpeg] 输出   : {0}" -f $OutputPath)
Write-Output ("[ffmpeg] 分辨率 : {0}" -f $(if ($Resolution) { $Resolution } else { '(保持原样)' }))
Write-Output ("[ffmpeg] 画质   : {0}  (crf={1}, preset={2})" -f $key, $crf, $preset)
Write-Output '------------------------------------------------------------'

# 直接透传 ffmpeg 的输出，这样进度和报错都能进宿主的终端区
& $ffmpeg @ffArgs
$code = $LASTEXITCODE

Write-Output '------------------------------------------------------------'
if ($code -eq 0) {
    Write-Output "[ffmpeg] 完成 -> $OutputPath"
} else {
    Write-Output "[ffmpeg] ffmpeg 退出码 $code"
}
exit $code
