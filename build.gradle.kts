// build.gradle.kts
// 이 프로젝트를 "어떻게 빌드할지" 정하는 Gradle 설정 파일이다 (Kotlin DSL 문법).
// - 어떤 플러그인을 쓰는지 (Java, Spring Boot)
// - 어떤 Java 버전으로 컴파일하는지
// - 어떤 라이브러리(의존성)를 가져오는지
// `./gradlew test`, `./gradlew bootRun` 같은 명령은 모두 이 파일을 읽고 동작한다.
//
// 왜 Gradle Kotlin DSL인가: Groovy DSL(build.gradle)보다 IDE 자동완성과 타입 검사가 잘 된다.
// Maven(pom.xml)도 선택지였지만 XML이 길어지고, Spring Initializr의 기본값도 Gradle이다.

plugins {
    // 순수 Java 프로젝트를 컴파일·테스트하는 기본 플러그인
    java
    // Spring Boot 플러그인: bootRun(앱 실행), bootJar(실행 가능한 jar 만들기) 태스크를 추가한다.
    id("org.springframework.boot") version "4.1.2"
    // 의존성 버전 관리 플러그인: Spring Boot가 검증한 라이브러리 버전 목록(BOM)을 적용해 준다.
    // 그래서 아래 dependencies에서 대부분 버전 번호를 적지 않아도 된다.
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        // toolchain: "이 프로젝트는 Java 25로 컴파일·실행한다"고 고정하는 설정.
        // Gradle 자체를 돌리는 JDK(여기선 27)와 별개로, 컴파일·테스트·bootRun은 JDK 25로 한다.
        // JDK 25가 없으면 settings.gradle.kts의 foojay 플러그인이 자동으로 내려받는다.
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    // 라이브러리를 내려받을 저장소. 대부분의 Java 라이브러리가 Maven Central에 있다.
    mavenCentral()
}

dependencies {
    // ── 의존성 종류(configuration) 읽는 법 ──
    // implementation      : 컴파일·실행 모두 필요
    // compileOnly         : 컴파일 때만 필요 (Lombok처럼 코드를 만들어 주고 사라지는 것)
    // runtimeOnly         : 실행 때만 필요 (H2 드라이버처럼 코드에서 직접 import하지 않는 것)
    // developmentOnly     : bootRun으로 개발할 때만. 배포용 jar에는 들어가지 않는다.
    // annotationProcessor : 컴파일 중에 애너테이션을 읽어 코드를 생성하는 도구
    // testXxx             : 테스트 코드에만 적용

    // Spring Web MVC: @RestController, 내장 Tomcat, JSON 변환(Jackson)을 제공한다.
    // 주의: Spring Boot 4부터 이름이 spring-boot-starter-web → spring-boot-starter-webmvc로 바뀌었다.
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    // Spring Data JPA: 자바 객체(엔티티)를 DB 테이블에 매핑(ORM)하고, Repository 인터페이스만 만들면 CRUD를 해 준다.
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // Validation: @NotBlank, @Size 같은 애너테이션으로 요청 값을 검증한다 (Bean Validation, Hibernate Validator).
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // AOP(AspectJ 애너테이션 방식): @Aspect, @Around로 ResponseAspect를 만든다.
    // 주의: Spring Boot 4부터 이름이 spring-boot-starter-aop → spring-boot-starter-aspectj로 바뀌었다.
    //   aspectjweaver는 data-jpa를 통해 이미 딸려 오지만, 우리 코드가 직접 쓰는 기능이라 명시적으로 선언한다.
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    // H2 콘솔: 브라우저에서 /h2-console로 DB 내용을 볼 수 있게 해 준다.
    // 주의: Boot 4에서는 H2 콘솔 자동 설정이 별도 모듈로 분리되어, 이 의존성이 없으면 콘솔이 안 뜬다.
    implementation("org.springframework.boot:spring-boot-h2console")

    // Lombok: @Getter, @NoArgsConstructor 등으로 반복 코드(getter, 생성자)를 컴파일 시점에 만들어 준다.
    // compileOnly + annotationProcessor 두 줄이 한 쌍이다. 하나만 있으면 동작하지 않는다.
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // devtools: 코드가 바뀌면 앱을 자동 재시작해 준다. 개발 편의용이라 developmentOnly.
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // H2: 자바로 만든 가벼운 DB. 별도 설치 없이 파일 또는 메모리에 DB를 만든다.
    // 코드에서 직접 쓰지 않고 JDBC 드라이버로만 쓰이므로 runtimeOnly.
    runtimeOnly("com.h2database:h2")

    // ── 테스트 ──
    // Boot 4부터는 기술별 테스트 스타터가 따로 있다 (예전엔 spring-boot-starter-test 하나).
    // webmvc-test 안에 JUnit 5, AssertJ, MockMvc, @SpringBootTest 등이 다 들어 있다.
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    // 테스트 코드에서도 Lombok을 쓸 수 있게
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    // Gradle이 JUnit 5 테스트를 찾아 실행할 때 필요한 런처
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    // JUnit 5(Jupiter)로 테스트를 실행한다. 이 줄이 없으면 Gradle은 JUnit 4 방식으로 찾아서 테스트 0개가 된다.
    useJUnitPlatform()
}
