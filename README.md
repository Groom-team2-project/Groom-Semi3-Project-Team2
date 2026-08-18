> 함께 고르고, 함께 완성하는 여행 플래너

이 프로젝트는 친구, 연인, 가족과 여행을 준비할 때 흩어지는 장소 후보와 의견, 일정을 한곳에 모아 관리하는 협업형 여행 계획 서비스입니다.

## 프로젝트 배경

여럿이 여행을 준비하면 지도 앱의 장소 링크, 메신저의 의견, 메모장의 일정이 서로 다른 곳에 쌓입니다. 그 결과 후보를 다시 찾거나, 누가 어떤 장소를 원했는지 확인하거나, 최종 일정을 공유하는 데 불필요한 시간이 듭니다.

프로젝트는 이 과정을 하나의 흐름으로 연결합니다.

1. 함께 갈 장소를 탐색합니다.
2. 마음에 드는 장소를 후보로 저장합니다.
3. 투표와 의견을 통해 방문지를 결정합니다.
4. 선택한 장소를 시간대별 일정으로 구성합니다.
5. 완성된 여행 계획을 멤버들과 공유합니다.

## 핵심 기능

- **장소 탐색**: 검색과 카테고리 필터를 이용한 여행지 탐색
- **지도 기반 확인**: 장소의 위치와 이동 동선을 한눈에 확인
- **후보 장소 관리**: 관심 있는 장소 저장 및 후보 목록 구성
- **공동 의사결정**: 멤버별 투표와 댓글을 통한 의견 수렴
- **일정 편집**: 선택한 장소를 시간대별 여행 일정에 추가
- **여행별 협업**: 여행 유형과 멤버에 맞는 플랜 생성 및 공유
- **반응형 화면**: 데스크톱과 모바일 환경을 고려한 사용자 경험

## 현재 진행 상태

| 영역 | 상태 |
| --- | --- |
| 서비스 콘셉트 및 주요 화면 | 완료 |
| 인터랙션 목업 | 완료 |
| 프런트엔드 애플리케이션 | 구현 예정 |
| 서버 및 데이터베이스 | 구현 예정 |
| 회원·여행 멤버 관리 | 구현 예정 |
| 실제 지도·장소 데이터 연동 | 구현 예정 |
| 배포 및 운영 환경 | 구현 예정 |

## MVP 구현 범위

- [ ] 회원가입 및 로그인
- [ ] 여행 생성·수정·삭제
- [ ] 초대 링크를 통한 멤버 참여
- [ ] 장소 검색 및 지도 표시
- [ ] 후보 장소 저장·삭제
- [ ] 후보별 투표 및 댓글
- [ ] 날짜·시간별 일정 편집
- [ ] 여행 계획 공유
- [ ] 모바일 화면 최적화

## 도메인 문서

- [장소 도메인 명세](docs/places-spec.md)
- [일정 도메인 명세](docs/schedules-spec.md)
- [활동 기록 정책](docs/activity-log-spec.md)

## 개발 로드맵

### 1. 기반 구성

- 제품 요구사항과 사용자 흐름 구체화
- 기술 스택 및 프로젝트 구조 확정
- 디자인 시스템과 공통 UI 컴포넌트 구축

### 2. 핵심 기능 구현

- 인증과 여행·멤버 관리
- 장소 검색, 지도 및 후보 저장
- 투표, 댓글, 일정 편집

### 3. 협업 경험 개선

- 실시간 변경 사항 반영
- 권한 및 초대 흐름 정교화
- 이동 동선과 일정 충돌 안내

### 4. 출시 준비

- 반응형 화면 및 접근성 점검
- 테스트, 오류 추적, 성능 최적화
- 배포 자동화와 운영 환경 구성

## 실행하기

Java 21이 필요합니다. 별도의 Gradle 설치 없이 저장소에 포함된 Gradle Wrapper를 사용합니다.

### 애플리케이션 실행

```bash
./gradlew bootRun
```

Windows PowerShell에서는 다음 명령을 사용합니다.

```powershell
.\gradlew.bat bootRun
```

실행 후 `http://localhost:8080`에서 목업 화면을 확인할 수 있습니다. 서버 상태는 `http://localhost:8080/actuator/health`에서 확인합니다.

### 테스트

```bash
./gradlew test
```

로컬에서는 별도의 데이터베이스 설정 없이 H2 인메모리 데이터베이스를 사용합니다. MySQL을 사용할 때는 `.env.example`을 참고해 `.env` 파일을 만듭니다. `.env`는 앱 실행 시 자동으로 읽힙니다(별도로 `export` 안 해도 됩니다). 실제 비밀번호가 포함된 `.env` 파일은 Git에 올리지 않습니다.

### 데이터베이스 마이그레이션

스키마 변경은 Hibernate 자동 생성이 아니라 Flyway 마이그레이션으로 관리합니다. 새 SQL 파일은
`src/main/resources/db/migration`에 추가합니다. 이미 적용된 마이그레이션 파일은 수정하지 않고,
다음 버전의 새 파일을 추가합니다.

버전 번호는 도메인별로 구간을 미리 나눠두지 않고, `V2`, `V3`, `V4` ... 순서대로 그냥 다음 번호를
씁니다.

```text
V{번호}__{설명}.sql
예) V2__add_places_table.sql
```

`develop`을 최신으로 받아서 가장 큰 번호 다음 번호를 쓰세요. PR 두 개가 같은 번호를 써서 겹치면,
**아직 merge 안 된 쪽만** 번호를 바꾸세요. 이미 merge된 마이그레이션의 번호나 내용을 바꾸면 다른 사람
환경에서 에러가 나니 절대 건드리지 마세요.

`V1__init_schema.sql`은 공통 초기 스키마이므로 수정하지 않습니다.

## Docker Compose 배포

현재 배포 구성은 한 대의 EC2에서 백엔드와 MySQL을 함께 실행합니다. MySQL 데이터는
`moigo_mysql_data` Docker 볼륨에 보존되고, DB 포트는 EC2 외부에 공개하지 않습니다.

### 배포 전 확인

이 구성은 `depends_on.required`를 사용하므로 **Docker Compose v2.20.0 이상**이 필요합니다. EC2에서
다음 명령을 실행하고 출력된 버전이 `2.20.0` 이상인지 확인합니다.

```bash
docker compose version
docker compose version --short
```

### 첫 EC2 통합 배포

1. `.env.example`을 `.env`로 복사하고 비밀번호, JWT, 카카오 OAuth 값을 실제 값으로 변경합니다.
2. `.env`의 `COMPOSE_PROFILES=local-db` 설정을 유지합니다.
3. EC2 보안 그룹에서 `.env`의 `APP_PORT`만 필요한 대상에 허용합니다. `APP_PORT`가 없으면 기본값은
   `8080`입니다.
4. 다음 명령으로 빌드하고 실행합니다.

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f backend
```

`COMPOSE_PROFILES=local-db`가 설정되어 있어 MySQL도 함께 실행됩니다. 배포 후
`.env`에 지정한 `APP_PORT`로 Actuator 상태가 `UP`인지 확인합니다.

```text
APP_PORT 미설정 또는 APP_PORT=8080: http://EC2주소:8080/actuator/health
APP_PORT=9090:                    http://EC2주소:9090/actuator/health
```

컨테이너 내부 애플리케이션 포트와 Docker 헬스체크는 항상 `8080`을 사용하고, `APP_PORT`는 EC2 외부에
공개하는 포트만 변경합니다.

### 향후 RDS 전환

RDS 전환은 현재 첫 배포 범위에 포함하지 않습니다. 실제 전환 시에는 환경변수 두 개만 바꾸는 것으로 끝내지
않고 다음 작업을 함께 진행해야 합니다.

- JDBC URL에 `sslMode=VERIFY_IDENTITY`를 적용해 인증서와 RDS 호스트 이름을 모두 검증합니다.
- Amazon RDS 루트 CA를 Java trust store에 등록하고 백엔드 컨테이너에 읽기 전용으로 마운트합니다.
- RDS 보안 그룹의 3306 인바운드는 인터넷 전체가 아니라 백엔드 EC2 보안 그룹에서만 허용합니다.
- EC2 MySQL 데이터를 RDS로 마이그레이션하고 데이터 정합성을 확인합니다.
- `.env`의 `COMPOSE_PROFILES`를 비우고 `COMPOSE_DB_URL`을 RDS 주소로 변경합니다.
- 전환 검증이 끝날 때까지 기존 `moigo_mysql_data` 볼륨을 롤백 용도로 보존합니다.

RDS JDBC URL은 다음 형식을 사용합니다.

```dotenv
COMPOSE_DB_URL=jdbc:mysql://RDS엔드포인트:3306/moigo?serverTimezone=Asia/Seoul&characterEncoding=UTF-8&sslMode=VERIFY_IDENTITY
```

인증서 다운로드, trust store 생성, 데이터 이전과 실제 전환 명령은 RDS 인스턴스와 사용하는 CA가 확정된
뒤 작성합니다.

## 프로젝트 구조

```text
.
├── gradle/                         # Gradle Wrapper
├── src/
│   ├── main/
│   │   ├── java/com/groom/moigo/
│   │   │   ├── domain/
│   │   │   │   ├── auth/          # 인증·인가 (카카오 로그인, JWT)
│   │   │   │   ├── user/          # 유저 엔티티
│   │   │   │   ├── plan/          # 여행 계획·멤버·초대
│   │   │   │   ├── place/         # 장소 검색·저장
│   │   │   │   ├── schedule/      # 일정·댓글
│   │   │   │   └── vote/          # 투표
│   │   │   └── global/            # 공통 설정, 에러, 응답 포맷
│   │   └── resources/
│   │       ├── db/migration/     # Flyway SQL 마이그레이션
│   │       ├── static/index.html  # UI 목업
│   │       └── application.yml
│   └── test/                       # 테스트
├── .env.example                    # 환경 변수 예시
├── Dockerfile                      # 백엔드 멀티 스테이지 이미지 빌드
├── docker-compose.yml              # 백엔드 + 선택형 MySQL 배포 구성
├── build.gradle
└── settings.gradle
```

## 기여하기

현재는 초기 구현 단계입니다. 기능 제안이나 개선 의견은 GitHub Issue로 남겨 주세요. 구현 작업을 시작하기 전에는 관련 Issue에서 범위와 방향을 먼저 공유해 주세요.

---

**여행 계획의 시작부터 결정까지, 모두가 함께.**
