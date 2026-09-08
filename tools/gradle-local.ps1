$GradleArguments = @($args)
if ($GradleArguments.Count -eq 0) { $GradleArguments = @(':lwjgl3:run') }
$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path -Parent $PSScriptRoot
$socketDirectory = Join-Path $projectDirectory 'build/socket-tmp'
New-Item -ItemType Directory -Force -Path $socketDirectory | Out-Null
$previousJavaOptions = $env:JAVA_TOOL_OPTIONS
try {
    # Java's Windows Unix-domain sockets reject this machine's shortened TEMP path.
    # Scope the canonical replacement to this build and its child JVMs only.
    $env:JAVA_TOOL_OPTIONS = ($previousJavaOptions + ' "-Djdk.net.unixdomain.tmpdir=' + $socketDirectory + '"').Trim()
    Push-Location $projectDirectory
    try {
        & (Join-Path $projectDirectory 'gradlew.bat') @GradleArguments
        $buildExitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }
} finally {
    $env:JAVA_TOOL_OPTIONS = $previousJavaOptions
}
exit $buildExitCode
