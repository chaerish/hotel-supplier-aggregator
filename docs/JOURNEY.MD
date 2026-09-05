# JOURNAL

구현 진행 중 매일의 의사결정, 시행착오, AI 활용 내역을 기록합니다.

---
## DAY 1 - 2026-09-02 (수) - 도메인 파악 및 설계

### 오늘의 목표
- [x] 구현 내용을 명확하게 읽고 자신만의 언어로 다시 작성
- [x] README.MD 초안 작성
- [x] 기술 스택 정하고 정한 이유 작성
- [x] DESIGN.md 1번(표준 모델) 채우기: 요금은 세금포함 통일, 재고는 날짜별, 숙소/객실타입 필드 확정
- [x] DESIGN.md 2번(매핑) 채우기: DB 스키마 확정, 매핑 갱신 시점(앱 기동 시 1회 등) 결정


### 수행 내용
- 요구사항 문서를 읽고 도메인(공급사 연동, 표준 모델, 매핑, 통합 검색, 견고성)을 파악했다. 
- 요구사항 명세서(REQUIREMENTS.md)를 본인 말로 재작성했다. 
- README 초안에 프로젝트 소개, 주요 기능, 기술 스택(선택 이유 포함)을 작성했다. 
- 표준 숙박 상품 모델(숙소/객실타입/요금/재고 단위)을 설계하고 근거를 README.MD에 반영했다. 
- 공급사 코드 ↔ 내부 식별자 매핑 설계(테이블 구조, 갱신 시점, 실패 시 처리)를 진행했다. 
- GitHub 저장소를 생성하고 Spring Boot(Java 21 + Gradle) 프로젝트를 스캐폴딩했다.


### 의사결정
#### **[요금 단위: 요청 기간 전체 총액]**
한 공급사가 날짜별 분해 요금을 제공하지 않아, 기간 총액 단위여야 양쪽 공급사 데이터를 모두 채울 수 있다고 판단.
  - 검토했다 폐기한 대안: 날짜별(1박당) 요금 단위 -  날짜별 분해를 제공하지 않는 공급사 데이터를 채울 수 없어 폐기.
#### **[요금 기준: 세금 포함(gross)]**
한 공급사가 세금별도 금액을 제공하지 않고, 세율 구성을 알 수 없어 임의로 역산하는 것은 부정확할 위험이 있다고 판단. 고객이 실제 지불하는 최종가 기준 비교가 자연스럽다는 점도 고려.
#### **[재고 단위: 날짜별]**
두 공급사 모두 날짜별 잔여 객실 수를 제공하므로 그대로 채택.
#### **[예약 가능 여부: 연박 중 하루라도 재고 0이면 예약 불가, 가능 시 최소 잔여 객실 수 노출]**
기간 전체를 예약할 수 있어야 고객에게 의미가 있다고 판단.
####  **[Money 값 객체 도입]**
금액은 숫자만으로는 의미가 불완전하고(통화 필요), 서로 다른 통화 간 실수를 방지하기 위해 amount+currency를 하나의 값 객체로 분리.
#### **[SupplierType을 엔티티가 아닌 enum으로 설계]** 
공급사 수가 2개로 고정적이고 상태를 가질 필요가 없어, 엔티티보다 enum이 적절하다고 판단. 신규 공급사 추가 시 enum 상수 추가 + 어댑터 클래스 추가가 필요하다는 트레이드오프를 인지.
#### **[매핑 테이블 키에 숙소 코드 포함]**
객실 타입 코드가 숙소 안에서만 유일하므로 (공급사, 숙소코드, 객실타입코드) 세 값을 유니크 키로 사용.
#### **[매핑 갱신 시점: 앱 기동 시 1회]**
숙소 목록은 자주 바뀌지 않는 정적 정보이므로 구현 범위에서는 충분하다고 판단.
  - 검토했다 폐기한 대안: 주기적 스케줄러 갱신. 실제 서비스라면 더 적절하지만, 지금 범위에서는 우선순위가 낮다고 판단해 보류. 다만 핵심 로직(refreshMapping)을 호출부와 분리해 설계해, 추후 스케줄러로 손쉽게 확장 가능하도록 구조만 마련.
#### **[매핑 생성 실패 시 처리]**
일부 공급사의 숙소 목록 조회가 실패해도 앱은 정상 기동시키고, 성공한 공급사만 매핑을 채운다. 전부 실패하면 자사 등록 숙소만으로 서비스한다. 실패는 로그로 남기고 알림(Slack/메일 등)을 고려한다.
  - 검토했다 폐기한 대안: 매핑 생성 실패 시 앱 기동 자체를 중단. 외부 API 장애가 서비스 전체를 막아버리는 건 이 구현이 지향하는 "부분 실패 허용" 철학과 맞지 않아 폐기.
#### **[Supplier 연동 어댑터 구조: 공통 인터페이스 + 공급사별 구현체]**
`SupplierAdapter` 인터페이스 하나를 두고, `SupplierAAdapter`/`SupplierBAdapter`가 각자 WebClient로 자기 공급사를 호출한 뒤 표준 모델로 정규화해서 반환하도록 설계했다. 상위 서비스(StaySearchService)는 인터페이스만 알면 되고, 공급사별 DTO나 응답 형식은 어댑터 내부에 갇혀 도메인으로 새어나가지 않는다.
  - 신규 공급사 추가 시 변경 지점: ① SupplierType enum에 상수 추가, ② 신규 SupplierAdapter 구현체 작성, ③ Spring Bean 등록(List<SupplierAdapter>에 자동 주입), ④ 매핑 데이터 채우기. 이 네 가지로 한정되도록 설계해, 기존 코드를 고치지 않고 새 어댑터만 추가하면 되게 했다.

### 고민했던 것 / 의문점 (해결 과정)
#### **공급사별로 어떻게 묶어서 호출할지?**
처음엔 "왜 공급사별로 나눠서 불러야 하지?"가 이해가 안 됐는데, 각 공급사의 재고와 요금 API 자체가 서로 다른 엔드포인트이고 그 공급사가 발급한 코드만 알아듣는다는 걸 이해하고 나서, 매핑 테이블을 supplier_type으로 그룹핑해서 각 어댑터에 전달하는 구조로 정리됨.
#### **예약 가능 객실 수를 응답에서 어떻게 표현할지 고민**
별도 boolean 필드(예약 가능 여부)를 둘지, 0이라는 숫자로만 표현할지 사이에서 고민하다가, 숫자 하나로 충분히 표현 가능하다고 판단해 별도 boolean 없이 availableRooms=0을 예약 불가로 취급하는 쪽으로 정리함.
#### **연박 시 재고 값을 어떤 기준으로 보여줄지**
첫날 값만 보여줄지, 평균을 낼지, 최소값을 보여줄지 고민하다가, "기간 전체를 예약할 수 있음을 보장하는 값"이라는 의미에 가장 부합하는 최소값(보수적 정책)으로 확정.
#### **날짜/인원 필터링이 어디서 일어나는지 헷갈림**
처음엔 우리 서버 쪽에서 직접 필터링해야 하는 줄 알았는데, 실제로는 공급사 API가 요청 파라미터(checkIn/checkOut/adults/children)를 받아 자체적으로 필터링(인원 초과 객실 제외, 날짜별 재고 계산)해서 응답한다는 걸 확인함. 우리 쪽에서 직접 필터링하는 건 "연박 중 하루라도 0이면 예약불가" 판정 하나뿐.
#### **Money를 값 객체로 만들지, 필드로 풀어서 둘지 고민**
통화가 여러 개 섞일 가능성(선택 구현: 통화 처리)을 고려해 값 객체로 분리하기로 했으나, 시간이 부족하면 단순히 long amount + String currency 필드로 풀어도 무방하다고 정리함 (트레이드오프 인지).

### 막힌 지점 / 시행착오
- IntelliJ에서 Gradle sync 시 `NoClassDefFoundError: org/gradle/util/VersionNumber` 발생. 원인은 IntelliJ에 번들된 Android 플러그인이 non-Android Gradle 프로젝트에도 툴링 모델을 끼워 넣으면서 생긴 충돌이었음.
- 이어서 Gradle 버전 호환성 문제로 두 번 더 헤맴: 9.7.1에서는 `DefaultDecoratedConvention` 관련 deprecated API 충돌, 8.11.1로 낮추니 이번엔 Spring Boot 4.1.1 플러그인이 요구하는 최소 Gradle 버전(8.14)보다 낮아서 재실패. 최종적으로 8.14.3으로 맞춤.

### AI 활용
#### 1. 무엇을 물었는가? 
- 요금을 세금별도(net) 기준으로 통일할 경우, 세금포함 금액에서 세금을 역산하는 게 왜 문제가 되는지 물어봄.
##### 2. AI 답변을 어떻게 수용/수정/거부했는가?
- AI가 "tax가 부가세만을 의미하는지, 다른 요금이 섞여 있는지 스펙만으로는 알 수 없어 임의로 세율을 가정해 역산하는 건 위험하다"고 답했는데, 이 주장의 출처를 요구했고 국세청 법령 링크를 근거로 받았다.
- 이를 검증한 뒤 세금포함(gross) 통일 근거 중 하나로 채택했다(AI 답변을 그대로 쓰지 않고, 출처 확인 후 본인 판단을 더해 수용).
#### 무엇을 물었는가?
- README, DESIGN.MD 등 했던 고민들을 노션에 적고, 노션 내용을 Copy 하여 문서화 요청. 
- 이후 검토는 스스로 진행.
### 참고 자료
- https://law.go.kr/LSW/lsInfoP.do?ancYnChk=0&lsId=001571&utm_source=chatgpt.com#0000

## Day 2 - 2026-09-03 (목) - 설계

### 오늘의 목표
- [x] DESIGN.md 3~4번 초안 채워서 항목 뼈대 완성 
- [x] 구현에 필요한 어댑터 패턴 다시 이론 복습
- [x] Spring Boot 프로젝트에 도메인 패키지 구조 설계 (domain/mapping/adapter/search/resilience/mock)
- [x] JOURNAL Day 2 기록 및 회고 작성 

### 수행 내용
- 어댑터(Adapter) 디자인 패턴 개념을 다시 공부했다 (Target/Adaptee/Adapter 구조, GoF 패턴으로서의 의미). 또한 이 구현에서는 어떤 것들을 각각 의미하는 지 파악.
- Supplier 어댑터의 패키지 구조를 설계했다: `domain`(표준 모델), `supplier`(어댑터 인터페이스 + 공급사별 하위 패키지), `mapping`, `search`, `config`로 나누고, 공급사별 DTO는 각 하위 패키지에 별도 파일로 두고 package-private으로 제한하기로 결정.
- 실패 처리 표준화를 위한 `SupplierErrorCode`(enum) + `SupplierAdapterException`(커스텀 예외) 설계를 진행했다.
- DTO → 표준 모델 변환 로직 위치(어댑터 내부 private 메서드 vs 별도 Mapper 클래스)를 비교하고, 별도 Mapper 클래스(`SupplierAMapper`, `SupplierBMapper`)로 분리하기로 결정.
- 병렬 호출 방식(WebFlux 리액티브 vs CompletableFuture)의 장단점을 비교하고 CompletableFuture로 결정.
- 위 결정들을 DESIGN.md 3번(경계 설계), 4번(50개 초과 시 배치 처리) 항목에 반영해 작성했다.

### 의사결정
#### [패키지 이름: adapter 대신 supplier]
원래 계획(오늘의 목표)에는 `adapter` 패키지로 적어뒀었는데, 실제로 구조를 짜보니 이 패키지 안에는 `SupplierAdapter` 인터페이스뿐 아니라 공급사별 DTO, Mapper, 실패 관련 타입까지 다 들어가게 됐다. "어댑터 클래스들의 모음"이 아니라 "공급사 연동과 관련된 모든 것의 모음"에 더 가깝다고 판단해서, 디자인 패턴 이름(`adapter`) 대신 도메인 이름(`supplier`)을 패키지명으로 선택했다.
  - 검토했다 폐기한 대안: `adapter` 패키지명 유지. 클래스 이름 자체에 이미 `Adapter`가 들어가 있어서(`SupplierAdapter`, `SupplierAAdapter` 등), 패키지명까지 `adapter`로 하면 `adapter.SupplierAdapter`처럼 이름이 겹치는 느낌이 든다는 점도 폐기 이유 중 하나였다.
#### [DTO→표준 모델 변환 로직 위치: 별도 Mapper 클래스]
- 요금 표준화(세금 포함/별도, 기간 총액/날짜별) 계산이 이 지점에 몰려 로직이 복잡해질 가능성이 크고, 변환 로직만 독립적으로 단위 테스트하고 싶어서 어댑터 클래스(HTTP 호출 담당)와 책임을 분리했다.
    - 검토했다 폐기한 대안: 어댑터 내부 private 메서드. 필드가 단순 대입 수준일 때는 충분하지만, 표준화 계산이 들어가는 이 구현 특성상 어댑터 클래스가 비대해질 위험이 있어 폐기.
#### [병렬 호출 방식: CompletableFuture]
- `SupplierAdapter` 인터페이스를 동기 리턴 타입으로 유지하면서, 서비스 계층에서 `CompletableFuture.supplyAsync(..., executor)`로 공급사별, 배치별 호출을 동시에 실행하기로 결정. WebClient는 그대로 사용(내부에서 `.block()`으로 동기화)하되, 애플리케이션 전체를 리액티브 체인으로 짜지는 않는 방식.
    - 검토했다 폐기한 대안: `Mono<List<StandardRoomOffer>>` + `Flux.merge()` 완전 리액티브 방식. 요구사항 문서에 "WebFlux 전면 도입 불필요"로 명시되어 있고, 공급사 2개, 배치 수가 적은 구현 규모에서는 이벤트 루프 기반 논블로킹의 이점이 거의 드러나지 않는 반면, 리액티브 체인 특유의 디버깅 난이도만 늘어난다고 판단해 폐기. 다만 트래픽이 커질 경우의 대안으로 문서에는 남겨둠.
    - 트레이드오프 인지: 어댑터 내부에서 `.block()`을 호출하므로 WebClient의 논블로킹 이점을 완전히 살리지 못한다는 점은 인지하고 있으며, 대신 코드 단순성과 명시적 스레드풀 제어를 택했다.
#### [실패 처리 표준화: SupplierErrorCode + SupplierAdapterException]
- 공급사 A(HTTP 상태 코드)와 B(HTTP 200 + 본문 코드)의 서로 다른 실패 알림 방식을, 어댑터 내부에서 공통 커스텀 예외(`SupplierAdapterException`, 공급사 타입 + 표준 에러 코드 보유)로 통일해 던지기로 결정. 상위 계층은 이 예외 타입 하나만 catch하면 부분 실패 처리가 가능하다.
    - 검토했다 폐기한 대안: 실패 시 단순히 빈 리스트만 반환. 실패와 "원래 결과가 0건"인 정상 케이스를 구분할 수 없어, 부분 실패 여부를 응답에 포함해야 하는 FR-4 요구사항을 충족하지 못해 폐기.
### 막힌 지점 / 시행착오
- 처음엔 "WebClient를 쓰라"는 요구사항과 "CompletableFuture로 병렬화"가 서로 모순되는 게 아닌지 헷갈렸다. WebClient는 HTTP 클라이언트 "라이브러리" 선택이고, WebFlux(리액티브 스타일)는 별개의 "프로그래밍 스타일" 선택이라는 걸 구분하고 나서, WebClient는 그대로 사용하되 결과를 어댑터 내부에서 `.block()`으로 동기화하고 그 바깥에서 CompletableFuture로 병렬화하는 조합이 요구사항과 상충하지 않는다는 걸 확인했다.
- 지금은 최소 요구사항에 맞춰 CompletableFuture로 결정하나, 추후 동시 접속이 많아질 때 등과 같은 확장성을 고려한다면 WebFlux(Mono/Flux)에 대한 코드도 생각하고 있다.
### AI 활용
#### 무엇을 물었는지
- 어댑터 패턴의 개념(Target/Adaptee/Adapter)과 이 구현에 왜 필요한지, DTO를 어느 패키지에 두고 접근 제어자를 어떻게 설정해야 도메인 계층으로 안 새어나가는지 물어봄.
- Mapper 클래스로 분리하는 기준(private 메서드 vs 별도 클래스)을 물어봄.
- WebFlux 리액티브 방식과 CompletableFuture 방식의 기술적 장단점(스레드 모델, 확장성, 디버깅 난이도, 요구사항과의 정합성)을 비교해달라고 물어봄.
- WebClient 사용 요구사항과 CompletableFuture 병렬화가 상충하지 않는지 확인함.

##### AI 답변을 어떻게 수용/수정/거부했는지
- 어댑터 패턴 설명과 패키지 구조 제안은 개념적으로 타당하다고 판단해 채택하되, 실제 클래스 설계(경계/변경 지점 등)는 요구사항 문서와 직접 대조해 표현을 다시 정리했다.
- Mapper 클래스 분리 기준은 "이 구현은 요금 표준화 로직이 복잡해질 가능성이 크다"는 이유로 Mapper 분리 쪽을 택했고, 코드는 아직 직접 작성하지 않았다 (설계만 AI와 논의, 구현은 직접 진행 예정).
- WebFlux vs CompletableFuture 비교표는 그대로 수용했으나, 최종 선택 근거는 "요구사항 문서에 WebFlux 전면 도입이 명시적으로 불필요하다고 되어 있다"는 점을 직접 재확인한 뒤 결정에 반영했다.
- DESIGN.md/JOURNAL.md에 AI가 생성한 문장을 그대로 붙여넣지 않고, 직접 검토하고 수정하며 작성했다.

## Day 3 - 2026-09-05 (토) - Supplier A/B Mapper 구현

### 오늘의 목표
- [x] Supplier A/B DTO를 표준 모델로 변환하는 Mapper 클래스 구현
- [x] Mapper 단위 테스트 작성
- [x] 미커밋 상태였던 작업을 단위별로 나눠 커밋
- [x] SupplierErrorCode enum과 SupplierAdapterException 작성 (공급사별 실패 판정 통일용)
- [x] SupplierAAdapter와 요청/응답 DTO(package-private) 작성
- [x] SupplierBAdapter와 요청/응답 DTO(package-private) 작성 (HTTP 200 + 본문 코드 실패 케이스 처리 포함)
- [x] WebClientConfig 작성 (연결/응답 타임아웃 임시값 적용)
- [x] StayMapping, RoomTypeMapping JPA 엔티티 작성
- [x] StayMappingRepository, RoomTypeMappingRepository 작성
- [x] 매핑 생성 로직(refreshMapping) 작성, 앱 기동 시 1회 호출
- [x] 일부 공급사의 fetchCatalog() 실패 시 그 공급사만 건너뛰는 처리
- [ ] search 패키지 구조 잡기
- [ ] StaySearchService에 List<SupplierAdapter> 자동 주입 설정
- [ ] 매핑 테이블 조회 결과를 공급사별로 그룹핑하는 로직 작성
- [ ] 50개 초과 시 배치 분할 로직 작성
- [ ] CompletableFuture 기반 병렬 호출과 join 결과 병합 뼈대 작성 (전용 Executor Bean 포함)
- [ ] 공급사 실패(SupplierAdapterException) 발생 시 부분 실패로 기록하고 나머지 결과로 계속 진행하는 처리 작성

### 수행 내용
- `SupplierAMapper`, `SupplierBMapper`를 구현해 각 공급사 응답 DTO를 표준 모델(`CatalogEntry`, `StandardRoomOffer`)로 변환하는 로직을 작성했다.
- 공급사 A는 일자별 요금(숙박비와 세금)을 배열로 제공해 합산해서 총액을 만들고, 공급사 B는 이미 합산된 총액을 제공하므로 그대로 사용하도록 구현했다.
- 재고는 기간 중 최솟값을 노출하도록 두 Mapper에 동일하게 구현했다. Day 1에서 DESIGN.md 1번 항목에 정리한 결정을 실제 코드에 반영한 것이다.
- `SupplierAMapperTest`, `SupplierBMapperTest`를 작성해 요금 합산과 재고 최솟값 계산 로직을 검증했다.
- 커밋 규칙에 맞춰 Mapper와 테스트를 한 커밋으로, 스킬 파일 추가를 별도 커밋으로 나눠 정리했다.
- Day 2에서 결정한 대로 `SupplierErrorCode`(TEMPORARILY_UNAVAILABLE, TIMEOUT, UNKNOWN)와 `SupplierAdapterException`을 구현했다.
- `SupplierAAdapter`, `SupplierBAdapter`를 구현해 `WebClient`로 Mock Supplier A/B를 실제로 호출하고, 응답 DTO를 각 Mapper로 표준 모델로 변환하도록 연결했다.
- `WebClientConfig`를 작성해 공급사별 `WebClient` 빈에 연결/응답 타임아웃(임시값)을 적용했다.
- `SupplierAAdapterTest`, `SupplierBAdapterTest`를 추가해 성공, HTTP 실패, resultCode 실패 세 가지 경로를 실제 HTTP 요청으로 검증했다.
- 통합 검색 서비스(이슈 #5) 작업 전에, 정작 매핑 테이블(엔티티, 리포지토리, 채우는 로직)이 전혀 없다는 걸 발견해서 이슈 #14로 분리해 먼저 구현했다.
- `StayMapping`, `RoomTypeMapping` JPA 엔티티와 리포지토리를 작성하고, `MappingRefreshService`로 각 `SupplierAdapter`의 `fetchCatalog()`를 호출해 매핑을 채우도록 구현했다. 앱 기동 시 1회 실행되도록 `MappingStartupInitializer`(`ApplicationRunner`)를 붙였다.
- docker-compose로 전체 스택(Postgres, Mock Supplier, 본체 앱)을 직접 띄워서 매핑 테이블이 실제로 채워지는지 확인했다.

### 의사결정
#### [요금 합산 로직: 공통화하지 않고 Mapper 별로 처리]
공급사 A는 일자별 요금 배열을 제공해 합산이 필요하고, 공급사 B는 이미 합산된 총액을 제공한다. 두 응답 구조가 근본적으로 달라서, 공통 합산 로직으로 추상화하기보다 각 Mapper 가 자신의 응답 구조에 맞게 직접 계산하도록 두었다.
  - 검토했다 폐기한 대안: 공통 합산 유틸리티 메서드 추출. 공급사가 2개뿐이고 입력 구조(일자별 배열과 단일 총액)가 서로 달라서, 지금 억지로 공통화하면 분기 처리만 늘어날 것 같아 보류했다. 공급사가 추가되는 시점에 다시 검토하기로 했다.

#### [어댑터 실패 유형 구분: WebClientResponseException vs WebClientRequestException]
공급사로부터 응답은 받았지만 상태 코드가 실패인 경우(`WebClientResponseException`)와 응답 자체를 못 받은 경우(연결/응답 타임아웃 등, `WebClientRequestException`)를 구분해서 각각 `TEMPORARILY_UNAVAILABLE`과 `TIMEOUT`으로 매핑했다. 그 외 예외는 `UNKNOWN`으로 분류했다.
  - 검토했다 폐기한 대안: reactor-netty의 구체적 예외 타입(`ReadTimeoutException`, `ConnectTimeoutException` 등)을 직접 catch하는 방식. Spring이 이미 `WebClientRequestException`으로 감싸서 제공하므로, 더 하위 구현체에 직접 의존하지 않는 이 방식이 더 안정적이라고 판단해 폐기.

#### [어댑터 테스트에 Mock 라이브러리 대신 JDK 내장 HttpServer 사용]
실제 HTTP 요청/응답 경로를 검증하고 싶었지만, WireMock 같은 별도 테스트 의존성을 추가하는 대신 JDK에 내장된 `com.sun.net.httpserver.HttpServer`로 가짜 서버를 띄워 검증했다.
  - 검토했다 폐기한 대안: WireMock 도입. 테스트 몇 개를 위해 새 의존성을 추가하는 것보다, JDK 내장 기능으로 같은 검증이 충분히 가능하다고 판단해 보류했다.

#### [Lombok 도입]
지금까지는 순수 Java(record, 명시적 생성자)로만 작성해왔는데, JPA 엔티티에서 반복되는 protected 기본 생성자 작성을 줄이기 위해 Lombok을 새로 도입했다. `@NoArgsConstructor(access = AccessLevel.PROTECTED)`로 `StayMapping`, `RoomTypeMapping`의 기본 생성자를 대체했고, 앞으로 추가되는 엔티티에도 계속 적용하기로 했다.
  - 검토했다 폐기한 대안: 계속 순수 Java 유지. 엔티티가 지금은 2개뿐이라 새 의존성 없이도 충분하다고 볼 수 있지만, 앞으로 엔티티가 늘어날 걸 고려해 지금 도입하는 쪽을 택했다.

### 막힌 지점 / 시행착오
- 재고 최솟값 계산 로직이 두 Mapper 에 거의 같은 형태로 중복돼 있다(설명 주석까지 동일). 공통 메서드로 뽑을지 고민했지만, 각 Mapper 가 받는 컬렉션 타입(`AvailabilityItem.dailyRates()`와 `SearchItem.inventory()`)이 서로 달라 이를 묶으려면 별도 인터페이스가 필요했다. 
- 지금 시점에는 그 정도 추상화가 과하다고 판단해 중복을 그대로 남겼다. 하지만 추후 추상화 할 의향은 있다.
- `WebClient` 클래스가 `spring-boot-starter-webmvc`가 아니라 `spring-webflux` 모듈에 있다는 걸 컴파일 에러를 보고서야 알았다. `spring-boot-starter-webflux`를 추가했는데, MVC(`starter-webmvc`)와 함께 있어도 Spring Boot가 서블릿 방식으로 우선 동작한다는 걸 확인하고 진행했다.
- `src/test/resources/application.yml`이 메인 설정을 완전히 대체한다는 걸 알고 있었는데도 `supplier.*` 설정을 빠뜨려서, `AggregatorApplicationTests`가 플레이스홀더 해석 실패로 깨졌다. 테스트 설정에도 같은 supplier 설정을 추가해 해결했다.
- docker-compose로 전체 스택을 띄워 매핑 생성을 검증하던 중, 공급사 A만 매핑이 비어 있는 걸 발견했다. 로그를 보니 `SupplierAAdapter`가 응답 타임아웃으로 실패하고 있었다. 원인은 `mock-supplier` 서비스에 healthcheck가 없어서, `app`이 `postgres`처럼 healthy 상태를 기다리지 않고 컨테이너가 "시작됨(service_started)"인 것만 확인한 뒤 바로 요청을 보냈기 때문이었다. mock-supplier의 Spring Boot가 실제로 요청을 받을 준비가 되기 전에 첫 요청이 가서 응답 타임아웃(3000ms)을 넘겨버린 것. `postgres`와 같은 방식으로 `mock-supplier`에도 curl 기반 healthcheck를 추가하고, `app`의 `depends_on` 조건을 `service_healthy`로 바꿔서 해결했다. 앞서 "2000ms/3000ms는 미검증 값"이라고 문서에 적어둔 우려가 실제로 재현된 사례였는데, 다만 근본 원인은 타임아웃 수치 자체보다 컨테이너 기동 순서 설정 쪽이었다.

### AI 활용
#### 무엇을 물었는지
- 지금까지 진행 상황을 확인해달라고 물었다. 어떤 파일이 이미 커밋됐고 어떤 파일이 아직 안 됐는지, 테스트가 통과하는지를 점검해달라고 요청했다.
- 미커밋 상태인 Mapper/테스트 파일과 스킬 파일을 커밋 단위 규칙에 맞게 나눠서 커밋해달라고 요청했다.
- `SupplierErrorCode` enum에 원본 실패 코드(HTTP 400/503, 공급사 B의 E503 등)를 그대로 담는 int/문자열 code 필드가 필요하지 않은지 물어봤다.
- JPA 엔티티의 protected 기본 생성자를 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`로 대체할 수 있다는 걸 먼저 제안했다.

##### AI 답변을 어떻게 수용/수정/거부했는지
- git 상태와 테스트 실행 결과를 직접 눈으로 확인했고, 진행 상황 요약은 사실 확인 용도로만 참고했다.
- 커밋을 Mapper와 테스트, 스킬 파일로 나누는 제안은 이 저장소의 커밋 단위 규칙과 맞아서 그대로 받아들였고, 커밋 메시지 문구는 실제 작업 내용과 맞는지 직접 검토했다.
- JOURNEY 날짜가 실제 작업일과 어긋나 있는 걸 발견하고 직접 정정을 요청했다(2026-09-04에서 2026-09-05로).
- enum에는 표준 분류만 담고, 원본 코드 같은 개별 실패의 상세 정보는 예외 인스턴스의 message에 담아야 한다는 설명을 들었다. 그대로 받아들이기 전에, 나중에 API 응답에 errorCode를 노출할 때 Jackson이 enum을 문자열로 직렬화한다는 점도 직접 확인해 근거로 삼았고, `SupplierAdapterException`을 던질 때 message에 원본 resultCode/HTTP 상태를 포함시키는 방식으로 구현했다.
- Lombok 제안은 그대로 적용하지 않고, 프로젝트에 아직 없는 의존성이라는 점을 먼저 확인한 뒤 도입 여부를 직접 판단해서 결정했다. 적용 범위도 protected 기본 생성자로만 한정하고, 다른 곳(getter 등)까지 임의로 Lombok화하지 않았다.

### 참고 자료
- 없음 