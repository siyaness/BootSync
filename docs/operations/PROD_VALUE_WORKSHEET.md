# BootSync Prod Value Worksheet

## 1. 문서 목적

이 문서는 BootSync 운영 배포의 **1단계 준비 작업**만 다룬다.

- 아직 AWS 리소스를 생성하지 않는다.
- 아직 실제 비밀값을 저장소에 적지 않는다.
- 지금 목표는 `무엇을`, `어디서`, `누가`, `언제` 정할지만 표로 정리하는 것이다.

함께 볼 문서:

- [PROD_ENV_CHECKLIST.md](PROD_ENV_CHECKLIST.md)
- [AWS_DEPLOYMENT_CHECKLIST.md](../planning/AWS_DEPLOYMENT_CHECKLIST.md)

## 2. 사용 원칙

- 이 문서에는 실비밀번호, 실토큰, 실도메인을 커밋하지 않는다.
- 실값은 개인 메모, 비공개 `.env`, Kubernetes Secret, AWS SSM/Secrets Manager 중 하나에 따로 둔다.
- 지금은 값이 없어도 괜찮다. 대신 `어디서 생기는 값인지`는 정해 둔다.

## 3. 초보자용 추천 기본안

AWS가 익숙하지 않다면 아래처럼 시작하는 편이 가장 무난하다.

| 항목 | 추천 시작값 | 이유 |
|---|---|---|
| AWS Region | `ap-northeast-2` | 문서 기준 추천값과 동일 |
| DB 이름 | `bootsync` | 이미 프로젝트 전반에서 쓰는 기본값 |
| 세션 쿠키 이름 | `BOOTSYNCSESSION` | 기본값 유지로 충분 |
| 시간대 | `Asia/Seoul` | 기본값 유지로 충분 |
| 프록시 신뢰 | `APP_SECURITY_TRUST_FORWARDED_HEADERS=true` 예정 | Ingress/프록시 뒤 운영 기준 |
| 메일 | 지금 쓰는 SMTP 계정 먼저, 정식 운영 전 SES 검토 | 처음부터 SES를 붙이지 않아도 시작 가능 |
| Secret 보관 | 첫 배포는 Kubernetes Secret, 나중에 SSM/Secrets Manager | 처음 진입 난이도 낮음 |

## 4. 먼저 결정할 5가지

아래 다섯 가지만 먼저 정하면 1단계는 거의 끝난다.

### 4.1 접속 주소 전략

| 질문 | 현재 결정 |
|---|---|
| 첫 배포를 `도메인`으로 바로 갈지, `EC2 임시 주소`로 먼저 smoke test 할지 | `EC2 임시 주소로 먼저 smoke test 후 도메인 연결` |
| 최종 공개 주소 후보는 무엇인지 | 아직 미정, 도메인 확보 후 확정 |

권장:

- 처음 배포 검증은 임시 주소로 해도 된다.
- 공개 전에는 `APP_PUBLIC_BASE_URL`을 실제 도메인으로 다시 맞춘다.

### 4.2 메일 발송 수단

| 질문 | 현재 결정 |
|---|---|
| 사용할 SMTP 제공자는 무엇인지 | `네이버 SMTP`로 먼저 시작 |
| 발신 주소 형식은 무엇인지 | 사용하는 네이버 메일 계정 주소 그대로 사용 예정 |
| 운영에서 `APP_RECOVERY_EMAIL_MAIL_ENABLED=true`로 갈지 | [x] 예 |

주의:

- 네이버 SMTP를 쓸 때는 네이버 도움말 기준으로 `2단계 인증`과 `애플리케이션 비밀번호` 준비가 필요하다.
- 처음에는 `MAIL_USERNAME=사용 중인 네이버 계정`, `APP_RECOVERY_EMAIL_FROM=같은 네이버 계정 주소`로 맞추는 편이 가장 단순하다.

### 4.3 DB 접근 방식

| 질문 | 현재 결정 |
|---|---|
| 운영 DB는 RDS MySQL로 갈지 | [x] 예 |
| DB 이름을 `bootsync`로 유지할지 | [x] 예 |
| DB 계정 이름 규칙을 어떻게 할지 (`bootsync_app` 등) | `bootsync_app` 권장 |

### 4.4 Secret 생성 방식

| 질문 | 현재 결정 |
|---|---|
| DB 비밀번호는 어디서 만들지 | PowerShell로 랜덤 생성 후 Secret에 저장 |
| Prometheus 토큰은 어디서 만들지 | PowerShell로 랜덤 생성 후 Secret에 저장 |
| IP HMAC secret은 어디서 만들지 | PowerShell로 랜덤 생성 후 Secret에 저장 |

권장:

- 길고 랜덤한 값으로 생성한다.
- 사람이 직접 외우는 비밀번호를 쓰지 않는다.

### 4.5 Secret 보관 위치

| 질문 | 현재 결정 |
|---|---|
| 첫 배포에서는 어디에 보관할지 (`Kubernetes Secret`, `SSM`, `Secrets Manager`) | `Kubernetes Secret` 또는 비공개 `.env`로 먼저 정리 |

## 5. 운영값 준비 시트

아래 표는 **지금 정해진 상태만** 적는 용도다.

| 변수 | 지금 상태 | 값이 나오는 곳 | 비고 |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` 예정 | 직접 결정 | 거의 고정 |
| `AWS_REGION` | `ap-northeast-2` 확정 | 직접 결정 | 문서 기본값 |
| `DB_URL` | 미정 | RDS 생성 후 확정 | 지금은 비워둬도 됨 |
| `DB_USERNAME` | `bootsync_app` 예정 | 직접 결정 | RDS 생성 시 같은 이름으로 맞추기 |
| `DB_PASSWORD` | 미정 | Secret 생성 시 확정 | 커밋 금지 |
| `APP_PUBLIC_BASE_URL` | 첫 배포는 EC2 임시 주소 예정 | 배포 후 EC2 공개 주소 확인 | recovery email 링크 기준, 이후 도메인으로 변경 |
| `APP_RECOVERY_EMAIL_FROM` | 사용하는 네이버 메일 계정 주소 예정 | 네이버 메일 계정 | 처음에는 SMTP 계정과 동일하게 시작 |
| `APP_RECOVERY_EMAIL_MAIL_ENABLED` | `true` 예정 | 직접 결정 | 운영 기준 |
| `MAIL_HOST` | 네이버 SMTP 예정 | SMTP 제공자 | 예: `smtp.naver.com` |
| `MAIL_PORT` | `587` 예정 | SMTP 제공자 | 기본 추천 |
| `MAIL_USERNAME` | 사용하는 네이버 계정 예정 | SMTP 제공자 | 보통 발신 주소와 동일하게 맞춤 |
| `MAIL_PASSWORD` | 네이버 애플리케이션 비밀번호 예정 | SMTP 제공자/Secret 생성 | 커밋 금지 |
| `APP_AUDIT_REQUEST_IP_HMAC_SECRET` | 랜덤 생성 예정 | Secret 생성 시 확정 | 커밋 금지 |
| `APP_MONITORING_PROMETHEUS_SCRAPE_TOKEN` | 랜덤 생성 예정 | Secret 생성 시 확정 | 커밋 금지 |
| `APP_SECURITY_TRUST_FORWARDED_HEADERS` | `true` 예정 | 직접 결정 | Ingress 뒤 운영 기준 |
| `SESSION_COOKIE_NAME` | `BOOTSYNCSESSION` 유지 예정 | 기본값 사용 | 바꿀 필요 거의 없음 |
| `MYSQL_SSL_MODE` | `REQUIRED` 예정 | 직접 결정 | RDS TCP backup/restore 기준 |
| `BACKUP_S3_BUCKET` | 아직 미정 | S3 버킷 생성 후 확정 | 배포 후반 단계 |

## 6. 1단계 완료 기준

아래를 만족하면 1단계 완료로 본다.

1. 최종 또는 임시 접속 주소 전략이 정해졌다.
2. SMTP 제공자가 정해졌다.
3. RDS를 쓸 것이라는 방향이 정해졌다.
4. 비밀값을 어디서 만들고 어디에 둘지 정해졌다.
5. 위 표에서 `미정`인 항목이 무엇인지 스스로 설명할 수 있다.

## 7. 비밀값 생성 추천 방식

아래 값들은 로컬 PowerShell에서 랜덤 생성한 뒤 비공개 보관 위치에 옮기는 방식을 권장한다.

```powershell
# DB 비밀번호 예시
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Max 256 }))

# Prometheus scrape token 예시
[guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')

# IP HMAC secret 예시
[guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
```

- 생성한 값은 저장소에 커밋하지 않는다.
- 첫 배포는 Kubernetes Secret 또는 비공개 `.env`에만 둔다.

## 8. 다음 단계로 넘어갈 때

1단계가 끝나면 다음은 `ECR push -> RDS 생성 -> k8s env 주입` 순서로 진행한다.

- 실제 AWS 순서는 [AWS_DEPLOYMENT_CHECKLIST.md](../planning/AWS_DEPLOYMENT_CHECKLIST.md)
- 실제 운영 변수 기준은 [PROD_ENV_CHECKLIST.md](PROD_ENV_CHECKLIST.md)
