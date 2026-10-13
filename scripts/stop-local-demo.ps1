$ErrorActionPreference='Stop'
$runtimeRoot=Join-Path (Split-Path -Parent $PSScriptRoot) '.local-runtime'
foreach ($port in @(33080,33307)) {
    $activePorts=[Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners().Port
    if ($port -notin $activePorts) { continue }
    $listeners=Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction Stop
    $process=Get-CimInstance Win32_Process -Filter "ProcessId=$($listeners[0].OwningProcess)"
    $expected=if($port -eq 33080){Join-Path $runtimeRoot 'tomcat'}else{Join-Path $runtimeRoot 'mysql-data'}
    if (!$process.CommandLine.Contains($expected)) { throw "Refusing to stop an unrelated process on $port" }
    if ($port -eq 33307) {
        $mysqlAdmin=Join-Path (Split-Path -Parent $process.ExecutablePath) 'mysqladmin.exe'
        & $mysqlAdmin --no-defaults --host=127.0.0.1 --port=33307 --user=root shutdown
        if ($LASTEXITCODE -ne 0) { throw 'MySQL graceful shutdown failed' }
    } else {
        $socket=[Net.Sockets.TcpClient]::new('127.0.0.1',33085)
        try { $bytes=[Text.Encoding]::ASCII.GetBytes('SHUTDOWN');$socket.GetStream().Write($bytes,0,$bytes.Length) } finally { $socket.Dispose() }
    }
    for ($attempt=0; $attempt -lt 40; $attempt++) {
        $activePorts=[Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners().Port
        if ($port -notin $activePorts) { break }
        Start-Sleep -Milliseconds 250
    }
    if ($port -in $activePorts) { throw "Service on $port did not stop; inspect its log" }
}
Write-Output 'Isolated demo services stopped; data remains in .local-runtime.'
