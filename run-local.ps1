param(
  [ValidateSet("local", "kafka", "rabbitmq")]
  [string]$MessagingProvider = "local",

  [ValidateSet("dev", "oauth2")]
  [string]$SecurityMode = "dev",

  [string]$DbHost = "localhost",
  [int]$DbPort = 5432,
  [string]$DbName = "chatapp",
  [string]$DbUser = "chat",
  [string]$DbPassword = "chat",

  [string]$RedisHost = "localhost",
  [int]$RedisPort = 6379,

  [string]$KafkaBootstrapServers = "localhost:9092",

  [string]$RabbitHost = "localhost",
  [int]$RabbitPort = 5672,
  [string]$RabbitUser = "guest",
  [string]$RabbitPassword = "guest",

  [switch]$SkipTests
)

$ErrorActionPreference = "Stop"

function Require-Command {
  param([string]$Name)
  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    throw "Required command '$Name' was not found in PATH."
  }
}

Write-Host "Validating local prerequisites..." -ForegroundColor Cyan
Require-Command java
Require-Command mvn

$env:APP_SECURITY_MODE = $SecurityMode
$env:APP_MESSAGING_PROVIDER = $MessagingProvider

$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://{0}:{1}/{2}" -f $DbHost, $DbPort, $DbName
$env:SPRING_DATASOURCE_USERNAME = $DbUser
$env:SPRING_DATASOURCE_PASSWORD = $DbPassword

$env:SPRING_DATA_REDIS_HOST = $RedisHost
$env:SPRING_DATA_REDIS_PORT = "$RedisPort"

$env:SPRING_KAFKA_BOOTSTRAP_SERVERS = $KafkaBootstrapServers

$env:SPRING_RABBITMQ_HOST = $RabbitHost
$env:SPRING_RABBITMQ_PORT = "$RabbitPort"
$env:SPRING_RABBITMQ_USERNAME = $RabbitUser
$env:SPRING_RABBITMQ_PASSWORD = $RabbitPassword

Write-Host "Starting app with configuration:" -ForegroundColor Green
Write-Host ("  Security mode:      {0}" -f $SecurityMode)
Write-Host ("  Messaging provider: {0}" -f $MessagingProvider)
Write-Host ("  Postgres:           {0}:{1}/{2}" -f $DbHost, $DbPort, $DbName)
Write-Host ("  Redis:              {0}:{1}" -f $RedisHost, $RedisPort)
if ($MessagingProvider -eq "kafka") {
  Write-Host ("  Kafka:              {0}" -f $KafkaBootstrapServers)
}
if ($MessagingProvider -eq "rabbitmq") {
  Write-Host ("  RabbitMQ:           {0}:{1}" -f $RabbitHost, $RabbitPort)
}

$runArgs = @("spring-boot:run")
if ($SkipTests) {
  $runArgs = @("-DskipTests") + $runArgs
}

Write-Host ""
Write-Host "App URL: http://localhost:8080" -ForegroundColor Yellow
Write-Host "Press Ctrl+C to stop." -ForegroundColor Yellow
Write-Host ""

mvn @runArgs
