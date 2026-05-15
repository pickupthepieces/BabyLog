param(
  [Parameter(Mandatory = $true)]
  [string]$HostName,
  [string]$User = "root",
  [string]$RemotePath = "/var/www/babylog"
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$AppPath = Resolve-Path (Join-Path $ProjectRoot "app")
$DistPath = Join-Path $AppPath "dist"
$ArchivePath = Join-Path $env:TEMP "babylog-dist.tar.gz"
$Target = "$User@$HostName"

Push-Location $AppPath
try {
  npm run build
}
finally {
  Pop-Location
}

if (Test-Path $ArchivePath) {
  Remove-Item -LiteralPath $ArchivePath -Force
}

tar -czf $ArchivePath -C $DistPath .

ssh $Target "sudo mkdir -p '$RemotePath'"
scp $ArchivePath "${Target}:/tmp/babylog-dist.tar.gz"
ssh $Target "sudo rm -rf '$RemotePath'/* && sudo tar -xzf /tmp/babylog-dist.tar.gz -C '$RemotePath' && rm -f /tmp/babylog-dist.tar.gz"

Write-Host "BabyLog PWA deployed to ${Target}:$RemotePath"
