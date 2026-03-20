# BootSync AWS Final Project Guide

## 1. 문서 목적

이 문서는 BootSync를 국비 과정 최종 프로젝트 기준으로 어떻게 마무리할지 정리한 실행 가이드다.

- 다른 세션의 AI나 다른 개발자가 바로 읽고 이어서 작업할 수 있도록 만든다.
- 앱 기능 추가보다 `AWS 배포`, `Docker`, `Kubernetes`, `운영 증적`에 집중한다.
- 현재 사용자 결정인 `AWS only`, `Azure 미사용`을 기본 전제로 삼는다.

## 2. 이 문서가 반영한 기준

### 2.1 프로젝트 목표에서 읽은 방향

핵심 해석:

- 과정의 중심은 단순 웹앱 제작보다 `클라우드 인프라`, `배포`, `운영`, `자동화`에 있다.
- 현재 BootSync의 포트폴리오 방향도 `AWS 배포`, `Docker`, `Kubernetes`, `운영 증적`을 분명하게 보여 주는 편이 더 적합하다.
- 따라서 발표와 저장소 구성은 "기능이 많은 앱"보다 "실제로 AWS에 배포되고 운영 가능한 서비스"를 중심으로 정리하는 것이 좋다.

따라서 최종 프로젝트는 "기능이 많은 앱"보다 "실제로 AWS에 배포되고 운영 가능한 서비스"를 우선 목표로 둔다.

### 2.2 현재 BootSync 문서 기준

- 명세는 AWS 단일 클라우드와 완성 가능한 구조를 우선한다. [BOOTSYNC_SPEC_V2.md](../spec/BOOTSYNC_SPEC_V2.md)
- 현재 개발 단계는 기능보다 운영 준비가 남아 있다. [PROJECT_PLAN.md](PROJECT_PLAN.md)
- 아직 `AWS 배포`는 TODO이고, `S3 백업`은 운영 준비가 남아 있다. [SPEC_TRACKER.md](../spec/SPEC_TRACKER.md)
- 앱은 이미 Docker 이미지 빌드가 가능하고, 로컬 backup/restore rehearsal도 존재한다. [README.md](../../README.md) [2026-03-15-backup-restore-rehearsal.md](../reports/ops/2026-03-15-backup-restore-rehearsal.md)

## 3. 현재 의사결정

이 문서를 읽는 사람은 아래 결정을 기본 전제로 삼는다.

- 클라우드는 `AWS only`
- `Azure`는 이번 프로젝트 범위에서 제외
- 앱 기능 범위는 현재 BootSync 상태에서 거의 고정
- 지금부터의 핵심 목표는 `배포`, `운영`, `발표용 증적`
- 가능한 한 `단일 서비스 + 단일 DB` 구조를 유지
- 마이크로서비스 분해는 하지 않음

중요:
BootSync 제품 명세의 `v1 제외 기능`에는 Kubernetes가 들어가 있지만, 이는 제품 기능 범위 기준이다. Kubernetes를 "배포/운영 인프라 레이어"로 사용하는 것은 허용 가능하다. 앱을 MSA로 쪼개라는 뜻은 아니다.

## 4. 최종 추천 아키텍처

추천안은 아래 한 가지다.

```text
사용자
  ->
공인 도메인 또는 EC2 Public IP
  ->
EC2 (Ubuntu)
  ->
k3s Kubernetes
  ->
BootSync app container
  ->
RDS MySQL

ECR
  -> k3s가 앱 이미지 pull

S3
  -> DB dump / 운영 산출물 / 백업 보고서 저장

SMTP 또는 SES
  -> recovery email 발송
```

구성 요소별 역할:

- `ECR`: Docker 이미지 저장소
- `EC2 + k3s`: 앱 실행 환경
- `RDS MySQL`: 운영 DB
- `S3`: 백업 파일 및 운영 증적 저장
- `SMTP/SES`: 실제 복구 이메일 발송

## 5. 왜 이 구조를 추천하는가

- `Docker`를 실제로 사용한다.
- `Kubernetes`를 실제로 사용한다.
- `AWS` 서비스를 분명하게 보여준다.
- `EKS`보다 비용과 복잡도가 낮다.
- 1인 프로젝트 기준으로 완성 가능성이 높다.
- 앱 구조를 과도하게 바꾸지 않아도 된다.

하지 않는 것:

- 지금 단계에서 MSA로 분리하지 않는다.
- Azure 연동을 억지로 넣지 않는다.
- Kubernetes를 보여주려고 앱 구조를 망가뜨리지 않는다.

## 6. 핵심 용어를 BootSync 기준으로 설명

### 6.1 아키텍처 확정

"아키텍처를 확정한다"는 말은 서비스별 역할을 정하는 것이다.

예시:

- 앱 이미지는 어디에 저장할까 -> `ECR`
- 앱은 어디서 실행할까 -> `EC2 위 k3s`
- DB는 어디에 둘까 -> `RDS`
- 백업 파일은 어디에 둘까 -> `S3`
- 메일은 무엇으로 보낼까 -> `SMTP 또는 SES`

### 6.2 Docker 이미지

BootSync를 실행 가능한 "포장된 앱 파일"로 만든 결과물이다.

- 현재 레포에는 이미 [Dockerfile](../../Dockerfile)이 있다.
- 이 이미지를 로컬에서 만든 뒤 AWS `ECR`에 올리고, Kubernetes가 그 이미지를 가져가서 실행한다.
- standalone 런타임 이미지는 기본 `SPRING_PROFILES_ACTIVE=prod`로 시작하도록 맞췄다.
- final stage는 non-root 사용자 `bootsync`로 실행되므로, 운영 컨테이너 기본선도 함께 확보했다.

### 6.3 DB 분리

운영 배포에서는 MySQL을 앱 컨테이너 안에 넣지 않고 AWS `RDS MySQL`로 분리한다.

- 현재 [docker-compose.yml](../../docker-compose.yml)은 로컬 개발용으로 `mysql + app`를 같이 띄운다.
- 최종 배포에서는 `app container -> RDS` 구조가 더 적절하다.

### 6.4 Kubernetes 매니페스트

Kubernetes가 앱을 어떻게 배포해야 하는지 적는 YAML 파일이다.

예를 들면 아래를 정의한다.

- 어떤 Docker 이미지를 쓸지
- 컨테이너를 몇 개 띄울지
- 어떤 환경변수를 넣을지
- 어떤 포트를 열지
- 외부에서 어떤 주소로 접근할지

### 6.5 S3 백업

DB 내용을 dump 파일로 만들어 S3에 저장하는 것이다.

현재 레포에는 아래 스크립트가 있다.

- [Invoke-MySqlBackupToS3.ps1](../../scripts/ops/Invoke-MySqlBackupToS3.ps1)
- [Invoke-MySqlRestoreFromS3.ps1](../../scripts/ops/Invoke-MySqlRestoreFromS3.ps1)

주의:

- 현재 스크립트는 `-Mode docker`와 `-Mode tcp`를 모두 지원한다.
- 로컬 rehearsal은 `docker` 모드, AWS/RDS 운영 백업과 복원 rehearsal은 `tcp` 모드로 실행한다.
- 남은 일은 스크립트 구현보다 `운영 AWS 자격증명 기준 S3 업로드 1회`, `prod-like 복원 리허설`, `RTO 8시간` 실측이다.

## 7. EC2+k3s vs EKS 비교

| 항목 | EC2 + k3s | EKS |
|---|---|---|
| 난이도 | 낮음 | 높음 |
| 비용 | 낮음 | 상대적으로 높음 |
| 준비 속도 | 빠름 | 느림 |
| 발표 포인트 | 직접 구성했다는 점이 강함 | 관리형 Kubernetes 경험을 보여주기 좋음 |
| 운영 부담 | 직접 관리 많음 | control plane은 AWS가 관리 |
| 1인 최종 프로젝트 적합성 | 높음 | 조건부 |

추천 결론:

- 기본 추천: `EC2 + k3s`
- 예외: 강사가 "반드시 EKS"를 요구하거나, 팀 프로젝트로 시간/비용 여유가 충분한 경우에만 `EKS`

## 8. Kubernetes 매니페스트 구조

지금 레포 기준으로는 아래처럼 역할별 폴더로 나눠 두는 편이 더 관리하기 좋다.

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
    70-poddisruptionbudget.yaml
  k8s-monitoring/
    00-namespace.yaml
    prometheus-scrape-secret.example.yaml
    prometheus-config.yaml
    prometheus-rbac.yaml
    prometheus-depl_svc.yaml
    node-exporter.yaml
    grafana-secret.example.yaml
    grafana-depl_svc.yaml
  k8s-argocd/
    argocd-application.yaml
    argocd-service.yaml
    argocd-ingress.yaml
    https.yaml
```

파일 설명:

- `k8s-bootsync`: 앱 본체, Ingress, TLS, HPA
- `k8s-monitoring`: Prometheus, Grafana, node-exporter
- `k8s-argocd`: GitOps 적용 예시
- `41-actuator-service.yaml`: Prometheus가 내부에서 `/actuator/prometheus`를 스크랩할 때 쓰는 별도 Service
- `prometheus-scrape-secret.example.yaml`: Prometheus가 BootSync scrape용 Bearer 토큰을 읽는 Secret 템플릿
- `50-ingress.yaml`: 사용자 트래픽은 열고 `/actuator`는 외부에서 막는 방향의 예시
- `55-certificate.yaml`, `https.yaml`: cert-manager가 있을 때 붙이는 TLS 예시
- Prometheus scrape 토큰은 `k8s-bootsync/20-secret.yaml`의 `APP_MONITORING_PROMETHEUS_SCRAPE_TOKEN`과 같은 값으로 맞춘다

처음부터 필요한 최소 세트:

- namespace
- deployment
- service
- ingress
- secret/configmap

모니터링, HPA, Argo CD는 앱이 뜬 뒤 추가해도 된다.

## 9. Terraform 우선순위

Terraform을 지금 모르는 상태라면 아래 순서로 접근한다.

### 9.1 원칙

- Terraform을 먼저 배우려고 프로젝트 전체를 멈추지 않는다.
- 먼저 수동 배포를 한 번 성공시킨다.
- 그 다음 Terraform으로 천천히 치환한다.

### 9.2 우선순위

1. `ECR`
2. `S3`
3. `EC2`
4. `Security Group`
5. `RDS`

후순위:

- Route 53
- IAM 세분화
- Kubernetes 리소스

현재 단계 추천:

- Terraform은 `필수`가 아니라 `가산점` 개념으로 본다.
- 시간이 부족하면 Terraform 없이도 프로젝트는 성립한다.
- 시간이 남으면 인프라 생성 과정을 코드화하는 용도로 추가한다.

## 10. 단계별 실행 로드맵

### Phase A. 기능 범위 고정

목표:

- 더 이상 큰 기능 추가를 하지 않는다.
- 배포와 운영 준비에 집중한다.

완료 기준:

- 현재 로그인, 출결, 스니펫, 설정, recovery email까지를 최종 범위로 본다.

### Phase B. Docker 이미지 빌드와 ECR 업로드

목표:

- BootSync 이미지를 로컬에서 빌드하고 AWS ECR에 push한다.

해야 할 일:

- 로컬 `docker build` 성공
- AWS CLI 로그인
- ECR 리포지토리 생성
- 이미지 tag/push

완료 기준:

- `ECR`에 BootSync 이미지가 올라가 있다.

### Phase C. 운영 DB를 RDS로 분리

목표:

- 로컬 MySQL 대신 AWS RDS MySQL을 사용한다.

해야 할 일:

- RDS MySQL 인스턴스 생성
- Security Group에서 EC2 접근 허용
- BootSync 환경변수를 RDS endpoint 기준으로 변경
- 앱 기동과 Flyway migration 확인

완료 기준:

- BootSync가 RDS에 연결되어 정상 기동한다.

### Phase D. EC2 위 k3s 준비

목표:

- Kubernetes 실행 환경을 AWS에 준비한다.

해야 할 일:

- Ubuntu EC2 생성
- Docker 또는 container runtime 준비
- k3s 설치
- `kubectl` 접속 확인

완료 기준:

- EC2에서 k3s 클러스터가 살아 있고 배포 준비가 끝난다.

### Phase E. Kubernetes 매니페스트로 앱 배포

목표:

- Kubernetes YAML로 BootSync를 배포한다.

해야 할 일:

- `k8s/k8s-bootsync` 기준으로 deployment/service/ingress 작성
- ECR 이미지 사용
- secret/configmap 주입
- 필요하면 `k8s/k8s-monitoring`, `k8s/k8s-argocd`까지 확장
- 앱 접속 확인

완료 기준:

- 외부에서 BootSync 화면 접속 가능
- 로그인과 기본 API 동작 확인

### Phase F. 운영 기능 마감

목표:

- "돌아간다"에서 끝내지 않고 "운영 가능" 상태로 만든다.

해야 할 일:

- SMTP 또는 SES 실메일 발송 확인
- S3 백업 1회 성공
- 복원 rehearsal 1회 기록
- purge 스케줄 1회 기록
- HTTPS 또는 최소한의 외부 진입 정리

완료 기준:

- 운영 증적 문서가 남아 있다.

### Phase G. 발표 자료 준비

목표:

- 기술 선택 이유와 운영 증거를 설명할 수 있게 만든다.

발표 자료에 꼭 들어갈 것:

- 최종 아키텍처 다이어그램
- Docker -> ECR -> k3s 배포 흐름
- RDS 연결 구조
- S3 백업/복원 설명
- recovery email / 운영 runbook / 보안 포인트
- 왜 `EKS`가 아니라 `EC2+k3s`를 택했는지

## 11. 바로 다음에 해야 할 것

다음 작업의 1순위는 아래다.

1. 로컬에서 BootSync Docker 이미지 빌드
2. AWS ECR 리포지토리 생성
3. 이미지 push
4. AWS RDS 생성
5. EC2 + k3s 준비

즉, 지금 가장 먼저 할 일은 `docker build -> ECR push`다.

## 12. 하지 말아야 할 것

- 새 기능 욕심내서 일정 미루기
- Azure까지 억지로 붙이기
- MSA로 서비스 쪼개기
- Kubernetes 자체를 목표로 보고 앱 완성도를 희생하기
- Terraform을 못 한다는 이유로 배포 자체를 미루기

## 13. 다른 AI / 다른 세션을 위한 작업 규칙

이 문서를 읽는 다음 작업자는 아래 규칙을 우선 따른다.

- 사용자 목표는 `AWS 기반 최종 프로젝트 완성`이다.
- Azure는 현재 범위 밖이다.
- 우선순위는 `배포 -> 운영 -> 발표 증적 -> 선택적 자동화` 순서다.
- 앱 기능 추가는 꼭 필요한 버그 수정 외에는 최대한 하지 않는다.
- Kubernetes는 인프라 레이어로만 사용하고, 앱을 MSA로 분해하지 않는다.
- RDS를 운영 DB로 사용하는 방향을 기본값으로 본다.
- 기존 backup/restore script는 참고 자산이지만, 운영형 S3 백업은 RDS 기준으로 재정리될 수 있다.

## 14. 이 문서와 함께 읽을 문서

- [BOOTSYNC_SPEC_V2.md](../spec/BOOTSYNC_SPEC_V2.md)
- [PROJECT_PLAN.md](PROJECT_PLAN.md)
- [SPEC_TRACKER.md](../spec/SPEC_TRACKER.md)
- [AWS_DEPLOYMENT_CHECKLIST.md](AWS_DEPLOYMENT_CHECKLIST.md)
- [PROD_ENV_CHECKLIST.md](../operations/PROD_ENV_CHECKLIST.md)
- [README.md](../../README.md)
- [OPERATIONS_RUNBOOK.md](../operations/OPERATIONS_RUNBOOK.md)
- [2026-03-15-backup-restore-rehearsal.md](../reports/ops/2026-03-15-backup-restore-rehearsal.md)
