#
# 원격 의존성 서버(EC2)로 SSH 포트 포워딩을 연다.
# 로컬 IDE에서 local 프로파일로 애플리케이션을 실행하기 전에 먼저 기동한다.
#
# 접속 정보는 저장소 루트의 .env.local 에서 읽는다:
#   OMISYS_DEP_HOST=ubuntu@<ip>          (필수)
#
# SSH 키는 저장소 루트의 omisys.pem 으로 고정한다. 다른 위치는 보지 않는다.
#
# 포워딩 포트: 3306(MySQL, LOCAL_MYSQL_PORT 로 변경 가능) / 6379-6383(Redis 5개) / 29092(Kafka) / 9200(ES)
# 종료: Ctrl+C
#
# 키 파일 ACL 이 소유자 외에게 열려 있으면 SSH 가 거부하므로 실행 시 자동으로 정리한다.
#
# 자세한 절차는 docs/development/local-setup.md 참조.

# 저장소 루트의 .env.local 을 먼저 읽는다. 환경변수로 직접 넘긴 값이 우선한다.
$RepoRoot = Split-Path -Parent $PSScriptRoot
$EnvFile = Join-Path $RepoRoot ".env.local"
if (Test-Path -LiteralPath $EnvFile) {
    foreach ($line in Get-Content -LiteralPath $EnvFile) {
        if ($line -match '^\s*([A-Z_][A-Z0-9_]*)=(.*)$') {
            $name = $Matches[1]
            if (-not [Environment]::GetEnvironmentVariable($name, 'Process')) {
                [Environment]::SetEnvironmentVariable($name, $Matches[2].Trim(), 'Process')
            }
        }
    }
}

$Ec2Host = $env:OMISYS_DEP_HOST

# 키 파일은 저장소 루트로 고정한다. PC 마다 여기에 omisys.pem 만 놓으면 된다.
$KeyPath = Join-Path $RepoRoot "omisys.pem"
# 로컬에 MySQL 이 이미 3306 을 잡고 있으면 bind 가 실패한다. 그럴 때만 바꾼다.
$LocalMysqlPort = if ($env:LOCAL_MYSQL_PORT) { $env:LOCAL_MYSQL_PORT } else { "3306" }

if ([string]::IsNullOrWhiteSpace($Ec2Host)) {
    Write-Error 'OMISYS_DEP_HOST 가 설정되지 않았습니다. 예) $env:OMISYS_DEP_HOST = "ubuntu@1.2.3.4"'
    exit 1
}

if (-not (Test-Path -LiteralPath $KeyPath -PathType Leaf)) {
    Write-Error "SSH 키가 없습니다: $KeyPath`nomisys.pem 을 저장소 루트에 두십시오. (.gitignore 대상이라 커밋되지 않습니다)"
    exit 1
}

# Windows OpenSSH 는 소유자 외 계정이 접근 가능한 키를 거부한다
# ("UNPROTECTED PRIVATE KEY FILE"). 저장소나 Downloads 에 둔 .pem 은 상속된 ACL 때문에
# 대부분 여기에 걸리므로, 필요할 때만 소유자 전용으로 정리한다.
$Me = "$env:USERDOMAIN\$env:USERNAME"
try {
    $acl = Get-Acl -LiteralPath $KeyPath
    $extra = @($acl.Access | Where-Object { $_.IdentityReference.Value -ne $Me })
    if ((-not $acl.AreAccessRulesProtected) -or $extra.Count -gt 0) {
        Write-Host "[info] 키 파일 권한을 소유자 전용으로 정리합니다: $KeyPath"
        icacls $KeyPath /inheritance:r /grant:r "${Me}:(R)" | Out-Null
    }
} catch {
    Write-Warning "키 파일 권한을 확인하지 못했습니다: $($_.Exception.Message)"
}

Write-Host "[info] 터널 연결: $Ec2Host"
Write-Host "[info] 로컬 포트 $LocalMysqlPort / 6379-6383 / 29092 / 9200 -> 원격 의존성"
Write-Host "[info] 종료하려면 Ctrl+C"

ssh -i $KeyPath -N `
    -o ServerAliveInterval=30 `
    -L ${LocalMysqlPort}:localhost:3306 `
    -L 6379:localhost:6379 `
    -L 6380:localhost:6380 `
    -L 6381:localhost:6381 `
    -L 6382:localhost:6382 `
    -L 6383:localhost:6383 `
    -L 29092:localhost:29092 `
    -L 9200:localhost:9200 `
    $Ec2Host
