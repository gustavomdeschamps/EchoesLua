<#
    Percorre a campanha inteira com as telas de producao e input sintetico.

    Roda fora do Gradle de proposito: o driver instancia as Screens reais e
    precisa de um processo limpo, sem daemon. Antes de chamar este script:

        gradlew.bat :core:jar :lwjgl3:classes driveClasspath

    Saida: relatorio no console e capturas em build/campaign-drive/.
    O save do jogador nao e tocado - o driver usa build/drive-home como HOME.
#>
$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
$jdk  = 'C:\Program Files\Java\jdk-21.0.10'
$cpFile = Join-Path $root 'build\drive-classpath.txt'
if (-not (Test-Path $cpFile)) { Write-Host 'Rode antes: gradlew.bat driveClasspath'; exit 1 }
$cp  = (Get-Content $cpFile -Raw).Trim()
$out = Join-Path $root 'build\drive-classes'
New-Item -ItemType Directory -Force -Path $out | Out-Null

& "$jdk\bin\javac.exe" -encoding UTF-8 -proc:none -nowarn -cp $cp -d $out (Join-Path $root 'tools\java\CampaignDrive.java')
if ($LASTEXITCODE -ne 0) { Write-Host 'Falha ao compilar o driver.'; exit 1 }

Push-Location (Join-Path $root 'assets')
& "$jdk\bin\java.exe" "-Duser.home=$root\build\drive-home" -cp "$out;$cp" CampaignDrive
$code = $LASTEXITCODE
Pop-Location
exit $code
