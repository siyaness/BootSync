// BootSync Mock Data

import type { AttendanceRecord, Snippet, User } from "@/lib/app-types";

export type { AttendanceRecord, AttendanceStatus, Snippet, User } from "@/lib/app-types";
export { formatKRW, getAllTags, getKoreanDateString, getRelativeTime } from "@/lib/display";

export const mockUser: User = {
  username: 'minjun_k',
  displayName: '김민준',
  recoveryEmail: 'm***@gmail.com',
  emailVerified: false,
  accountStatus: 'active',
};

// Generate attendance for current month
function generateAttendanceData(): AttendanceRecord[] {
  const now = new Date();
  const year = now.getFullYear();
  const month = now.getMonth();
  const records: AttendanceRecord[] = [];

  // Fill in past working days (Mon-Fri)
  for (let day = 1; day <= now.getDate(); day++) {
    const date = new Date(year, month, day);
    const dayOfWeek = date.getDay();

    // Skip weekends
    if (dayOfWeek === 0 || dayOfWeek === 6) continue;
    // Skip today
    if (day === now.getDate()) continue;

    const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;

    // Simulate realistic data
    if (day === 3) {
      records.push({ date: dateStr, status: '지각', memo: '버스 지연으로 10분 늦음' });
    } else if (day === 7) {
      records.push({ date: dateStr, status: '지각', memo: '교통체증' });
    } else if (day === 10) {
      records.push({ date: dateStr, status: '조퇴', memo: '병원 예약' });
    } else if (day === 14) {
      records.push({ date: dateStr, status: '결석', memo: '감기몸살' });
    } else if (day === 18) {
      records.push({ date: dateStr, status: '지각' });
    } else if (day <= now.getDate() - 1) {
      records.push({ date: dateStr, status: '출석' });
    }
  }

  return records;
}

export const mockAttendance: AttendanceRecord[] = generateAttendanceData();

export const mockSnippets: Snippet[] = [
  {
    id: '1',
    title: 'Git 초기 설정',
    tags: ['git', '설정'],
    content: `# Git 초기 설정

프로젝트 시작 시 필수 Git 설정입니다.

## 사용자 정보 설정

\`\`\`bash
git config --global user.name "Your Name"
git config --global user.email "your@email.com"
\`\`\`

## 기본 브랜치 이름 설정

\`\`\`bash
git config --global init.defaultBranch main
\`\`\`

## 한글 파일명 표시 설정

\`\`\`bash
git config --global core.quotepath false
\`\`\`

## 현재 설정 확인

\`\`\`bash
git config --list
\`\`\``,
    createdAt: '2025-01-08T09:00:00Z',
    updatedAt: '2025-01-15T14:30:00Z',
  },
  {
    id: '2',
    title: 'Java 환경변수 설정 (Windows)',
    tags: ['java', '환경설정'],
    content: `# Java 환경변수 설정 (Windows)

## 1. JDK 다운로드 및 설치
Oracle JDK 또는 OpenJDK를 설치합니다.

## 2. 환경변수 설정

### JAVA_HOME 설정
1. 시스템 속성 → 환경변수
2. 시스템 변수 → 새로 만들기
3. 변수 이름: \`JAVA_HOME\`
4. 변수 값: \`C:\\Program Files\\Java\\jdk-17\`

### Path에 추가
\`\`\`
%JAVA_HOME%\\bin
\`\`\`

## 3. 확인

\`\`\`bash
java -version
javac -version
\`\`\`

정상적으로 버전이 표시되면 설정 완료!`,
    createdAt: '2025-01-05T10:00:00Z',
    updatedAt: '2025-01-14T11:20:00Z',
  },
  {
    id: '3',
    title: 'MySQL 연결 오류 해결',
    tags: ['mysql', '오류'],
    content: `# MySQL 연결 오류 해결

## 오류 메시지
\`\`\`
ERROR 2003 (HY000): Can't connect to MySQL server on 'localhost' (10061)
\`\`\`

## 원인
MySQL 서비스가 실행되지 않고 있음

## 해결 방법

### Windows
\`\`\`bash
# 서비스 시작
net start MySQL80

# 또는 services.msc에서 MySQL80 서비스 시작
\`\`\`

### Mac
\`\`\`bash
brew services start mysql
\`\`\`

### 포트 확인
\`\`\`bash
netstat -an | findstr 3306
\`\`\`

## 추가 확인사항
- 방화벽 설정 확인
- my.cnf(my.ini) 에서 bind-address 확인
- 포트 충돌 여부 확인`,
    createdAt: '2025-01-10T15:00:00Z',
    updatedAt: '2025-01-13T09:45:00Z',
  },
  {
    id: '4',
    title: 'Spring Boot 프로젝트 생성',
    tags: ['spring', '설정'],
    content: `# Spring Boot 프로젝트 생성

## Spring Initializr 사용

1. https://start.spring.io 접속
2. 설정:
   - **Project**: Maven
   - **Language**: Java
   - **Spring Boot**: 3.2.x
   - **Packaging**: Jar
   - **Java**: 17

## 필수 Dependencies
- Spring Web
- Spring Data JPA
- MySQL Driver
- Lombok
- Spring Boot DevTools

## 프로젝트 구조
\`\`\`
src/
├── main/
│   ├── java/com/example/demo/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   └── dto/
│   └── resources/
│       └── application.yml
└── test/
\`\`\`

## application.yml 기본 설정
\`\`\`yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: password
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
\`\`\``,
    createdAt: '2025-01-12T08:00:00Z',
    updatedAt: '2025-01-12T16:30:00Z',
  },
  {
    id: '5',
    title: '자주 쓰는 Linux 명령어',
    tags: ['linux', '명령어'],
    content: `# 자주 쓰는 Linux 명령어

## 파일/디렉토리
| 명령어 | 설명 |
|--------|------|
| \`ls -la\` | 상세 목록 (숨김 파일 포함) |
| \`cd ~\` | 홈 디렉토리 이동 |
| \`mkdir -p dir/sub\` | 하위 디렉토리까지 생성 |
| \`rm -rf dir\` | 디렉토리 강제 삭제 |
| \`cp -r src dest\` | 디렉토리 복사 |
| \`mv old new\` | 이름 변경 / 이동 |

## 검색
\`\`\`bash
# 파일 찾기
find / -name "*.log" -type f

# 파일 내용 검색
grep -r "검색어" ./src/

# 프로세스 찾기
ps aux | grep java
\`\`\`

## 권한
\`\`\`bash
chmod 755 script.sh
chown user:group file
\`\`\`

## 네트워크
\`\`\`bash
# 포트 확인
ss -tuln | grep 8080

# 네트워크 상태
ip addr show
\`\`\``,
    createdAt: '2025-01-06T13:00:00Z',
    updatedAt: '2025-01-11T10:15:00Z',
  },
];

// Allowance calculation
export const BASE_ALLOWANCE = 116000;
export const DEDUCTION_PER_ABSENCE = 5800;

export function calculateAllowance(records: AttendanceRecord[]) {
  const absences = records.filter(r => r.status === '결석').length;
  const lateCount = records.filter(r => r.status === '지각').length;
  const earlyLeaveCount = records.filter(r => r.status === '조퇴').length;
  const presentCount = records.filter(r => r.status === '출석').length;

  const lateAbsences = Math.floor(lateCount / 3);
  const earlyLeaveAbsences = Math.floor(earlyLeaveCount / 3);

  const totalDeductionDays = absences + lateAbsences + earlyLeaveAbsences;
  const totalDeduction = totalDeductionDays * DEDUCTION_PER_ABSENCE;
  const expectedAmount = Math.max(0, BASE_ALLOWANCE - totalDeduction);

  return {
    baseAmount: BASE_ALLOWANCE,
    absences,
    lateCount,
    earlyLeaveCount,
    presentCount,
    lateAbsences,
    earlyLeaveAbsences,
    totalDeductionDays,
    totalDeduction,
    expectedAmount,
  };
}
