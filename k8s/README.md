# BootSync Kubernetes 세트

이 폴더는 BootSync를 `EC2 + k3s` 또는 유사한 Kubernetes 환경에 올리기 위한 매니페스트 모음입니다.

## 이 README는 언제 보면 되나

- 프로젝트 전체 소개와 로컬 실행은 [README.md](/C:/B_Recheck/README.md)를 먼저 봅니다.
- 명세, 계획, 운영 문서 위치를 찾고 싶으면 [docs/README.md](/C:/B_Recheck/docs/README.md)를 봅니다.
- 이 파일은 Kubernetes 매니페스트를 실제로 적용하거나, `k8s-bootsync`, `k8s-monitoring`, `k8s-argocd`의 역할을 구분할 때 보는 전용 안내입니다.

강의 예시처럼 역할별 폴더로 나눴습니다.

```text
k8s/
  k8s-bootsync/
  k8s-monitoring/
  k8s-argocd/
```

현재 기준:

- 앱은 same-origin 구조라서 Spring Boot 컨테이너 하나가 `/app` 정적 자산과 `/api`를 함께 제공합니다.
- 운영 DB는 Kubernetes 안이 아니라 `RDS MySQL`을 사용하는 전제를 둡니다.
- `*.example.yaml` 파일은 실제 값으로 복사해서 바꿔야 하는 템플릿입니다.
- Prometheus는 앱 내부 서비스 `bootsync-actuator-service`를 통해 `/actuator/prometheus`를 스크랩합니다.
- `/actuator/prometheus`는 익명 공개가 아니라 Bearer 토큰이 있어야 응답합니다.
- 외부 Ingress는 `/actuator` 경로를 막는 방향으로 기본값을 넣었습니다.

## 폴더 구성

### `k8s-bootsync`

BootSync 앱 본체 배포 세트입니다.

- `00-namespace.yaml`: 앱 namespace
- `10-configmap.yaml`: 비민감 운영값
- `20-secret.example.yaml`: DB/메일/감사 시크릿 템플릿
- `30-deployment.yaml`: 앱 Deployment
- `40-service.yaml`: 사용자 트래픽용 Service
- `41-actuator-service.yaml`: Prometheus 스크랩용 내부 Service
- `50-ingress.yaml`: 외부 도메인 진입점
- `55-certificate.yaml`: cert-manager TLS 예시
- `60-hpa.yaml`: HPA
- `70-poddisruptionbudget.yaml`: PDB

### `k8s-monitoring`

Prometheus, Grafana, node-exporter용 매니페스트입니다.

- `00-namespace.yaml`: 모니터링 namespace
- `prometheus-scrape-secret.example.yaml`: BootSync scrape 토큰 템플릿
- `prometheus-config.yaml`: Prometheus scrape 설정
- `prometheus-rbac.yaml`: Prometheus RBAC
- `prometheus-depl_svc.yaml`: Prometheus Deployment/Service
- `node-exporter.yaml`: node-exporter DaemonSet/Service
- `grafana-secret.example.yaml`: Grafana 관리자 계정 템플릿
- `grafana-depl_svc.yaml`: Grafana datasource/Deployment/Service

### `k8s-argocd`

Argo CD 설치 이후 붙일 GitOps 템플릿입니다.

- `argocd-application.yaml`: BootSync 앱/모니터링 Application 예시
- `argocd-service.yaml`: Argo CD 서버 노출용 Service
- `argocd-ingress.yaml`: Argo CD Ingress
- `https.yaml`: Argo CD TLS 인증서 예시

## 앱 배포 순서

```powershell
kubectl apply -f k8s/k8s-bootsync/00-namespace.yaml
kubectl apply -f k8s/k8s-bootsync/10-configmap.yaml
kubectl apply -f k8s/k8s-bootsync/20-secret.yaml
kubectl apply -f k8s/k8s-bootsync/30-deployment.yaml
kubectl apply -f k8s/k8s-bootsync/40-service.yaml
kubectl apply -f k8s/k8s-bootsync/41-actuator-service.yaml
kubectl apply -f k8s/k8s-bootsync/50-ingress.yaml
kubectl apply -f k8s/k8s-bootsync/55-certificate.yaml
kubectl apply -f k8s/k8s-bootsync/60-hpa.yaml
kubectl apply -f k8s/k8s-bootsync/70-poddisruptionbudget.yaml
```

주의:

- `20-secret.example.yaml`을 `20-secret.yaml`로 복사해서 실제 값으로 바꾼 뒤 적용합니다.
- `20-secret.yaml`의 `APP_MONITORING_PROMETHEUS_SCRAPE_TOKEN`과 `prometheus-scrape-secret.yaml`의 토큰 값은 반드시 같아야 합니다.
- `55-certificate.yaml`은 `cert-manager`가 있을 때만 적용합니다.
- `60-hpa.yaml`은 `metrics-server`가 있어야 정상 동작합니다.
- `50-ingress.yaml`의 `server-snippet`은 ingress-nginx에서 snippet annotation이 허용된 경우에만 동작합니다.

## 모니터링 배포 순서

```powershell
kubectl apply -f k8s/k8s-monitoring/00-namespace.yaml
kubectl apply -f k8s/k8s-monitoring/prometheus-rbac.yaml
kubectl apply -f k8s/k8s-monitoring/prometheus-scrape-secret.yaml
kubectl apply -f k8s/k8s-monitoring/prometheus-config.yaml
kubectl apply -f k8s/k8s-monitoring/prometheus-depl_svc.yaml
kubectl apply -f k8s/k8s-monitoring/node-exporter.yaml
kubectl apply -f k8s/k8s-monitoring/grafana-secret.yaml
kubectl apply -f k8s/k8s-monitoring/grafana-depl_svc.yaml
```

주의:

- `grafana-secret.example.yaml`을 `grafana-secret.yaml`로 복사해서 관리자 비밀번호를 바꿉니다.
- `prometheus-scrape-secret.example.yaml`을 `prometheus-scrape-secret.yaml`로 복사하고, 앱 Secret의 `APP_MONITORING_PROMETHEUS_SCRAPE_TOKEN`과 같은 값으로 맞춥니다.
- Grafana와 Prometheus는 기본 ClusterIP 서비스라서 필요하면 별도 Ingress나 `kubectl port-forward`를 추가합니다.

## Argo CD 적용 순서

Argo CD 자체 설치 후 아래 예시를 적용합니다.

```powershell
kubectl apply -f k8s/k8s-argocd/argocd-service.yaml
kubectl apply -f k8s/k8s-argocd/argocd-ingress.yaml
kubectl apply -f k8s/k8s-argocd/https.yaml
kubectl apply -f k8s/k8s-argocd/argocd-application.yaml
```

주의:

- `argocd-application.yaml`의 `repoURL`과 `targetRevision`은 실제 Git 저장소 기준으로 바꿔야 합니다.
- 실제 운영에서는 placeholder secret 대신 Sealed Secret, External Secret, private overlay 같은 방식을 권장합니다.

## 운영 메모

- Ingress 뒤에서 실제 IP를 신뢰하려면 `APP_SECURITY_TRUST_FORWARDED_HEADERS=true`를 유지합니다.
- recovery email 실메일 테스트 전에는 `APP_RECOVERY_EMAIL_DEVELOPMENT_PREVIEW_ENABLED=false`인지 확인합니다.
- purge 스케줄을 운영에서 돌릴 때는 `APP_ACCOUNT_DELETION_PURGE_ENABLED=true`가 필요합니다.
