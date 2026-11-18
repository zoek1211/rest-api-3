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
| [004](https://github.com/zoek1211/rest-api-3/tree/004) | 글 작성 POST /api/v1/posts 와 입력값 검증 |
| [005](https://github.com/zoek1211/rest-api-3/tree/005) | 글 수정 PUT /api/v1/posts/{id} |
| [006](https://github.com/zoek1211/rest-api-3/tree/006) | 글 삭제 DELETE /api/v1/posts/{id} |
| [007](https://github.com/zoek1211/rest-api-3/tree/007) | 댓글 다건 조회 GET /api/v1/posts/{postId}/comments |
| [008](https://github.com/zoek1211/rest-api-3/tree/008) | 댓글 단건 조회 GET /api/v1/posts/{postId}/comments/{id} |
| [009](https://github.com/zoek1211/rest-api-3/tree/009) | 댓글 작성 POST /api/v1/posts/{postId}/comments |
| [010](https://github.com/zoek1211/rest-api-3/tree/010) | 댓글 수정 PUT /api/v1/posts/{postId}/comments/{id} |

## 단계별 진행

### 000: 프로젝트 뼈대와 설정 문서  ·  [코드 보기](https://github.com/zoek1211/rest-api-3/commit/000)

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

### 001: 글·댓글 엔티티와 샘플 데이터  ·  [코드 보기](https://github.com/zoek1211/rest-api-3/commit/001)

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

### 002: 글 다건 조회 GET /api/v1/posts  ·  [코드 보기](https://github.com/zoek1211/rest-api-3/commit/002)

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

### 003: 글 단건 조회 GET /api/v1/posts/{id} 와 404 처리  ·  [코드 보기](https://github.com/zoek1211/rest-api-3/commit/003)

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

### 004: 글 작성 POST /api/v1/posts 와 입력값 검증  ·  [코드 보기](https://github.com/zoek1211/rest-api-3/commit/004)

**무엇을 했나**: 정상 작성(201, `"201-1"`, `"4번 글이 작성되었습니다."`, data 전체 필드, 다시 다건 조회하면 4개), 빈 제목·빈 본문(400 `"400-1"`), 깨진 JSON(400 `"400-2"`) 테스트를 하나씩 먼저 써서 실패를 확인하고 통과시켰습니다. 요청 DTO `PostWriteReqBody`(`@NotBlank`, `@Size`)와 `@Valid`로 입력을 검증하고, `ResponseAspect`가 `RsData.resultCode` 앞자리로 HTTP 상태를 정하게 해서 404 처리기도 `ResponseEntity` 없이 `RsData`만 반환하도록 정리했습니다.
**왜**: 상태 코드를 컨트롤러마다 `ResponseEntity`로 적으면 `resultCode`와 실제 상태가 두 군데에서 따로 관리되어 어긋날 수 있습니다(ADR 0001). AOP로 한 곳에서 정하면 컨트롤러는 "무슨 결과인지"만 말합니다. 검증·JSON 오류는 컨트롤러 본문까지 오지 않고 예외로 끝나므로 전역 처리기에서 400으로 바꿉니다.
**핵심 코드**

```java
// ResponseAspect.java: RsData를 반환하는 컨트롤러·예외 처리기 메서드를 감싼다
@Around("execution(public com.back.global.rsData.RsData *(..)) && (within(@...RestController *) || within(@...RestControllerAdvice *))")
public Object setStatusFromResultCode(ProceedingJoinPoint joinPoint) throws Throwable {
    Object result = joinPoint.proceed();
    if (result instanceof RsData<?> rsData) {
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getResponse();
        response.setStatus(rsData.statusCode()); // "201-1" → 201
    }
    return result;
}

// ApiV1PostController.java
@PostMapping
public RsData<PostDto> write(@RequestBody @Valid PostWriteReqBody reqBody) {
    Post post = postService.write(reqBody.title(), reqBody.content());
    return new RsData<>("201-1", "%d번 글이 작성되었습니다.".formatted(post.getId()), new PostDto(post));
}

// GlobalExceptionHandler.java: "필드명-규칙-메시지" 줄을 정렬해 잇는다
String msg = e.getBindingResult().getFieldErrors().stream()
        .map(fe -> fe.getField() + "-" + fe.getCode() + "-" + fe.getDefaultMessage())
        .sorted()
        .collect(Collectors.joining("\n"));
return new RsData<>("400-1", msg);
```

**배운 점**: AOP(`@Aspect` + `@Around`)는 Bean을 프록시로 감싸 여러 메서드에 공통 동작을 끼워 넣는다(Boot 4 스타터 이름은 `spring-boot-starter-aspectj`) / `@Valid`가 있어야 요청 DTO의 검증 애너테이션이 동작하고, 실패하면 `MethodArgumentNotValidException`이 난다 / JSON 변환 실패(`HttpMessageNotReadableException`)는 검증보다 앞 단계라 코드를 나눠(400-2) 구분한다

### 005: 글 수정 PUT /api/v1/posts/{id}  ·  [코드 보기](https://github.com/zoek1211/rest-api-3/commit/005)

**무엇을 했나**: 정상 수정(200, `"200-1"`, `"1번 글이 수정되었습니다."`, `data: null`, 다시 단건 조회하면 제목·본문이 바뀌고 `modifyDate`가 수정 전과 다름) 테스트를 먼저 써서 실패(핸들러 없음)를 확인하고 통과시켰습니다. 없는 글(404 `"404-1"`)과 빈 제목(400 `"400-1"`, 글은 그대로) 테스트도 추가했는데, 기존 `GlobalExceptionHandler`가 이미 처리해 처음부터 통과했습니다. 요청 DTO는 작성과 같은 규칙의 `PostModifyReqBody`를 따로 두었습니다.
**왜**: 수정은 `save()` 없이 엔티티 값만 바꾸면 트랜잭션이 끝날 때 JPA가 변경을 감지(dirty checking)해 UPDATE를 보냅니다. 다만 테스트처럼 바깥 트랜잭션에 묶여 커밋이 미뤄지면 `@LastModifiedDate`도 갱신되지 않고, 같은 트랜잭션의 단건 조회는 1차 캐시의 옛 값을 돌려줍니다. 그래서 서비스에서 `flush()`로 UPDATE를 바로 실행해 "수정 API가 끝나면 수정일이 이미 갱신되어 있다"를 보장했습니다.
**핵심 코드**

```java
// PostService.java
@Transactional
public void modify(long id, String title, String content) {
    Post post = postRepository.findById(id).orElseThrow(); // 없으면 404

    post.modify(title, content); // 값만 바꾼다. save() 호출 없음 = 변경 감지(dirty checking)

    postRepository.flush(); // UPDATE + @LastModifiedDate 갱신을 지금 실행(커밋은 아님)
}

// ApiV1PostController.java
@PutMapping("/{id}")
public RsData<Void> modify(@PathVariable long id, @RequestBody @Valid PostModifyReqBody reqBody) {
    postService.modify(id, reqBody.title(), reqBody.content());
    return new RsData<>("200-1", "%d번 글이 수정되었습니다.".formatted(id)); // data = null
}
```

**배운 점**: 영속성 컨텍스트에 있는 엔티티는 값만 바꾸면 flush 시점에 스냅샷과 비교해 UPDATE가 자동으로 나간다(dirty checking, `readOnly` 트랜잭션에서는 안 된다) / `@LastModifiedDate`는 flush 때 채워지므로 커밋이 미뤄지면 응답에 옛 수정일이 보일 수 있다 / PUT은 자원 전체를 바꾸는 요청이라 모든 필드가 필수다

### 006: 글 삭제 DELETE /api/v1/posts/{id}  ·  [코드 보기](https://github.com/zoek1211/rest-api-3/commit/006)

**무엇을 했나**: 댓글 3개가 달린 1번 글을 삭제하면 200(`"200-1"`, `"1번 글이 삭제되었습니다."`, `data: null`)이고 이후 단건 조회는 404라는 테스트와, 없는 글 삭제는 404라는 테스트를 먼저 써서 실패(핸들러 없음)를 확인하고 통과시켰습니다. 서비스는 글을 찾은 뒤(`orElseThrow`, 없으면 404) `delete` + `flush()`합니다.
**왜**: 댓글은 `Post.comments`의 cascade REMOVE가 함께 지우므로 따로 지우지 않습니다(SQL 로그에 `post_comment` DELETE 3번 → `post` DELETE 1번). `flush()`가 없으면 테스트의 바깥 트랜잭션 때문에 DELETE SQL이 롤백 직전까지 안 나가, 외래 키 위반 같은 DB 오류가 테스트에서 숨겨집니다. 응답은 204 대신 ADR 0001대로 RsData 본문이 있는 200으로 통일했습니다.
**핵심 코드**

```java
// PostService.java
@Transactional
public void delete(long id) {
    Post post = postRepository.findById(id).orElseThrow(); // 없으면 404

    postRepository.delete(post); // 댓글은 cascade REMOVE로 먼저 함께 삭제된다

    postRepository.flush(); // DELETE SQL을 지금 실행해 FK 오류 등이 바로 드러나게 한다
}

// ApiV1PostController.java
@DeleteMapping("/{id}")
public RsData<Void> delete(@PathVariable long id) {
    postService.delete(id);
    return new RsData<>("200-1", "%d번 글이 삭제되었습니다.".formatted(id)); // data = null
}
```

**배운 점**: `cascade = REMOVE`가 있으면 부모를 지울 때 JPA가 자식 DELETE를 먼저 보낸다(없으면 FK 제약 위반) / 쓰기 결과는 DB를 직접 보지 않고 같은 API(단건 조회 404)로 확인한다 / DELETE는 멱등이다: 두 번째 요청은 404지만 "글이 없다"는 서버 상태는 같다

### 007: 댓글 다건 조회 GET /api/v1/posts/{postId}/comments  ·  [코드 보기](https://github.com/zoek1211/rest-api-3/commit/007)

**무엇을 했나**: 두 번째 테스트 클래스 `ApiV1PostCommentControllerTest`를 만들고, 1번 글의 댓글 3개가 id 내림차순으로 모든 필드(`postId`는 1)와 함께 온다는 테스트를 먼저 써서 실패(빈 컨트롤러라 핸들러가 `ResourceHttpRequestHandler`)를 확인한 뒤 `PostCommentDto`, `ApiV1PostCommentController`, `PostService.findCommentsByPostIdOrderByIdDesc`로 통과시켰습니다. 댓글 없는 3번 글은 빈 배열, 없는 글은 404라는 테스트도 추가했습니다(구현이 이미 처리해서 바로 통과).
**왜**: 댓글은 글의 하위 자원이라 URL을 `/posts/{postId}/comments`로 중첩하고, 글을 먼저 찾아(`orElseThrow`, 없으면 404) 그 글의 댓글을 꺼냅니다. `open-in-view: false`라서 LAZY인 `post.comments`는 서비스 트랜잭션 안에서 `toList()`로 읽어 둬야 하고, `@OrderBy`가 없어 순서는 직접 id 내림차순으로 정렬합니다. 응답에는 `Post` 객체 대신 `postId` 숫자만 넣어 순환 참조를 끊습니다.
**핵심 코드**

```java
// PostService.java
@Transactional(readOnly = true)
public List<PostComment> findCommentsByPostIdOrderByIdDesc(long postId) {
    Post post = postRepository.findById(postId).orElseThrow(); // 없는 글이면 404, 댓글 0개면 빈 목록

    return post.getComments()                                  // LAZY 컬렉션: 트랜잭션 안에서 읽어 둔다
            .stream()
            .sorted(Comparator.comparing(PostComment::getId).reversed()) // 최신순
            .toList();
}

// ApiV1PostCommentController.java
@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
public class ApiV1PostCommentController {
    @GetMapping
    public List<PostCommentDto> getItems(@PathVariable long postId) {
        return postService.findCommentsByPostIdOrderByIdDesc(postId)
                .stream().map(PostCommentDto::new).toList();
    }
}
```

**배운 점**: 소속이 필수인 자원은 경로를 중첩해 표현한다(`/posts/{postId}/comments`) / "없는 글"(404)과 "댓글 없는 글"(200 + `[]`)은 다르다 / LAZY 연관의 프록시는 `getId()`만으로는 SELECT를 하지 않지만, 컬렉션은 트랜잭션 밖에서 처음 읽으면 `LazyInitializationException`이 난다

### 008: 댓글 단건 조회 GET /api/v1/posts/{postId}/comments/{id}  ·  [코드 보기](https://github.com/zoek1211/rest-api-3/commit/008)

**무엇을 했나**: `/posts/1/comments/1`이 200과 모든 필드(id, 날짜 2개, `postId`=1, `content`="댓글 1", 필드 정확히 5개)를 돌려준다는 테스트를 먼저 써서 실패(매핑이 없어 핸들러가 `ResourceHttpRequestHandler`)를 확인한 뒤, `ApiV1PostCommentController.getItem`과 `PostService.findCommentById`로 통과시켰습니다. 없는 댓글, 다른 글의 댓글(`/posts/1/comments/4`), 없는 글이 모두 404라는 테스트도 추가했습니다(구현이 이미 처리해서 바로 통과).
**왜**: 댓글을 id만으로 찾으면 2번 글의 4번 댓글이 `/posts/1/comments/4`로 보여 버립니다. 글을 먼저 찾고 **그 글의 댓글 중에서** 찾으면 소속 검사를 따로 쓸 필요가 없어 빠뜨릴 수도 없습니다. 세 경우 모두 클라이언트에게는 "이 주소에 댓글이 없다"이므로 같은 404(`404-1`)로 답합니다.
**핵심 코드**

```java
// PostService.java
@Transactional(readOnly = true)
public PostComment findCommentById(long postId, long id) {
    Post post = postRepository.findById(postId).orElseThrow(); // 없는 글 → 404

    return post.getComments()                  // 이 글의 댓글 중에서만 찾는다
            .stream()
            .filter(comment -> comment.getId() == id)
            .findFirst()
            .orElseThrow();                    // 없는 댓글, 다른 글의 댓글 → 404
}

// ApiV1PostCommentController.java
@GetMapping("/{id}") // 클래스의 /api/v1/posts/{postId}/comments 뒤에 붙는다
public PostCommentDto getItem(@PathVariable long postId, @PathVariable long id) {
    return new PostCommentDto(postService.findCommentById(postId, id));
}
```

**배운 점**: 중첩 자원의 단건 조회는 "부모에 속하는지"까지 확인해야 한다 / 부모에서 자식을 꺼내는 구조로 만들면 소속 검사가 저절로 된다 / 클래스 수준 경로 변수와 메서드 수준 경로 변수를 함께 `@PathVariable`로 받는다

### 009: 댓글 작성 POST /api/v1/posts/{postId}/comments  ·  [코드 보기](https://github.com/zoek1211/rest-api-3/commit/009)

**무엇을 했나**: 1번 글에 댓글을 쓰면 201·`"201-1"`·`"N번 댓글이 작성되었습니다."`와 새 댓글(`PostCommentDto`, 필드 5개)이 오고, 다시 조회하면 1번 글의 댓글이 4개라는 테스트를 먼저 써서 실패(핸들러 없음)를 확인한 뒤, `PostCommentWriteReqBody`(본문 2–100자, `@NotBlank`), `PostService.writeComment`, `ApiV1PostCommentController.write`로 통과시켰습니다. 빈 본문 400(`"400-1"`)과 없는 글 404 테스트도 함께 통과합니다.
**왜**: 댓글은 `post.addComment()`로 글의 컬렉션에 넣기만 하면 cascade PERSIST로 저장되지만, INSERT는 flush 때(보통 커밋 직전) 나갑니다. id는 IDENTITY(auto_increment)라 INSERT 전에는 `null`이어서, 응답에 id를 담으려면 서비스에서 `flush()`로 INSERT를 앞당겨야 합니다(레퍼런스 056강의 flush 이슈). 실제로 `flush()`를 빼면 테스트가 `$.data.id`가 `null`이라며 실패합니다.
**핵심 코드**

```java
// PostService.java
@Transactional
public PostComment writeComment(long postId, String content) {
    Post post = postRepository.findById(postId).orElseThrow(); // 없는 글 → 404

    PostComment comment = post.addComment(content); // cascade PERSIST: save() 없이 flush 때 INSERT
    // 아직 comment.getId() == null. INSERT가 나가야 IDENTITY id가 정해진다.
    postRepository.flush();                         // 지금 INSERT → id·생성일 채워짐

    return comment;
}

// ApiV1PostCommentController.java
@PostMapping
public RsData<PostCommentDto> write(@PathVariable long postId,
                                    @RequestBody @Valid PostCommentWriteReqBody reqBody) {
    PostComment comment = postService.writeComment(postId, reqBody.content());
    return new RsData<>("201-1", "%d번 댓글이 작성되었습니다.".formatted(comment.getId()), new PostCommentDto(comment));
}
```

**배운 점**: cascade PERSIST로 저장되는 엔티티는 flush 전까지 INSERT되지 않는다 / IDENTITY 전략의 id는 INSERT 직후에야 생긴다 → 응답에 id가 필요하면 `flush()` / 하위 자원의 소속(`postId`)은 본문이 아니라 경로로 받는다

### 010: 댓글 수정 PUT /api/v1/posts/{postId}/comments/{id}  ·  [코드 보기](https://github.com/zoek1211/rest-api-3/commit/010)

**무엇을 했나**: 1번 글의 1번 댓글을 수정하면 200·`"200-1"`·`"1번 댓글이 수정되었습니다."`·`data: null`이 오고, 다시 단건 조회하면 바뀐 본문과 갱신된 `modifyDate`가 보인다는 테스트를 먼저 써서 실패(핸들러 없음)를 확인한 뒤, `PostCommentModifyReqBody`, `PostComment.modify`, `PostService.modifyComment`, `ApiV1PostCommentController.modify`로 통과시켰습니다. 빈 본문 400(`"400-1"`)과 다른 글의 댓글 404(그 댓글은 그대로) 테스트도 추가했습니다.
**왜**: 댓글 찾기는 단건 조회의 `findCommentById`(글의 댓글 목록 안에서 찾기)를 재사용해 소속 검사를 따로 쓰지 않아도 되게 했습니다. 저장은 글 수정과 같이 변경 감지에 맡기고, `@Transactional` 테스트에서도 `modifyDate`가 갱신되어 보이도록 `flush()`로 UPDATE를 앞당깁니다.
**핵심 코드**

```java
// PostService.java
@Transactional
public void modifyComment(long postId, long id, String content) {
    // 글의 댓글 중에서 찾는다 → 없는 글·없는 댓글·다른 글의 댓글 모두 404
    PostComment comment = findCommentById(postId, id);

    comment.modify(content);   // 값만 바꾼다. save() 없이 변경 감지가 UPDATE
    postRepository.flush();    // 지금 UPDATE → @LastModifiedDate 갱신
}

// ApiV1PostCommentController.java
@PutMapping("/{id}")
public RsData<Void> modify(@PathVariable long postId, @PathVariable long id,
                           @RequestBody @Valid PostCommentModifyReqBody reqBody) {
    postService.modifyComment(postId, id, reqBody.content());
    return new RsData<>("200-1", "%d번 댓글이 수정되었습니다.".formatted(id));
}
```

**배운 점**: 변경 감지는 cascade 대상인 하위 엔티티(댓글)에도 똑같이 동작한다 / 같은 클래스 안의 `this.method()` 호출은 프록시를 거치지 않아 그 메서드의 `@Transactional(readOnly = true)`가 적용되지 않는다 / 작성과 수정은 규칙이 같아도 요청 DTO를 나눈다
