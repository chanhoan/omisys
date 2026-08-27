#
# 원격 의존성 서버(EC2)로 SSH 포트 포워딩을 연다.
# 로컬 IDE에서 local 프로파일로 애플리케이션을 실행하기 전에 먼저 기동한다.
#
# 접속 정보는 저장소 루트의 .env.local 에서 읽는다:
#   OMISYS_DEP_HOST=ubuntu@<ip>          (필수)
#   OMISYS_DEP_KEY=<키 경로>              (생략 시 자동 탐색)
#
# 포워딩 포트: 3306(MySQL, LOCAL_MYSQL_PORT 로 변경 가능) / 6379-6383(Redis 5개) / 29092(Kafka) / 9200(ES)
# 종료: Ctrl+C
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
$KeyPath = $env:OMISYS_DEP_KEY

# 키 파일: 명시된 경로가 없으면 흔한 위치를 순서대로 찾는다.
if ([string]::IsNullOrWhiteSpace($KeyPath)) {
    $candidates = @(
        (Join-Path $RepoRoot "omisys.pem"),
        (Join-Path $HOME ".ssh\omisys.pem"),
        (Join-Path $HOME "Downloads\omisys.pem"),
        (Join-Path $HOME "keys\omisys.pem")
    )
    foreach ($c in $candidates) {
        if (Test-Path -LiteralPath $c -PathType Leaf) {
            $KeyPath = $c
            Write-Host "[info] 키 파일 자동 탐색: $KeyPath"
            break
        }
    }
}
# 로컬에 MySQL 이 이미 3306 을 잡고 있으면 bind 가 실패한다. 그럴 때만 바꾼다.
$LocalMysqlPort = if ($env:LOCAL_MYSQL_PORT) { $env:LOCAL_MYSQL_PORT } else { "3306" }

if ([string]::IsNullOrWhiteSpace($Ec2Host)) {
    Write-Error 'OMISYS_DEP_HOST 가 설정되지 않았습니다. 예) $env:OMISYS_DEP_HOST = "ubuntu@1.2.3.4"'
    exit 1
}

if ([string]::IsNullOrWhiteSpace($KeyPath)) {
    Write-Error 'OMISYS_DEP_KEY 가 설정되지 않았습니다. 예) $env:OMISYS_DEP_KEY = "C:\keys\omisys.pem"'
    exit 1
}

if (-not (Test-Path -LiteralPath $KeyPath -PathType Leaf)) {
    Write-Error "키 파일을 찾을 수 없습니다: $KeyPath"
    exit 1
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
