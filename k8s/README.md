# BootSync Kubernetes 초안

이 폴더는 BootSync를 `EC2 + k3s` 또는 유사한 Kubernetes 환경에 올리기 위한 초안 매니페스트입니다.

현재 기준:

- 앱은 same-origin 구조라서 Spring Boot 컨테이너 하나가 `/app` 정적 자산과 `/api`를 함께 제공합니다.
- 운영 DB는 Kubernetes 안이 아니라 `RDS MySQL`을 사용하는 전제를 둡니다.
- 시크릿 값은 실제 값 대신 placeholder만 남겨 두었습니다.

## 파일 구성

- `00-namespace.yaml`: `bootsync` 네임스페이스
- `10-configmap.yaml`: 비민감 환경변수
- `20-secret.example.yaml`: 시크릿 예시
- `30-deployment.yaml`: 앱 배포
- `40-service.yaml`: ClusterIP 서비스
- `50-ingress.yaml`: 외부 도메인 진입점 예시

## 적용 순서

```powershell
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/10-configmap.yaml
kubectl apply -f k8s/20-secret.example.yaml
kubectl apply -f k8s/30-deployment.yaml
kubectl apply -f k8s/40-service.yaml
kubectl apply -f k8s/50-ingress.yaml
```

## 적용 전 반드시 바꿔야 할 값

- `bootsync-app` 이미지 경로
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `APP_PUBLIC_BASE_URL`
- `APP_RECOVERY_EMAIL_FROM`
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`
- `APP_AUDIT_REQUEST_IP_HMAC_SECRET`
- Ingress 호스트명

## 운영 메모

- Ingress 뒤에서 실제 IP를 신뢰하려면 `APP_SECURITY_TRUST_FORWARDED_HEADERS=true`를 유지합니다.
- recovery email 실메일 테스트 전에는 `APP_RECOVERY_EMAIL_DEVELOPMENT_PREVIEW_ENABLED=false`인지 확인합니다.
- purge 스케줄을 운영에서 돌릴 때는 `APP_ACCOUNT_DELETION_PURGE_ENABLED=true`가 필요합니다.
