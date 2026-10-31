# One-time setup for SmarC on Windows: creates the Gradle wrapper (gradlew.bat) and local.properties.
# Run from this folder:   powershell -ExecutionPolicy Bypass -File .\setup-wrapper.ps1
# Add -Build to also build the debug APK straight away.
param([switch]$Build)
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
$gradleVersion = "8.9"

# 1) Java 17 (the Android Gradle plugin used here needs it). Falls back to Android Studio's bundled JDK.
function Get-JavaMajor { try { $o = (& java -version 2>&1 | Select-Object -First 1); if ("$o" -match '"(\d+)') { [int]$Matches[1] } else { 0 } } catch { 0 } }
if ((Get-JavaMajor) -lt 17) {
    $jbr = "C:\Program Files\Android\Android Studio\jbr"
    if (Test-Path "$jbr\bin\java.exe") {
        $env:JAVA_HOME = $jbr; $env:Path = "$jbr\bin;$env:Path"
        Write-Host "Using Android Studio's JDK: $jbr (only for this PowerShell window)"
    } else {
        throw "JDK 17 not found. Install it (winget install EclipseAdoptium.Temurin.17.JDK), open a NEW PowerShell window and run this again."
    }
}
Write-Host ("Java: " + (& java -version 2>&1 | Select-Object -First 1))

# 2) Android SDK location
if (-not (Test-Path "local.properties")) {
    $sdk = $env:ANDROID_HOME; if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }; if (-not $sdk) { $sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk" }
    if (-not (Test-Path $sdk)) { throw "Android SDK not found at $sdk. Install Android Studio (it installs the SDK) or set ANDROID_HOME, then run this again." }
    "sdk.dir=" + ($sdk -replace '\\', '/') | Set-Content "local.properties" -Encoding ASCII
    Write-Host "Wrote local.properties (sdk.dir=$sdk)"
}

# 3) Download Gradle once, generate the wrapper in a scratch folder, copy it into the project
$work = Join-Path $env:TEMP "smarc-gradle-$gradleVersion"
$zip = "$work.zip"; $home2 = Join-Path $work "gradle-$gradleVersion"
if (-not (Test-Path "$home2\bin\gradle.bat")) {
    Write-Host "Downloading Gradle $gradleVersion ..."
    Invoke-WebRequest "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip" -OutFile $zip
    if (Test-Path $work) { Remove-Item $work -Recurse -Force }
    Expand-Archive $zip -DestinationPath $work -Force
}
$scratch = Join-Path $env:TEMP "smarc-wrapper-scratch"
if (Test-Path $scratch) { Remove-Item $scratch -Recurse -Force }
New-Item -ItemType Directory $scratch | Out-Null
Push-Location $scratch
& "$home2\bin\gradle.bat" wrapper --gradle-version $gradleVersion
Pop-Location
Copy-Item "$scratch\gradlew", "$scratch\gradlew.bat" -Destination . -Force
Copy-Item "$scratch\gradle" -Destination . -Recurse -Force
Write-Host "Gradle wrapper created."

if ($Build) { & .\gradlew.bat assembleDebug } else { Write-Host "Now run:  .\gradlew.bat assembleDebug" }
