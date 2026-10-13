$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    New-Item -ItemType Directory -Path 'target/algorithm-tests' -Force | Out-Null
    & javac --release 17 -d 'target/algorithm-tests' 'src/main/java/com/examplanner/service/SeatingAllocator.java' 'src/main/java/com/examplanner/service/ManualSeatingRules.java' 'src/test/java/com/examplanner/service/SeatingAllocatorChecks.java' 'src/test/java/com/examplanner/service/ManualSeatingRulesChecks.java'
    if ($LASTEXITCODE -ne 0) { throw 'Algorithm test compilation failed' }
    & java -cp 'target/algorithm-tests' com.examplanner.service.SeatingAllocatorChecks
    if ($LASTEXITCODE -ne 0) { throw 'Algorithm tests failed' }
    & java -cp 'target/algorithm-tests' com.examplanner.service.ManualSeatingRulesChecks
    if ($LASTEXITCODE -ne 0) { throw 'Manual seating rule tests failed' }
} finally { Pop-Location }
