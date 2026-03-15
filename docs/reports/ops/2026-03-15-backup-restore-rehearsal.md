# BootSync Backup/Restore Rehearsal 2026-03-15

## 목적

- 로컬 환경에서 DB 백업 자동화 스크립트가 실제 SQL dump, manifest, report를 생성하는지 확인한다.
- 생성된 dump를 별도 MySQL 컨테이너에 복원해 기본 데이터가 살아나는지 확인한다.
- 실제 S3 업로드 전 단계에서 스크립트 경로와 로그 산출물을 고정한다.

## 실행 시각

- backup rehearsal: 2026-03-15 03:03 KST
- restore rehearsal: 2026-03-15 03:05 KST

## 실행 명령

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File C:\B_Recheck\scripts\ops\Invoke-MySqlBackupToS3.ps1 -SkipUpload
```

```powershell
docker run -d --name bootsync-mysql-restore-test -e MYSQL_ROOT_PASSWORD=rootpw -e MYSQL_DATABASE=bootsync -e MYSQL_USER=bootsync -e MYSQL_PASSWORD=restorepw mysql:8.4
powershell -NoProfile -ExecutionPolicy Bypass -File C:\B_Recheck\scripts\ops\Invoke-MySqlRestoreFromS3.ps1 -SourceFile C:\B_Recheck\build\ops-backup\bootsync-20260315-030309.sql -ContainerName bootsync-mysql-restore-test
```

## 백업 결과

- dump file: `C:\B_Recheck\build\ops-backup\bootsync-20260315-030309.sql`
- dump size: `13,897 bytes`
- SHA-256: `62CABAFB36A39DE39AB08D2D4C034F89BB3F223F3460296F79EA2EF50DD1CE13`
- manifest: `C:\B_Recheck\build\ops-backup\bootsync-20260315-030309.manifest.json`
- report: `C:\B_Recheck\build\ops-backup\reports\bootsync-20260315-030309.md`
- stderr log: `C:\B_Recheck\build\ops-backup\logs\bootsync-20260315-030309-mysqldump.stderr.log`

## 복원 결과

- restore target container: `bootsync-mysql-restore-test`
- source dump: `C:\B_Recheck\build\ops-backup\bootsync-20260315-030309.sql`
- restore script log root: `C:\B_Recheck\build\ops-restore\logs`
- restored row counts:
  - `member = 2`
  - `attendance_record = 5`
  - `snippet = 3`
- source container query row counts와 동일하게 복원됨을 확인했다.

## 경고 / 참고

- backup stderr와 restore stderr에는 공통으로 `Using a password on the command line interface can be insecure.` 경고만 남았고, exit code 실패는 없었다.
- backup rehearsal은 `-SkipUpload`로 수행했기 때문에 실제 S3 object 생성과 IAM 권한 검증은 아직 하지 않았다.
- 이번 rehearsal은 로컬 기준 기능 검증이며, 공개 출시 전에는 운영 또는 prod-like 환경에서 실제 S3 업로드 1회 이상과 `RTO 8시간` 측정을 별도 기록해야 한다.
