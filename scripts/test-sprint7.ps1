param(
    [switch]$ConfirmDisposable,
    [string]$MysqlHome='C:\Program Files\MySQL\MySQL Server 8.0',
    [string]$TomcatHome='C:\Tools\apache-tomcat-10.1.60',
    [string]$JavaHome='C:\Program Files\Java\jdk-25'
)
$ErrorActionPreference='Stop'
if(!$ConfirmDisposable){throw 'Specify -ConfirmDisposable to create and remove the separate Sprint 7 test database.'}
$projectRoot=Split-Path -Parent $PSScriptRoot
$runtimeRoot=Join-Path $projectRoot '.local-runtime'
$testBase=Join-Path $runtimeRoot 's7-check\tomcat'
$mysql=Join-Path $MysqlHome 'bin\mysql.exe'
$mysqlArgs=@('--no-defaults','--host=127.0.0.1','--port=33307','--user=root','--batch','--skip-column-names')
$war=Join-Path $projectRoot 'target\exam-seating.war'
if(!(Test-Path -LiteralPath $war)){throw 'Build the project first: mvn package'}
$listening=[Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners().Port
if(33090 -in $listening -or 33095 -in $listening){throw 'A Sprint 7 test port is already in use'}
$actual=& $mysql @mysqlArgs --execute='SELECT @@datadir'
if($LASTEXITCODE -ne 0){throw 'Start the isolated local demo database on port 33307 first'}
if([IO.Path]::GetFullPath(($actual|Out-String).Trim().TrimEnd('/','\')) -ne [IO.Path]::GetFullPath((Join-Path $runtimeRoot 'mysql-data'))){throw 'Refusing to use a database outside this workspace'}
$exists=& $mysql @mysqlArgs --execute="SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='exam_seating_s7_check'"
if($exists -ne '0'){throw 'The test database already exists; refusing to replace it'}
$created=$false
$serverStarted=$false
$savedDatabaseEnvironment=@{URL=$env:DB_URL;User=$env:DB_USER;Password=$env:DB_PASSWORD}
Push-Location $projectRoot
try {
    & $mysql @mysqlArgs --execute='CREATE DATABASE exam_seating_s7_check'
    if($LASTEXITCODE -ne 0){throw 'Test database creation failed'}
    $created=$true
    $sql=''
    foreach($file in @('database\schema.sql','database\seed.sql','database\demo-data.sql')){
        $sql+=(Get-Content -LiteralPath $file -Raw).Replace('exam_seating','exam_seating_s7_check')+[Environment]::NewLine
    }
    $OutputEncoding=[Text.UTF8Encoding]::new($false)
    $sql | & $mysql @mysqlArgs
    if($LASTEXITCODE -ne 0){throw 'Test fixtures could not be imported'}
    foreach($directory in @('conf','logs','temp','work','webapps')){New-Item -ItemType Directory -Force (Join-Path $testBase $directory)|Out-Null}
    Copy-Item -Path (Join-Path $TomcatHome 'conf\*') -Destination (Join-Path $testBase 'conf') -Force
    $config=Join-Path $testBase 'conf\server.xml'
    $xml=(Get-Content -LiteralPath $config -Raw).Replace('port="8005"','port="33095"').Replace('port="8080"','port="33090" address="127.0.0.1"')
    [IO.File]::WriteAllText($config,$xml,[Text.UTF8Encoding]::new($false))
    Copy-Item -LiteralPath $war -Destination (Join-Path $testBase 'webapps\exam-seating.war') -Force
    $env:DB_URL='jdbc:mysql://127.0.0.1:33307/exam_seating_s7_check?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
    $env:DB_USER='root';$env:DB_PASSWORD=''
    $javaArgs=@("-Dcatalina.home=`"$TomcatHome`"","-Dcatalina.base=`"$testBase`"","-Djava.io.tmpdir=`"$testBase\temp`"","-Djava.util.logging.config.file=`"$testBase\conf\logging.properties`"",'-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager','-cp',"`"$TomcatHome\bin\bootstrap.jar;$TomcatHome\bin\tomcat-juli.jar`"",'org.apache.catalina.startup.Bootstrap','start')
    $server=Start-Process -FilePath (Join-Path $JavaHome 'bin\java.exe') -ArgumentList $javaArgs -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $testBase 'console-out.log') -RedirectStandardError (Join-Path $testBase 'console-error.log')
    $serverStarted=$true
    $ready=$false
    for($attempt=0;$attempt -lt 40;$attempt++){
        try{Invoke-WebRequest 'http://127.0.0.1:33090/exam-seating/login.html' -UseBasicParsing -TimeoutSec 2|Out-Null;$ready=$true;break}catch{Start-Sleep -Milliseconds 500}
    }
    if(!$ready){throw 'Test server did not become ready; inspect .local-runtime/s7-check/tomcat logs'}
    & python scripts/test-sprint7.py --confirm-disposable
    if($LASTEXITCODE -ne 0){throw 'Sprint 7 API checks failed'}
} finally {
    try {
    if($serverStarted -and !$server.HasExited){
        $socket=[Net.Sockets.TcpClient]::new('127.0.0.1',33095)
        try{$bytes=[Text.Encoding]::ASCII.GetBytes('SHUTDOWN');$socket.GetStream().Write($bytes,0,$bytes.Length)}finally{$socket.Dispose()}
        if(!$server.WaitForExit(15000)){throw 'Test server did not stop; test database retained for inspection'}
    }
    if($created){
        & $mysql @mysqlArgs --execute='DROP DATABASE exam_seating_s7_check'
        if($LASTEXITCODE -ne 0){throw 'Test database cleanup failed'}
    }
    } finally {
    $env:DB_URL=$savedDatabaseEnvironment.URL;$env:DB_USER=$savedDatabaseEnvironment.User;$env:DB_PASSWORD=$savedDatabaseEnvironment.Password
    Pop-Location
    }
}
