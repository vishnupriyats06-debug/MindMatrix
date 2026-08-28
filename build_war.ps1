# ============================================================
#  MindMatrix – WAR Builder Script
#  Run from project root: C:\Users\ELCOT\OneDrive\Desktop\MindMatrix\MindMatrix
#  Usage: .\build_war.ps1
# ============================================================

$projectRoot  = "C:\Users\HP\OneDrive\Desktop\MindMatrix\MindMatrix"
$warName      = "mindmatrix"
$tomcatHome   = "C:\xampp\tomcat"          # XAMPP's built-in Tomcat
$webContent   = "$projectRoot\WebContent"
$srcDir       = "$projectRoot\src"
$distDir      = "$projectRoot\dist"
$warTmp       = "$projectRoot\war_tmp"
$mysqlJar     = "$webContent\WEB-INF\lib\mysql-connector-j-9.7.0.jar"
$servletJar   = "$tomcatHome\lib\servlet-api.jar"
$classesOut   = "$webContent\WEB-INF\classes"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   MindMatrix WAR Builder" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ── Step 1: Compile Java Sources ────────────────────────────
Write-Host "[1/4] Compiling Java servlet sources..." -ForegroundColor Yellow

New-Item -ItemType Directory -Force -Path "$classesOut\com\mindmatrix" | Out-Null

$javaFiles = Get-ChildItem "$srcDir\com\mindmatrix\*.java" | Select-Object -ExpandProperty FullName

$compileResult = & javac -encoding UTF-8 -cp "$servletJar;$mysqlJar" -d $classesOut $javaFiles 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation FAILED:" -ForegroundColor Red
    Write-Host $compileResult
    exit 1
}
Write-Host "   Compilation successful!" -ForegroundColor Green

# ── Step 2: Prepare WAR staging folder ──────────────────────
Write-Host "[2/4] Preparing WAR staging folder..." -ForegroundColor Yellow

if (Test-Path $warTmp) { Remove-Item -Recurse -Force $warTmp }
New-Item -ItemType Directory -Path $warTmp | Out-Null

# Copy all web content (HTML, CSS, JS)
Copy-Item -Recurse -Path "$webContent\*" -Destination "$warTmp\" -Exclude "*.zip"

Write-Host "   Staging folder ready!" -ForegroundColor Green

# ── Step 3: Package as WAR (ZIP) ────────────────────────────
Write-Host "[3/4] Packaging as WAR..." -ForegroundColor Yellow

if (-not (Test-Path $distDir)) { New-Item -ItemType Directory -Path $distDir | Out-Null }

$warPath = "$distDir\$warName.war"
if (Test-Path $warPath) { Remove-Item -Force $warPath }

# Use Java jar tool to package the WAR cleanly with forward slashes
& jar cf $warPath -C $warTmp .

Write-Host "   WAR created: $warPath" -ForegroundColor Green

# ── Step 4: Deploy to Tomcat webapps ────────────────────────
Write-Host "[4/4] Deploying to Tomcat webapps..." -ForegroundColor Yellow

$webappsPath  = "$tomcatHome\webapps"
$deployedWar  = "$webappsPath\$warName.war"
$deployedDir  = "$webappsPath\$warName"

# Remove old deployment
if (Test-Path $deployedWar) { Remove-Item -Force $deployedWar }
if (Test-Path $deployedDir) { Remove-Item -Recurse -Force $deployedDir }

Copy-Item $warPath -Destination $webappsPath

Write-Host "   WAR deployed to: $webappsPath" -ForegroundColor Green

# ── Cleanup ─────────────────────────────────────────────────
Remove-Item -Recurse -Force $warTmp

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   BUILD SUCCESSFUL!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Now start (or restart) Tomcat:" -ForegroundColor White
Write-Host "   $tomcatHome\bin\startup.bat" -ForegroundColor Gray
Write-Host ""
Write-Host "Then open your app at:" -ForegroundColor White
Write-Host "   http://localhost:8080/mindmatrix/register.html" -ForegroundColor Cyan
Write-Host "   http://localhost:8080/mindmatrix/login.html" -ForegroundColor Cyan
Write-Host "   http://localhost:8080/mindmatrix/dashboard.html" -ForegroundColor Cyan
Write-Host ""
