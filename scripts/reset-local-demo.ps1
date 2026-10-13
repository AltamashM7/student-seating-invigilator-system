param([switch]$ConfirmDisposable,[string]$MysqlHome='C:\Program Files\MySQL\MySQL Server 8.0')
$ErrorActionPreference='Stop'
if (!$ConfirmDisposable) { throw 'Specify -ConfirmDisposable: this replaces ONLY the isolated demo database.' }
$projectRoot=Split-Path -Parent $PSScriptRoot
$runtimeRoot=Join-Path $projectRoot '.local-runtime'
$mysql=Join-Path $MysqlHome 'bin\mysql.exe'
$arguments=@('--no-defaults','--host=127.0.0.1','--port=33307','--user=root','--default-character-set=utf8mb4')
$actual=& $mysql @arguments --batch --skip-column-names --execute='SELECT @@datadir'
if ($LASTEXITCODE -ne 0) { throw 'Disposable database is unavailable' }
$actual=[IO.Path]::GetFullPath(($actual | Out-String).Trim().TrimEnd('/','\'))
$expected=[IO.Path]::GetFullPath((Join-Path $runtimeRoot 'mysql-data'))
if ($actual -ne $expected) { throw "Refusing to reset a server outside this workspace: $actual" }
$sql="DROP DATABASE IF EXISTS exam_seating;`n"
foreach ($file in @('database\schema.sql','database\seed.sql','database\demo-data.sql')) { $sql+=Get-Content -LiteralPath (Join-Path $projectRoot $file) -Raw; $sql+="`n" }
$OutputEncoding=[Text.UTF8Encoding]::new($false)
$sql | & $mysql @arguments
if ($LASTEXITCODE -ne 0) { throw 'Demo data load failed' }
Write-Output 'Fresh demo loaded: 240 students, 480 enrollments, 24 draft exams. Run prepare-demo.py to publish example seating.'
