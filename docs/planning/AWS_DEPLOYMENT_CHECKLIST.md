# BootSync AWS Deployment Checklist

## 1. 문서 목적

이 문서는 BootSync를 AWS에 실제로 배포할 때 따라야 하는 실행 체크리스트다.

- 목표는 `문서상 계획`이 아니라 `실제 배포 완료`다.
- 현재 프로젝트 기준 추천 아키텍처는 `ECR -> RDS MySQL -> EC2 + k3s`다.
- 세부 개념 설명은 [AWS_FINAL_PROJECT_GUIDE.md](AWS_FINAL_PROJECT_GUIDE.md)를 먼저 보고, 이 문서는 실행 순서 기준으로 본다.

## 2. 권장 기본값

아래 값은 현재 프로젝트 기준 추천값이다. 강의 요구나 계정 사정이 있으면 바꿔도 된다.

- AWS Region: `ap-northeast-2`
- ECR repository: `bootsync-app`
- EC2 OS: `Ubuntu 24.04 LTS`
- EC2 instance type: `t3.small`
- RDS engine: `MySQL 8.4`
- Kubernetes: `k3s`
- app namespace: `bootsync`

## 3. 시작 전 준비물

- AWS 계정과 콘솔 접근
- AWS CLI 설치 및 `aws configure` 완료
- Docker Desktop 또는 Docker Engine
- 이 프로젝트 워크스페이스
- 운영용 도메인 또는 임시 EC2 Public IP
- SMTP/SES 준비

## 4. 완료 기준

이 문서 기준 배포 완료는 아래를 만족할 때로 본다.

- `ECR`에 현재 BootSync 이미지가 올라가 있다.
- `RDS MySQL`이 생성되고 앱이 정상 연결된다.
- `EC2 + k3s`에서 BootSync 앱이 기동한다.
- 외부에서 `/app/login` 접속이 된다.
- 로그인, 대시보드, 기본 API가 동작한다.

## 5. 배포 전 사전 점검

### 5.1 로컬 코드 상태 확인

- [README.md](../../README.md) 기준 주요 기능이 로컬에서 이미 동작하는지 확인
- [PROJECT_PLAN.md](PROJECT_PLAN.md) 기준 현재 우선순위가 배포 단계인지 확인
- [SPEC_TRACKER.md](../spec/SPEC_TRACKER.md) 기준 `AWS 배포`, `S3 백업`, `운영 SMTP`가 아직 남아 있는지 확인

### 5.2 로컬 빌드 확인

```powershell
cd <repo-root>
docker build -t bootsync-app:latest .
docker run --rm --entrypoint sh bootsync-app:latest -lc 'id -u; whoami; echo $SPRING_PROFILES_ACTIVE'
```

기대 결과:

- 이미지 빌드 성공
- 런타임 사용자가 `bootsync`
- 기본 프로필이 `prod`

## 6. Phase 1: ECR 준비와 이미지 push

### 6.1 변수 정리

아래 값은 실제 계정 기준으로 채운다.

```powershell
$env:AWS_REGION='<AWS_REGION>'
$env:AWS_ACCOUNT_ID='<AWS_ACCOUNT_ID>'
$env:ECR_REPOSITORY='bootsync-app'
$env:IMAGE_TAG='latest'
```

### 6.2 ECR 리포지토리 생성

```powershell
aws ecr create-repository `
  --repository-name $env:ECR_REPOSITORY `
  --region $env:AWS_REGION
```

이미 있으면 이 단계는 건너뛴다.

### 6.3 ECR 로그인

```powershell
aws ecr get-login-password --region $env:AWS_REGION `
  | docker login `
      --username AWS `
      --password-stdin "$($env:AWS_ACCOUNT_ID).dkr.ecr.$($env:AWS_REGION).amazonaws.com"
```

### 6.4 이미지 tag / push

```powershell
docker build -t bootsync-app:latest .

$imageUri = "$($env:AWS_ACCOUNT_ID).dkr.ecr.$($env:AWS_REGION).amazonaws.com/$($env:ECR_REPOSITORY):$($env:IMAGE_TAG)"

docker tag bootsync-app:latest $imageUri
docker push $imageUri
```

완료 체크:

- ECR 콘솔에서 `bootsync-app:latest` 확인
- push 시 에러 없음

## 7. Phase 2: RDS MySQL 생성

### 7.1 생성 원칙

- DB는 컨테이너 안에 두지 않는다.
- 앱은 `k3s`, DB는 `RDS`로 분리한다.
- 공개 접근보다는 `EC2 security group`에서만 접근되게 한다.

### 7.2 생성 체크리스트

- Engine: `MySQL 8.4`
- Single-AZ
- 퍼블릭 액세스: 가능하면 `No`
- DB 이름: `bootsync`
- 앱용 사용자/비밀번호 생성
- 백업 보존 기간 활성화
- Security Group: `3306`을 앱 EC2 보안 그룹에서만 허용

### 7.3 배포에 필요한 산출물

아래 값이 확보되면 다음 단계로 간다.

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

예시:

```text
jdbc:mysql://bootsync-db.xxxxxxxxxxxx.ap-northeast-2.rds.amazonaws.com:3306/bootsync?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
```

주의:

- 로컬용 `allowPublicKeyRetrieval=true`, `useSSL=false` 조합을 그대로 쓰지 않는다.
- RDS 보안 정책에 맞는 JDBC URL을 사용한다.

## 8. Phase 3: EC2 생성과 k3s 준비

### 8.1 EC2 생성 체크리스트

- Ubuntu 24.04 LTS
- `t3.small`
- 보안 그룹:
  - `22` SSH: 본인 IP만
  - `80` HTTP: 필요 시 허용
  - `443` HTTPS: 필요 시 허용
- 디스크 크기: 최소 `20GB`
- IAM Role:
  - 최소 `ECR pull`
  - 필요 시 `S3` 접근

### 8.2 k3s 설치

EC2 접속 후:

```bash
curl -sfL https://get.k3s.io | sh -
sudo kubectl get nodes
```

완료 체크:

- node가 `Ready`
- `kubectl` 명령 정상

### 8.3 kubeconfig 정리

- 원격 EC2에서 직접 `kubectl`을 사용하거나
- `~/.kube/config`를 로컬로 가져와 원격 클러스터를 조작한다.

학생 프로젝트라면 처음에는 EC2 안에서 직접 작업해도 충분하다.

## 9. Phase 4: 운영 환경값 준비

이 단계에서는 [PROD_ENV_CHECKLIST.md](../operations/PROD_ENV_CHECKLIST.md)를 같이 본다.

최소 준비:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `APP_PUBLIC_BASE_URL`
- `APP_RECOVERY_EMAIL_FROM`
- `APP_RECOVERY_EMAIL_MAIL_ENABLED=true`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`

강력 권장:

- `APP_AUDIT_REQUEST_IP_HMAC_SECRET`
- `APP_SECURITY_TRUST_FORWARDED_HEADERS=true`

## 10. Phase 5: Kubernetes 매니페스트 작성

현재 레포에는 역할별로 나눈 `k8s/` 폴더가 이미 있다.
앱 배포는 우선 `k8s/k8s-bootsync`를 기준으로 수정해서 사용한다.

```text
k8s/
  k8s-bootsync/
    00-namespace.yaml
    10-configmap.yaml
    20-secret.example.yaml
    30-deployment.yaml
    40-service.yaml
    41-actuator-service.yaml
    50-ingress.yaml
    55-certificate.yaml
    60-hpa.yaml
  k8s-monitoring/
    prometheus-config.yaml
    prometheus-rbac.yaml
    prometheus-depl_svc.yaml
    node-exporter.yaml
    grafana-depl_svc.yaml
  k8s-argocd/
    argocd-application.yaml
```

실전 적용 전 필수 수정:

- `k8s-bootsync/10-configmap.yaml`의 `APP_PUBLIC_BASE_URL`, 메일 발신 주소, purge/forwarded header 설정
- `k8s-bootsync/20-secret.example.yaml`를 실제 `20-secret.yaml`로 복사한 뒤 `DB_*`, `MAIL_*`, `APP_AUDIT_REQUEST_IP_HMAC_SECRET`, `APP_MONITORING_PROMETHEUS_SCRAPE_TOKEN` 채우기
- `k8s-monitoring/prometheus-scrape-secret.example.yaml`을 실제 `prometheus-scrape-secret.yaml`로 복사하고, 앱 Secret과 같은 Prometheus scrape 토큰으로 맞추기
- `k8s-bootsync/30-deployment.yaml`의 ECR 이미지 경로를 실제 계정/리전 값으로 변경
- `k8s-bootsync/50-ingress.yaml`의 host를 실제 도메인 또는 임시 주소에 맞게 변경
- `k8s-bootsync/55-certificate.yaml`과 `k8s-argocd/https.yaml`은 `cert-manager`가 있을 때만 적용
- `k8s-bootsync/60-hpa.yaml`은 `metrics-server`가 있어야 정상 동작

최소 반영 항목:

- namespace: `bootsync`
- image: ECR의 `bootsync-app:latest`
- port: `8080`
- env: [PROD_ENV_CHECKLIST.md](../operations/PROD_ENV_CHECKLIST.md) 기준 주입
- imagePullSecret: ECR pull 가능하게 준비
- Prometheus scrape target: `bootsync-actuator-service:8080/actuator/prometheus` with Bearer token

## 11. Phase 6: 첫 배포

### 11.1 배포 전 확인

- ECR image push 완료
- RDS endpoint 준비 완료
- prod env 값 정리 완료
- EC2/k3s 준비 완료

### 11.2 배포

```bash
kubectl apply -f k8s/k8s-bootsync/00-namespace.yaml
kubectl apply -f k8s/k8s-bootsync/10-configmap.yaml
kubectl apply -f k8s/k8s-bootsync/20-secret.yaml
kubectl apply -f k8s/k8s-bootsync/30-deployment.yaml
kubectl apply -f k8s/k8s-bootsync/40-service.yaml
kubectl apply -f k8s/k8s-bootsync/41-actuator-service.yaml
kubectl apply -f k8s/k8s-bootsync/50-ingress.yaml
kubectl apply -f k8s/k8s-bootsync/55-certificate.yaml
kubectl apply -f k8s/k8s-bootsync/60-hpa.yaml

kubectl get pods -n bootsync
kubectl get svc -n bootsync
kubectl get ingress -n bootsync
```

```bash
kubectl apply -f k8s/k8s-monitoring/00-namespace.yaml
kubectl apply -f k8s/k8s-monitoring/prometheus-rbac.yaml
kubectl apply -f k8s/k8s-monitoring/prometheus-scrape-secret.yaml
kubectl apply -f k8s/k8s-monitoring/prometheus-config.yaml
kubectl apply -f k8s/k8s-monitoring/prometheus-depl_svc.yaml
```

완료 체크:

- pod가 `Running`
- readiness/liveness 실패 없음
- 앱 로그에서 DB 연결 및 Flyway migration 성공 확인
- `/actuator/prometheus`는 헤더 없는 직접 요청에 `401`이 나오고, 내부 Prometheus scrape 토큰으로만 수집되는지 확인

## 12. Phase 7: 배포 직후 스모크 테스트

최소 아래를 확인한다.

1. `/actuator/health`
2. `/app/login`
3. 로그인
4. `/app/dashboard`
5. `/app/attendance`
6. `/app/snippets`
7. `/app/settings`

추가로 확인할 것:

- recovery email 발송 가능 여부
- 세션 유지 정상 여부
- RDS에 실제 데이터 저장 여부

## 13. Phase 8: 운영 증적 수집

배포가 끝났다고 바로 종료하지 않는다.
최소 아래 증적을 남긴다.

- ECR push 성공 스크린샷 또는 콘솔 기록
- RDS 생성 정보
- EC2 / k3s node 상태
- 앱 접속 화면
- `/actuator/health` 성공
- 로그인 성공
- SMTP 실메일 1회
- S3 업로드 1회
- prod-like 복원 rehearsal 1회

## 14. 실패 시 우선 확인 순서

1. pod 로그에서 `DB_URL`, `MAIL_*` 누락 여부
2. RDS security group 허용 여부
3. EC2 outbound/network 정상 여부
4. ECR pull 인증 실패 여부
5. `APP_PUBLIC_BASE_URL` 오설정 여부
6. `SPRING_PROFILES_ACTIVE=prod` 적용 여부

## 15. 바로 다음 한 작업

이 문서를 읽고 가장 먼저 할 일은 아래 하나다.

1. `docker build`
2. `aws ecr create-repository`
3. `docker push`

즉, 시작점은 `ECR에 이미지 올리기`다.
