#
# 원격 의존성 서버(EC2)로 SSH 포트 포워딩을 연다.
# 로컬 IDE에서 local 프로파일로 애플리케이션을 실행하기 전에 먼저 기동한다.
#
# 사용 전 환경변수 설정:
#   $env:OMISYS_DEP_HOST = "ec2-user@<ip>"
#   $env:OMISYS_DEP_KEY  = "C:\keys\omisys.pem"
#
# 포워딩 포트: 3306(MySQL) / 6379(Redis) / 29092(Kafka) / 9200(Elasticsearch)
# 종료: Ctrl+C
#
# 자세한 절차는 docs/development/local-setup.md 참조.

$Ec2Host = $env:OMISYS_DEP_HOST
$KeyPath = $env:OMISYS_DEP_KEY

if ([string]::IsNullOrWhiteSpace($Ec2Host)) {
    Write-Error 'OMISYS_DEP_HOST 가 설정되지 않았습니다. 예) $env:OMISYS_DEP_HOST = "ec2-user@1.2.3.4"'
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
Write-Host "[info] 로컬 포트 3306 / 6379 / 29092 / 9200 -> 원격 의존성"
Write-Host "[info] 종료하려면 Ctrl+C"

ssh -i $KeyPath -N `
    -o ServerAliveInterval=30 `
    -L 3306:localhost:3306 `
    -L 6379:localhost:6379 `
    -L 29092:localhost:29092 `
    -L 9200:localhost:9200 `
    $Ec2Host
