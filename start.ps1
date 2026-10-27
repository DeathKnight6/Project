[CmdletBinding()]
param(
    [switch]$Check,
    [switch]$OpenBrowser,
    [ValidateRange(10, 600)]
    [int]$StartupTimeoutSeconds = 120
)

$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$startedProcesses = New-Object System.Collections.Generic.List[System.Diagnostics.Process]

function Find-Executable {
    param([string]$Name, [string]$Fallback)
    $command = Get-Command $Name -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($command) { return $command.Source }
    if ($Fallback -and (Test-Path -LiteralPath $Fallback)) { return $Fallback }
    throw "找不到 $Name，请先安装对应工具或把其 bin 目录加入 PATH。"
}

function Get-JavaMajor {
    param([string]$Executable)
    $info = New-Object System.Diagnostics.ProcessStartInfo
    $info.FileName = $Executable
    $info.Arguments = '-version'
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $info
    try {
        [void]$process.Start()
        $versionText = $process.StandardError.ReadToEnd() + $process.StandardOutput.ReadToEnd()
        $process.WaitForExit()
        if ($process.ExitCode -eq 0 -and $versionText -match 'version\s+"(\d+)(?:\.(\d+))?') {
            if ($Matches[1] -eq '1') { return [int]$Matches[2] }
            return [int]$Matches[1]
        }
        return 0
    } finally { $process.Dispose() }
}

function Test-LocalPort {
    param([int]$Port)
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $connect = $client.ConnectAsync('127.0.0.1', $Port)
        return $connect.Wait(500) -and $client.Connected
    } catch { return $false } finally { $client.Dispose() }
}

function Test-BackendReady {
    try {
        $response = Invoke-RestMethod 'http://127.0.0.1:8080/api/stock/list' -TimeoutSec 2
        return $response.code -eq 0 -and @($response.data).Count -gt 0
    } catch { return $false }
}

function Test-FrontendReady {
    try {
        $response = Invoke-WebRequest 'http://127.0.0.1:5173/' -UseBasicParsing -TimeoutSec 2
        return $response.StatusCode -eq 200 -and $response.Content.Contains('/src/main.ts')
    } catch { return $false }
}

function Start-ServiceProcess {
    param([string]$Name, [string]$Executable, [string]$Arguments, [string]$Directory)
    # cmd.exe 负责运行 .cmd；工作目录单独传递，支持项目路径中的中文与空格。
    $commandLine = '""{0}" {1}"' -f $Executable, $Arguments
    $process = Start-Process -FilePath $env:ComSpec -ArgumentList @('/d', '/s', '/c', $commandLine) `
        -WorkingDirectory $Directory -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $logDirectory "$Name.log") `
        -RedirectStandardError (Join-Path $logDirectory "$Name.error.log")
    $startedProcesses.Add($process)
    return $process
}

function Wait-ServiceReady {
    param([System.Diagnostics.Process]$Process, [scriptblock]$Probe, [string]$Name)
    $deadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $Process.Refresh()
        if ($Process.HasExited) { throw "$Name 已退出，请检查 .run 目录中的日志。" }
        if (& $Probe) { return }
        Start-Sleep -Seconds 1
    }
    throw "$Name 在 $StartupTimeoutSeconds 秒内未就绪，请检查 .run 目录中的日志。首次下载依赖可使用 -StartupTimeoutSeconds 600。"
}

try {
    $javaCandidates = @()
    if ($env:JAVA_HOME) { $javaCandidates += Join-Path $env:JAVA_HOME 'bin\java.exe' }
    $javaCandidates += 'D:\jdk-25\jdk-25.0.2\bin\java.exe'
    $javaOnPath = Get-Command java.exe -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($javaOnPath) { $javaCandidates += $javaOnPath.Source }
    $javaExecutable = $null
    foreach ($candidate in ($javaCandidates | Select-Object -Unique)) {
        if ((Test-Path -LiteralPath $candidate) -and (Get-JavaMajor $candidate) -ge 25 -and
            (Test-Path -LiteralPath (Join-Path (Split-Path $candidate) 'javac.exe'))) {
            $javaExecutable = $candidate
            break
        }
    }
    if (-not $javaExecutable) { throw '找不到 JDK 25+，请安装后设置 JAVA_HOME。' }
    $env:JAVA_HOME = Split-Path (Split-Path $javaExecutable)
    $env:Path = (Join-Path $env:JAVA_HOME 'bin') + ';' + $env:Path
    $mavenExecutable = Find-Executable 'mvn.cmd' 'D:\apache-maven-3.9.9\bin\mvn.cmd'
    $nodeExecutable = Find-Executable 'node.exe' ''
    $npmExecutable = Find-Executable 'npm.cmd' ''
    Write-Host "JDK   : $env:JAVA_HOME"
    Write-Host "Maven : $mavenExecutable"
    Write-Host "Node  : $nodeExecutable"
    Write-Host "npm   : $npmExecutable"

    $backendInUse = Test-LocalPort 8080
    $frontendInUse = Test-LocalPort 5173
    if ($backendInUse -and -not (Test-BackendReady)) { throw '8080 端口已被其他服务占用或后端尚未就绪，请稍后重试。' }
    if ($frontendInUse -and -not (Test-FrontendReady)) { throw '5173 端口已被其他服务占用，请释放该端口后重试。' }
    if ($Check) {
        Write-Host '环境检查通过。' -ForegroundColor Green
        Write-Host "后端已运行：$backendInUse；前端已运行：$frontendInUse"
        return
    }

    $logDirectory = Join-Path $projectRoot '.run'
    [void](New-Item -ItemType Directory -Path $logDirectory -Force)
    if (-not $frontendInUse -and -not (Test-Path -LiteralPath (Join-Path $projectRoot 'frontend\node_modules'))) {
        Write-Host '首次运行，正在安装前端依赖……'
        Push-Location (Join-Path $projectRoot 'frontend')
        try {
            & $npmExecutable ci
            if ($LASTEXITCODE -ne 0) { throw '前端依赖安装失败。' }
        } finally { Pop-Location }
    }

    if (-not $backendInUse) {
        Write-Host '[1/2] 正在启动后端……'
        $backendProcess = Start-ServiceProcess 'backend' $mavenExecutable '-q spring-boot:run' (Join-Path $projectRoot 'backend')
        Wait-ServiceReady $backendProcess { Test-BackendReady } '后端'
    } else { Write-Host '[1/2] 后端已经就绪，复用现有服务。' }
    if (-not $frontendInUse) {
        Write-Host '[2/2] 正在启动前端……'
        $frontendProcess = Start-ServiceProcess 'frontend' $npmExecutable 'run dev -- --host 127.0.0.1' (Join-Path $projectRoot 'frontend')
        Wait-ServiceReady $frontendProcess { Test-FrontendReady } '前端'
    } else { Write-Host '[2/2] 前端已经就绪，复用现有服务。' }

    Write-Host "`n启动成功：http://127.0.0.1:5173/" -ForegroundColor Green
    Write-Host '后端接口：http://127.0.0.1:8080/api'
    if ($OpenBrowser) {
        try { Start-Process 'http://127.0.0.1:5173/' }
        catch { Write-Host '未能自动打开浏览器，请手动访问 http://127.0.0.1:5173/' -ForegroundColor Yellow }
    }
    if ($startedProcesses.Count -eq 0) { return }
    Write-Host '日志位于 .run 目录。按 Ctrl+C 停止本次启动的服务。'
    while ($true) {
        foreach ($process in $startedProcesses) {
            $process.Refresh()
            if ($process.HasExited) { throw "服务进程已退出，请检查 $logDirectory 中的日志。" }
        }
        Start-Sleep -Seconds 1
    }
} catch {
    Write-Host "启动失败：$($_.Exception.Message)" -ForegroundColor Red
    exit 1
} finally {
    # 仅结束本次脚本启动的进程树；不会停止检查阶段发现的现有服务。
    foreach ($process in $startedProcesses) {
        try {
            $process.Refresh()
            if (-not $process.HasExited) { & taskkill.exe /PID $process.Id /T /F 2>$null | Out-Null }
        } catch { } finally { $process.Dispose() }
    }
}
