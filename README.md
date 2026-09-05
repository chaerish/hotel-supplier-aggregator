# Hotel Supplier Aggregator

- 여러 외부 숙박 상품 공급사의 재고와 요금을 하나의 플랫폼에서 통합해 보여주는 백엔드 시스템입니다. 

## 구현 목적 
- 숙박 플랫폼은 고객에게 정확한 숙소 정보를 제공해야 합니다.
- 공급사마다 상품을 표현하는 방식이 서로 달라(가격 산정 기준, 식별자 체계 등) 이를 그대로 노출하면 고객이 일관된 형태로 상품을 비교할 수 있습니다.

## 구현 목표 
- **"고객에게 일관된 정보를 정확하게 제공"** 하기 위해, 사전에 다른 형식의 공급사 응답을 표준 숙박 상품 모델로 정규화하고, 여러 공급사를 동시에 조회해 하나의 통합된 검색 결과로 병합하는 것을 목표로 합니다.
- **개인 학습 목표**: 
    - 이종 시스템 통합 설계 감각 : 실무에서는 외부 API를 여러 개 붙이는 일은 자주 일어나며, 정답이 없는 상황에서 무엇을 표준으로 삼고 무엇을 버릴지 판단하고 근거를 남기며 도메인을 빠르게 흡수하고자 합니다.
    - 비동기/병렬 처리 및 장애 격리 익히기 : 외부 서비스를 여러개 호출했을 시 우리 서비스에 장애가 전파되지 않기 위해 타임아웃, 부분 실패, 실패 판정 통일 같은 것을 직접 구현해보며 방어적 설계 습관을 갖추도록 합니다.
    - 어뎁터 패턴 사용: 우리 도메인이 외부 형식에 묶이지 않고 경계를 두는 경험을 함으로써 디자인 패턴 설계를 경험합니다.
    - 문서화 습관: 지속해서 판단 근거, 회고 등 히스토리를 남기며 문서화를 정교하게 하는 방법을 터득합니다.

## 주요 기능
- 서로 다른 형식의 외부 공급사 응답을 하나의 표준 숙박 상품 모델로 정규화한다.
- 날짜와 인원 조건으로 여러 공급사를 동시에(병렬) 조회하는 통합 검색 API
- 공급사 코드와 내부 식별자를 매핑해, 지역 검색을 지원하지 않는 공급사 API를 조회 가능한 형태로 변환
- 일부 공급사 장애나 무응답 시에도 나머지 공급사 결과로 정상 응답하는 부분 실패 허용 
- 신규 공급사 추가 시 변경 범위를 최소화하는 어댑터 계층 분리

## 실행 방법

### Docker Compose로 한 번에 실행 (권장)
클론 후 별도 설치 없이 아래 한 줄이면 PostgreSQL, Mock Supplier, 본체 앱이 함께 뜹니다.
```bash
docker compose up --build
```
- `postgres`: 컨테이너 내부는 5432, 호스트에는 5433으로 노출 (로컬에 이미 PostgreSQL이 5432를 쓰고 있는 경우와 충돌하지 않도록)
- `mock-supplier`: 9191 포트 (`SPRING_PROFILES_ACTIVE=mock`으로 같은 이미지를 재사용)
- `app`: 8080 포트, `postgres`와 `mock-supplier`가 기동된 뒤에 시작됨

### 로컬에서 직접 실행
#### 요구 사항
- JDK 21+
- 로컬에 접속 가능한 PostgreSQL (또는 `docker compose up postgres`만 따로 실행)

#### 빌드 및 실행
```bash
./gradlew clean build
./gradlew bootRun
```
접속 정보는 환경변수로 덮어쓸 수 있습니다 (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`). 기본값은 `localhost:5432/aggregator`입니다.

### Mock Supplier 서버
```bash
# "mock" 프로필로 기동하면 MockSupplierController만 활성화되고 9191 포트에서 뜬다.
# 본체 애플리케이션(8080)과 분리해, 어댑터가 자기 자신을 호출하는 상황을 방지한다.
# (DB 연결이 필요 없도록 이 프로필에서는 DataSource/JPA 자동 설정도 꺼둔다.)
./gradlew bootRun --args='--spring.profiles.active=mock'
```

### 장애 모드 전환 (Mock 조작)
```bash
curl -X POST 'http://localhost:9191/control/a/mode?value=no-response'   # Supplier A 무응답
curl -X POST 'http://localhost:9191/control/b/mode?value=error'         # Supplier B 장애 (200+E503)
curl -X POST 'http://localhost:9191/control/a/mode?value=normal'        # 정상으로 복귀
```

### 통합 검색 API 호출 예시
```bash
curl "http://localhost:8080/api/v1/stays/search?checkIn=2026-09-01&checkOut=2026-09-04&adults=2&children=0"
```

### API 문서 (Swagger UI)
앱 기동 후 브라우저에서 http://localhost:8080/swagger-ui/index.html 접속

### DB 접속 (PostgreSQL)
```bash
docker compose exec postgres psql -U aggregator -d aggregator
```

## 기술 스택 
| 항목 | 선택 | 비고                                                                                                                                                                    |
|---|---|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Language | Java 21 | 	팀/개인 숙련도가 가장 높은 언어로 빠르게 구현에 집중하기 위해 선택. Record, 패턴 매칭 등 최신 문법으로 DTO와 표준 모델을 간결하게 작성 가능                                                                                |
| Framework | Spring Boot 4.1 |                                                                                                                                                                       |
| Build | Gradle | 의존성 관리와 빌드 스크립트 작성이 Maven보다 유연하고, Spring Initializr 기본 조합과의 호환성이 좋음                                                                                                   |
| Database | PostgreSQL (Docker Compose로 함께 기동) | 매핑 테이블 저장 용도. 클론한 사람이 별도 설치 없이 `docker compose up`만으로 실제 RDB와 함께 바로 실행해볼 수 있도록 함. 테스트는 속도를 위해 H2 인메모리를 별도로 사용 (`src/test/resources/application.yml`)                                                                                                        |
| HTTP Client | Spring WebClient | 다수 공급사를 동시에(병렬) 호출하고 타임아웃과 재시도를 세밀하게 제어해야 하는 이 구현의 요구에 맞음. RestClient/RestTemplate은 비동기, 병렬 호출 제어가 상대적으로 번거로워 배제. MVC(Tomcat) 위에서 WebClient만 사용하고 WebFlux 전면 도입은 하지 않음. |


## 아키텍처 개요
```text
[Client]
   │  GET /api/v1/stays/search?checkIn=..&checkOut=..&adults=..
   ▼
[StaySearchController]
   ▼
[StaySearchService]
   │
   ├─ 1. mapping DB 조회 → 보유 숙소를 공급사별로 그룹핑, 50개 단위 배치 분할
   │
   ├─ 2. 배치별, 공급사별 호출을 CompletableFuture로 병렬 실행
   │        ┌─────────────────────┐        ┌─────────────────────┐
   │        │  SupplierAAdapter   │        │  SupplierBAdapter   │
   │        │  (WebClient 호출)    │        │  (WebClient 호출)    │
   │        └─────────┬───────────┘        └─────────┬───────────┘
   │                  ▼                               ▼
   │        [Mock Supplier A API]           [Mock Supplier B API]
   │                  │                               │
   │        SupplierAResponse                SupplierBResponse
   │                  ▼                               ▼
   │           SupplierAMapper                 SupplierBMapper
   │                  │                               │
   │                  └──────────┬────────────────────┘
   │                             ▼
   │                  List<StandardRoomOffer> (표준 모델)
   │
   ├─ 3. 실패한 배치/공급사는 SupplierAdapterException으로 격리 → 부분 실패로 기록
   │
   ▼
[병합 + 응답 조립] → SearchResponse (offers + partialFailures)
   ▼
[Client] (JSON 응답)

```

## 패키지 구조 

```text
src/main/java/.../
├── domain/           # 표준 숙박 상품 모델
├── mapping/          # 공급사 코드 ↔ 내부 식별자 매핑
│   ├── entity/       # StayMapping, RoomTypeMapping
│   ├── repository/
│   └── service/      # MappingRefreshService, 앱 기동 시 1회 실행되는 MappingStartupInitializer
├── supplier/
│   ├── SupplierAdapter.java  # 공통 인터페이스
│   ├── suppliera/    # Supplier A 전용 요청/응답 DTO + 변환
│   ├── supplierb/    # Supplier B 전용 요청/응답 DTO + 변환
│   └── error/        # SupplierErrorCode, SupplierAdapterException
├── search/           # 통합 검색 서비스, 병렬 조회, 병합
│   ├── controller/   # StaySearchController
│   ├── service/      # StaySearchService
│   └── dto/          # SearchResponse, PartialFailure
├── config/           # WebClient, 검색용 Executor 등 설정
└── mock/             # Mock Supplier 컨트롤러
```

`mapping`, `search`는 계층(entity/repository/service/controller/dto)별로 나눴다. `supplier`는 공급사(A/B)별로 나눠 DTO를 package-private으로 캡슐화하는 게 목적이라 계층 기준이 아니다 (DESIGN.md 3번 참조).

## 핵심 설계 결정

각 결정의 상세 근거, 검토 후 폐기한 대안은 [docs/DESIGN.md](docs/DESIGN.md)에 정리했습니다. 아래는 무엇을 선택했는지에 대한 요약입니다.

### 1. 표준 숙박 상품 모델

| 개념 | 표준 단위 |
|---|---|
| 숙소 | 물리적 시설 1곳 = 레코드 1개 |
| 객실 타입 | 숙소 내 객실 타입 1개 = 레코드 1개 (개별 물리 객실 아님) |
| 요금 | 요청한 숙박 기간 전체의 세금 포함 총액 |
| 재고 | 날짜별 잔여 객실 수 |

- **요금:** "기간 전체 총액, 세금 포함" 기준으로 통일했습니다. 한 공급사가 날짜별 분해 요금과 세금별도 금액을 제공하지 않아, 양쪽 공급사 데이터를 모두 손실 없이 수용할 수 있는 단위를 선택했습니다.
- **예약 가능 여부:** 연박 기간 중 하루라도 재고가 0이면 예약 불가로 판정하고, 노출 수량은 기간 중 최소 잔여 객실 수로 표시합니다.
- **조식 포함 여부:** 요금과 별개의 boolean 속성으로 유지해, 요금 차이의 숨은 원인이 드러나게 했습니다.
- 날짜별 세금 상세 내역, 서로 다른 공급사 간 동일 숙소 자동 병합은 표준 모델에서 제외 (후자는 선택 구현 항목으로 분리).

### 2. 공급사 코드 ↔ 내부 식별자 매핑

- **매핑:** 앱 기동 시 1회 생성합니다. 숙소 목록은 자주 바뀌지 않는 정적 정보인 반면, 재고와 요금은 검색 시점마다 실시간 조회하는 방식으로 성격 차이를 반영했습니다. (갱신 로직은 호출부와 분리해, 추후 스케줄러로 확장 가능한 구조입니다.)
- **객실 타입 코드** : 숙소 안에서만 유일하므로, 매핑 키에 숙소 코드를 함께 포함시켰습니다.
- **일부 공급사의 매핑 생성이 실패해도 앱은 정상 기동**하며, 성공한 공급사만 매핑을 채웁니다.

### 3. Supplier 연동 어댑터

- **공급사별 요청/응답 DTO:** 각 어댑터와 같은 패키지에 두고 package-private으로 제한해, `SupplierAdapter` 인터페이스 밖으로 노출되지 않게 했습니다. DTO → 표준 모델 변환은 전용 Mapper 클래스가 담당합니다.
- **공급사마다 다른 실패 알림 방식(HTTP 상태 코드 vs 본문 코드):** 어댑터 내부에서 `SupplierAdapterException`(공급사 타입 + `SupplierErrorCode`)으로 통일해 던집니다.
- **신규 공급사 추가 시:** `SupplierType` enum 추가 → 신규 어댑터 구현체 작성 → `@Component` 등록, 이 세 가지만으로 충분하며 기존 서비스 코드는 무변경입니다.

### 4. 통합 검색 API

- **예약 불가 상품**: 응답에서 제외하지 않고 `availableRooms: 0`으로 그대로 노출합니다. 클라이언트가 이 값으로 표시 방식을 직접 결정할 수 있게 하기 위함입니다.
- **50개 초과 숙소**: 배치로 나눠 여러 번 조회하며, 배치 간과 공급사 간 호출은 `CompletableFuture` + 전용 스레드풀로 병렬 실행합니다. (WebClient는 유지하되 WebFlux 전면 도입은 하지 않는 방향)

### 5. 연동 견고성

- **타임아웃:** 연결 2000ms, 응답 3000ms (환경변수로 오버라이드 가능). 실제 공급사 응답 시간 데이터가 없는 상태에서 고른 미검증 임시값이며, 타임아웃 발생 시 표준 예외로 변환되는 메커니즘 자체는 테스트로 확인했습니다. 자세한 근거는 [docs/DESIGN.md](docs/DESIGN.md) 5번 참조.
- **실패 판정 통일:** 공급사 A(HTTP 4xx/5xx), 공급사 B(HTTP 200 + resultCode != '0000'), 응답 자체를 못 받은 경우(연결/응답 타임아웃 포함) 모두 `SupplierAdapterException`으로 통일해서 던집니다.
- **부분 실패 허용:** 미구현 (통합 검색 서비스 구현 예정).

### 6. (선택 구현 시) 추가 설계
- 재시도 정책 / 서킷 브레이커 / 캐시 전략 / 정규화 실패 데이터 격리 / 중복 상품 병합 / 통화 처리 / 예약 대행 흐름 중 진행한 항목만 작성
---

## 구현 범위

| 항목                          | 상태   | 비고 |
|-----------------------------|------|---|
| ① 숙박 상품 통합 모델 설계            | 완료   | |
| ② Supplier 연동 어댑터           | 완료   | |
| ③ 통합 검색 API                 | 완료   | |
| ④ 연동 견고성 (타임아웃/부분실패/실패판정통일) | 완료   | |
| ⑤ Mock Supplier 구성          | 완료   | |
| 선택: 재시도 정책                  | 미완료  | |
| 선택: 서킷 브레이커                 | 미완료  | |
| 선택: API 문서 자동화 (Swagger)    | 완료   | springdoc-openapi, `/swagger-ui/index.html` |
 
