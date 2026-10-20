param(
    [string]$MysqlHome = 'C:\Program Files\MySQL\MySQL Server 8.0',
    [string]$TomcatHome = 'C:\Tools\apache-tomcat-10.1.60',
    [string]$JavaHome = 'C:\Program Files\Java\jdk-25',
    [switch]$Offline,
    [switch]$SkipBuild
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$runtimeRoot = Join-Path $projectRoot '.local-runtime'
$dataPath = Join-Path $runtimeRoot 'mysql-data'
$tomcatBase = Join-Path $runtimeRoot 'tomcat'
$mysql = Join-Path $MysqlHome 'bin\mysql.exe'
$mysqld = Join-Path $MysqlHome 'bin\mysqld.exe'
$java = Join-Path $JavaHome 'bin\java.exe'
foreach ($required in @($mysql,$mysqld,$java,(Join-Path $TomcatHome 'bin\bootstrap.jar'))) {
    if (!(Test-Path -LiteralPath $required)) { throw "Missing prerequisite: $required. Pass the installed paths to this script." }
}
New-Item -ItemType Directory -Path $runtimeRoot -Force | Out-Null
function Test-LocalDatabase {
    try { $databasePath = & $mysql --no-defaults --host=127.0.0.1 --port=33307 --user=root --batch --skip-column-names --execute='SELECT @@datadir' 2>$null }
    catch { return $false }
    if ($LASTEXITCODE -ne 0) { return $false }
    $resolvedData = [IO.Path]::GetFullPath(($databasePath | Out-String).Trim().TrimEnd('/','\'))
    if ($resolvedData -ne [IO.Path]::GetFullPath($dataPath)) { throw 'Port 33307 belongs to another database. Nothing will be changed.' }
    return $true
}
if (!(Test-LocalDatabase)) {
    if (!(Test-Path -LiteralPath $dataPath)) {
        & $mysqld --no-defaults --initialize-insecure "--basedir=$MysqlHome" "--datadir=$dataPath"
        if ($LASTEXITCODE -ne 0) { throw 'Disposable MySQL initialization failed' }
    }
    $mysqlArgs = @('--no-defaults',"--basedir=`"$MysqlHome`"","--datadir=`"$dataPath`"",'--bind-address=127.0.0.1','--port=33307','--mysqlx=OFF',"--pid-file=`"$runtimeRoot\mysql-server.pid`"","--log-error=`"$runtimeRoot\mysql-error.log`"")
    Start-Process -FilePath $mysqld -ArgumentList $mysqlArgs -WindowStyle Hidden | Out-Null
    $ready = $false
    for ($attempt=0; $attempt -lt 30; $attempt++) { Start-Sleep -Milliseconds 500; if (Test-LocalDatabase) { $ready=$true; break } }
    if (!$ready) { throw 'MySQL did not become ready; inspect .local-runtime/mysql-error.log' }
}
$exists = & $mysql --no-defaults --host=127.0.0.1 --port=33307 --user=root --batch --skip-column-names --execute="SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='exam_seating'"
if ($exists -eq '0') { & (Join-Path $PSScriptRoot 'reset-local-demo.ps1') -ConfirmDisposable -MysqlHome $MysqlHome }
$migrationTable = & $mysql --no-defaults --host=127.0.0.1 --port=33307 --user=root --batch --skip-column-names --execute="SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='exam_seating' AND table_name='schema_migrations'"
$upgradeRequired = $true
if ($migrationTable -eq '1') {
    $applied = & $mysql --no-defaults --host=127.0.0.1 --port=33307 --user=root --batch --skip-column-names --execute="SELECT COUNT(*) FROM exam_seating.schema_migrations WHERE version='002_complete_project'"
    $upgradeRequired = $applied -ne '1'
}
if ($upgradeRequired) {
    $dump = Join-Path $MysqlHome 'bin\mysqldump.exe'
    $backup = Join-Path $runtimeRoot ("before-project-upgrade-" + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.sql')
    & $dump --no-defaults --host=127.0.0.1 --port=33307 --user=root --single-transaction --no-tablespaces "--result-file=$backup" --databases exam_seating
    if ($LASTEXITCODE -ne 0) { throw 'Database backup failed. No upgrade was applied.' }
    $OutputEncoding = [Text.UTF8Encoding]::new($false)
    Get-Content -LiteralPath (Join-Path $projectRoot 'database\migrations\002_complete_project.sql') -Raw | & $mysql --no-defaults --host=127.0.0.1 --port=33307 --user=root --default-character-set=utf8mb4
    if ($LASTEXITCODE -ne 0) { throw 'Project database upgrade failed; existing records were not reset. Inspect the MySQL error before restarting.' }
    Write-Output 'Remaining project database modules installed; existing records retained.'
}
if (!$SkipBuild) {
Push-Location $projectRoot
try {
    $buildArgs = @("-Dmaven.repo.local=$runtimeRoot\m2",'-Dmaven.test.skip=true','package')
    if ($Offline) { $buildArgs=@('-o',"-Dmaven.repo.local=$runtimeRoot\m2",'-Dmaven.test.skip=true','package') }
    & mvn @buildArgs
    if ($LASTEXITCODE -ne 0) { throw 'Maven build failed' }
} finally { Pop-Location }
} elseif (!(Test-Path -LiteralPath (Join-Path $projectRoot 'target\exam-seating.war'))) {
    throw 'Build the project before using -SkipBuild.'
}
foreach ($directory in @('conf','logs','temp','webapps','work')) { New-Item -ItemType Directory -Path (Join-Path $tomcatBase $directory) -Force | Out-Null }
if (!(Test-Path -LiteralPath (Join-Path $tomcatBase 'conf\server.xml'))) {
    Copy-Item -Path (Join-Path $TomcatHome 'conf\*') -Destination (Join-Path $tomcatBase 'conf')
    $serverFile=Join-Path $tomcatBase 'conf\server.xml'
    $xml=Get-Content -LiteralPath $serverFile -Raw
    $xml=$xml.Replace('port="8005"','port="33085"').Replace('port="8080"','port="33080" address="127.0.0.1"')
    Set-Content -LiteralPath $serverFile -Value $xml -Encoding UTF8
}
Copy-Item -LiteralPath (Join-Path $projectRoot 'target\exam-seating.war') -Destination (Join-Path $tomcatBase 'webapps\exam-seating.war') -Force
$listeners=Get-NetTCPConnection -LocalPort 33080 -State Listen -ErrorAction SilentlyContinue
if ($listeners) {
    $running=Get-CimInstance Win32_Process -Filter "ProcessId=$($listeners[0].OwningProcess)"
    if (!$running.CommandLine.Contains($tomcatBase)) { throw 'Port 33080 belongs to another process' }
    Write-Output 'WAR copied; the running demo Tomcat will redeploy it.'
} else {
    $env:DB_URL='jdbc:mysql://127.0.0.1:33307/exam_seating?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
    $env:DB_USER='root'; $env:DB_PASSWORD=''
    $classPath="$TomcatHome\bin\bootstrap.jar;$TomcatHome\bin\tomcat-juli.jar"
    $javaArgs=@("-Dcatalina.home=`"$TomcatHome`"","-Dcatalina.base=`"$tomcatBase`"","-Djava.io.tmpdir=`"$tomcatBase\temp`"","-Djava.util.logging.config.file=`"$tomcatBase\conf\logging.properties`"",'-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager','-cp',"`"$classPath`"",'org.apache.catalina.startup.Bootstrap','start')
    $process=Start-Process -FilePath $java -ArgumentList $javaArgs -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $tomcatBase 'console-out.log') -RedirectStandardError (Join-Path $tomcatBase 'console-error.log')
    Set-Content -LiteralPath (Join-Path $runtimeRoot 'tomcat.pid') -Value $process.Id
}
Write-Output 'Demo: http://127.0.0.1:33080/exam-seating/ (allow a few seconds to deploy)'
