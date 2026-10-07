<#
    Ollama 本地大模型插件 —— 包装脚本。

    为什么需要它：宿主拼命令行的方式是 "executable + 按表单顺序的值"，没法在某个值
    前面插 flag，所以把「调 API、组 JSON、解析结果」都收进这个脚本。

    参数顺序与 manifest.json 的 parameters 严格一一对应，改一个必须改另一个。
    注意：model 放在最后一位且可选 —— 宿主对空的中间参数会直接跳过导致错位，
    只有末尾的空参数省略才是安全的。
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)][string]$Prompt,
    [Parameter(Position = 1)][string]$Style = '简洁',
    [Parameter(Position = 2)][string]$Model = 'qwen3:0.6b'
)

$ErrorActionPreference = 'Stop'

# 关键：强制以 UTF-8 输出。宿主 shell(PowerShell 7)按 UTF-8 读管道，而 Windows
# PowerShell 5.1 默认按系统代码页(GBK)写控制台；不显式设成 UTF-8，模型返回的中文
# 会在「本脚本 -> 父 shell -> App」这一跳变成乱码。
try {
    [Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
    $OutputEncoding = [Console]::OutputEncoding
} catch { }

$BaseUrl = if ($env:OLLAMA_HOST) { $env:OLLAMA_HOST } else { 'http://127.0.0.1:11434' }

function Fail([string]$msg) { Write-Output "[ollama] $msg"; exit 1 }

if ([string]::IsNullOrWhiteSpace($Prompt)) { Fail '提示词为空，请在参数面板输入要问的内容。' }

# 服务在不在线
try { Invoke-RestMethod -Uri "$BaseUrl/api/version" -TimeoutSec 5 | Out-Null }
catch { Fail "连不上 Ollama（$BaseUrl）。请先运行 ollama serve，或确认端口对不对。" }

# 模型存不存在：不存在就把已装的列出来，别静默失败
try { $tags = Invoke-RestMethod -Uri "$BaseUrl/api/tags" -TimeoutSec 10 } catch { $tags = $null }
$have = @()
if ($tags -and $tags.models) { $have = @($tags.models | ForEach-Object { $_.name }) }
if ($have.Count -gt 0 -and ($have -notcontains $Model)) {
    $match = $have | Where-Object { $_ -like "$Model*" } | Select-Object -First 1
    if ($match) { $Model = $match }
    else {
        Write-Output "[ollama] 本机没有模型 '$Model'。已安装的有："
        $have | ForEach-Object { Write-Output "    - $_" }
        Fail '请先 ollama pull <模型>，或把「模型」参数改成上面任意一个。'
    }
}

switch ($Style) {
    '详细' { $temp = 0.7; $maxTok = 600 }
    '代码' { $temp = 0.2; $maxTok = 900 }
    default { $temp = 0.3; $maxTok = 250 }   # 简洁
}

$body = @{
    model   = $Model
    prompt  = $Prompt
    stream  = $false
    options = @{ temperature = $temp; num_predict = $maxTok; num_ctx = 2048 }
} | ConvertTo-Json -Depth 6 -Compress

Write-Output "[ollama] 模型=$Model  风格=$Style (temperature=$temp)"
Write-Output ('-' * 56)

try {
    $resp = Invoke-RestMethod -Uri "$BaseUrl/api/generate" -Method Post -Body $body `
            -ContentType 'application/json; charset=utf-8' -TimeoutSec 600
} catch { Fail "请求失败：$($_.Exception.Message)" }

Write-Output $resp.response
Write-Output ('-' * 56)

# 顺手报一下实测吞吐，正好给研究报告当数据
if ($resp.eval_count -and $resp.eval_duration) {
    $secs = [double]$resp.eval_duration / 1e9
    $tps  = [double]$resp.eval_count / [math]::Max($secs, 0.001)
    Write-Output ('[ollama] 生成 {0} tokens，用时 {1:N1}s，约 {2:N1} tokens/秒' -f $resp.eval_count, $secs, $tps)
}
exit 0