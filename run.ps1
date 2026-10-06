# Genera el jar (sense executar els tests) i l'executa.
#
# Ús:
#   .\run.ps1          menú d'exemples
#   .\run.ps1 4        executa directament l'exemple 4
#   .\run.ps1 tots     executa tots els exemples

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

# Perquè els accents es vegin bé a la consola
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$jar = "target/dam-m0486-ra1-pr13-exemples.jar"

Write-Host "Generant $jar..."
mvn -q "-DskipTests" package
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

java -jar $jar @args
exit $LASTEXITCODE
