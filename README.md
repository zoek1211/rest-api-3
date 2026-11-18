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
