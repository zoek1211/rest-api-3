# rest-api-3

글(Post)과 댓글(PostComment)을 다루는 작은 게시판 REST API를 **TDD로 한 단계씩** 만들어 보는 학습 프로젝트입니다.
각 단계는 git 태그(`000`, `001`, …)로 남아 있어서, 아래 목차의 링크로 그 시점의 코드를 바로 열어 볼 수 있습니다.

- 스택: Java 25, Spring Boot 4.1, Gradle(Kotlin DSL), Spring Web MVC, Spring Data JPA, Validation, Lombok, H2
- 용어: [GLOSSARY.md](GLOSSARY.md) / 설계 결정: [docs/adr/](docs/adr/)
- 레퍼런스: slog.gg "스프링부트, REST API" 강의 (2부 REST API by TDD)

## 실행 방법

JDK를 따로 설치하지 않아도 됩니다. 첫 빌드 때 Gradle이 JDK 25를 자동으로 내려받습니다.

```bash
# 테스트 실행 (test 프로필, H2 메모리 DB)
./gradlew test

# 앱 실행 (dev 프로필, H2 파일 DB ./db_dev) → http://localhost:8080
./gradlew bootRun
```

H2 콘솔: http://localhost:8080/h2-console
JDBC URL에 `jdbc:h2:./db_dev;MODE=MySQL`, User Name에 `sa`, 비밀번호는 비워 두고 접속합니다.

## 단계 목차

| 태그 | 내용 |
|---|---|
| [000](https://github.com/zoek1211/rest-api-3/tree/000) | 프로젝트 뼈대와 설정 문서 |
| [001](https://github.com/zoek1211/rest-api-3/tree/001) | 글·댓글 엔티티와 샘플 데이터 |
| [002](https://github.com/zoek1211/rest-api-3/tree/002) | 글 다건 조회 GET /api/v1/posts |
| [003](https://github.com/zoek1211/rest-api-3/tree/003) | 글 단건 조회 GET /api/v1/posts/{id} 와 404 처리 |

## 단계별 진행

### 000: 프로젝트 뼈대와 설정 문서  ·  [코드 보기](https://github.com/zoek1211/rest-api-3/tree/000)

**무엇을 했나**: Spring Initializr로 Spring Boot 4.1 + Gradle Kotlin DSL 프로젝트를 만들고, 개발용 H2 파일 DB(MySQL 모드, H2 콘솔)와 테스트용 H2 메모리 DB를 프로필로 나눠 설정했습니다. 컨텍스트가 뜨는지만 보는 `contextLoads` 테스트를 먼저 실패시키고(Application 클래스 없음) 통과시켰습니다.
**왜**: 이후 모든 엔드포인트 테스트가 "전체 컨텍스트 + test 프로필 + 깨끗한 메모리 DB" 위에서 돌기 때문에, 그 바닥이 먼저 확실히 동작해야 합니다. 개발 DB는 파일이라 재시작해도 데이터가 남고, 테스트 DB는 메모리라 매번 새로 시작합니다.
**핵심 코드**

```yaml
# src/main/resources/application.yml (dev, 기본 프로필)
spring:
  profiles:
    default: dev
  datasource:
    url: jdbc:h2:./db_dev;MODE=MySQL   # 파일 DB: 앱을 껐다 켜도 데이터 유지
  h2:
    console:
      enabled: true                    # /h2-console

# src/test/resources/application-test.yml (@ActiveProfiles("test"))
spring:
  datasource:
    url: jdbc:h2:mem:db_test;MODE=MySQL;DB_CLOSE_DELAY=-1   # 메모리 DB: 테스트마다 깨끗하게
  jpa:
    hibernate:
      ddl-auto: create-drop
```

**배운 점**: Gradle toolchain + foojay로 JDK 버전을 빌드에 고정하기 / Spring 프로필로 환경별 설정 덮어쓰기 / `@SpringBootTest`는 `@SpringBootConfiguration`을 패키지 위쪽으로 찾아 올라간다(그래서 Application 클래스는 최상위 패키지에)

### 001: 글·댓글 엔티티와 샘플 데이터  ·  [코드 보기](https://github.com/zoek1211/rest-api-3/tree/001)

**무엇을 했나**: 공통 부모 `BaseEntity`(id, 작성일, 수정일)와 글 `Post`, 댓글 `PostComment` 엔티티를 만들고, 앱이 시작될 때 글이 0개면 글 3개·댓글 5개(분배 3/2/0)를 넣는 `BaseInitData`를 추가했습니다. 아직 API가 없어서 `SampleDataTest`가 저장소로 개수와 분배, 날짜 자동 기록만 확인합니다(이후 API 테스트가 생기면 대체됩니다).
**왜**: 이후 모든 엔드포인트 테스트가 "1번 글에 댓글 3개" 같은 샘플 데이터를 전제로 쓰기 때문에 데이터의 바닥을 먼저 깝니다. 개발 DB는 파일이라 `count() > 0` 검사가 없으면 `bootRun`마다 데이터가 쌓입니다.
**핵심 코드**

```java
// Post.java: 연관관계의 주인은 외래 키(post_id)를 가진 PostComment.post. 이쪽은 mappedBy로 "거울"만 한다.
@OneToMany(mappedBy = "post", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
private List<PostComment> comments = new ArrayList<>();

// PostComment.java: 소속 글 필수
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(nullable = false)
private Post post;

// BaseInitData.java: 모든 프로필에서, 비어 있을 때만
@Override
@Transactional
public void run(ApplicationArguments args) {
    if (postRepository.count() > 0) return;
    Post post1 = postRepository.save(new Post("제목 1", "내용 1"));
    Post post2 = postRepository.save(new Post("제목 2", "내용 2"));
    postRepository.save(new Post("제목 3", "내용 3"));
    post1.addComment("댓글 1"); post1.addComment("댓글 2"); post1.addComment("댓글 3");
    post2.addComment("댓글 4"); post2.addComment("댓글 5"); // 커밋 때 cascade PERSIST로 INSERT
}
```

**배운 점**: 양방향 연관관계와 연관관계의 주인(`mappedBy`) / JPA Auditing은 `@EntityListeners` + `@EnableJpaAuditing` 둘 다 있어야 하고, 빠지면 에러 없이 null이 저장된다 / `ApplicationRunner`는 `@SpringBootTest` 컨텍스트에서도 실행된다

### 002: 글 다건 조회 GET /api/v1/posts  ·  [코드 보기](https://github.com/zoek1211/rest-api-3/tree/002)

**무엇을 했나**: 첫 MockMvc 테스트 `ApiV1PostControllerTest`를 먼저 써서 실패를 확인하고(컨트롤러 없음 → 컴파일 실패, 빈 컨트롤러 → 매핑 없음), `ApiV1PostController` → `PostService` → `PostRepository` 흐름과 응답용 `PostDto` record를 만들어 통과시켰습니다. 응답은 래퍼 없는 `PostDto` 배열이고 id 내림차순(3, 2, 1)입니다.
**왜**: 엔티티를 그대로 JSON으로 내보내면 `Post.comments ↔ PostComment.post` 순환 참조, 지연 로딩 예외, DB 구조 노출 문제가 생깁니다. 조회는 데이터 자체가 결과라 `RsData`로 감싸지 않습니다(ADR 0001).
**핵심 코드**

```java
// ApiV1PostController.java
@GetMapping
public List<PostDto> getItems() {
    return postService.findAllByOrderByIdDesc()
            .stream()
            .map(PostDto::new) // 엔티티 → DTO. 엔티티는 응답 밖으로 나가지 않는다.
            .toList();
}

// ApiV1PostControllerTest.java: 1. 요청 보내기 → 2. 결과 검증
ResultActions resultActions = mvc.perform(get("/api/v1/posts")).andDo(print());

resultActions
        .andExpect(handler().methodName("getItems"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3));
for (int i = 0; i < 3; i++) {
    long id = 3 - i; // 최신순: 3, 2, 1
    resultActions
            .andExpect(jsonPath("$[%d].id".formatted(i)).value(id))
            .andExpect(jsonPath("$[%d].createDate".formatted(i)).value(matchesPattern(DATE_TIME_PATTERN)))
            .andExpect(jsonPath("$[%d].title".formatted(i)).value("제목 " + id))
            .andExpect(jsonPath("$[%d].comments".formatted(i)).doesNotExist());
}
```

**배운 점**: MockMvc는 서버 없이 DispatcherServlet에 요청을 넣어 HTTP 경계 전체를 검증한다 / DTO(record)로 API 계약과 엔티티 구조를 분리한다 / 테스트의 `@Transactional`은 끝나면 롤백해 테스트끼리 DB를 공유해도 서로 영향이 없다

### 003: 글 단건 조회 GET /api/v1/posts/{id} 와 404 처리  ·  [코드 보기](https://github.com/zoek1211/rest-api-3/tree/003)

**무엇을 했나**: `GET /posts/1`(200, 모든 필드)과 `GET /posts/999`(404) 테스트를 먼저 써서 실패를 확인했습니다(매핑 없음 → `ResourceHttpRequestHandler`, 예외 처리기 없음 → `ServletException`으로 터짐). `getItem` + `PostService.findById`(`orElseThrow()`)로 성공 케이스를, 공통 응답 봉투 `RsData{resultCode, msg, data}`와 `GlobalExceptionHandler`로 404 케이스를 통과시켰습니다.
**왜**: 컨트롤러마다 try-catch로 404를 만들면 코드가 복사되고 응답 모양이 어긋나기 쉽습니다. 서비스는 예외만 던지고, 에러 응답은 `@RestControllerAdvice` 한 곳에서 같은 형식으로 만듭니다. 상태 코드는 `ResponseAspect`가 생기기 전까지 핸들러가 `ResponseEntity`로 직접 정합니다.
**핵심 코드**

```java
// PostService.java
public Post findById(long id) {
    return postRepository.findById(id).orElseThrow(); // 없으면 NoSuchElementException
}

// GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<RsData<Void>> handle(NoSuchElementException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new RsData<>("404-1", "해당 데이터가 존재하지 않습니다."));
    }
}

// ApiV1PostControllerTest.java: 상태와 resultCode를 둘 다 본다 (ADR 0001)
mvc.perform(get("/api/v1/posts/999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.resultCode").value("404-1"))
        .andExpect(jsonPath("$.data").value(nullValue()));
```

**배운 점**: `@PathVariable`로 URL 경로 값을 받는다 / `Optional.orElseThrow()`로 "없음"을 예외로 바꾸고 `@RestControllerAdvice` + `@ExceptionHandler`에서 한 번에 HTTP 응답으로 변환한다 / 처리기가 없으면 MockMvc에서는 예외가 `ServletException`으로 그대로 튀어나온다
