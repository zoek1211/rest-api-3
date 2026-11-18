package com.back;

// BackApplication
// 앱의 시작점(entry point). `./gradlew bootRun`을 하면 이 클래스의 main()이 실행된다.
// 요청 흐름에서 보면: main() → 스프링 컨테이너 생성 → 내장 Tomcat(8080) 시작 →
// 이후 들어오는 HTTP 요청은 컨트롤러(@RestController)들로 전달된다.

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// @SpringBootApplication = 아래 세 가지를 합친 애너테이션
//   1) @SpringBootConfiguration : 이 클래스가 설정의 뿌리임을 표시 (테스트가 이걸 찾아 컨텍스트를 띄운다)
//   2) @EnableAutoConfiguration  : 클래스패스에 있는 라이브러리를 보고 알아서 설정 (H2 있으면 DataSource, webmvc 있으면 Tomcat 등)
//   3) @ComponentScan            : 이 클래스가 있는 패키지(com.back)와 그 하위 패키지에서 @Component, @Service 등을 찾아 Bean으로 등록
// 주의: 그래서 이 클래스는 반드시 최상위 패키지(com.back)에 둔다.
//       com.back 밖(예: com.other)에 만든 클래스는 스캔되지 않아 Bean으로 등록되지 않는다.
@SpringBootApplication
// @EnableJpaAuditing: JPA Auditing을 켠다. BaseEntity의 @CreatedDate·@LastModifiedDate가 자동으로 채워지게 한다.
// 주의: 이 애너테이션이 없으면 에러 없이 조용히 날짜가 null로 저장된다 (001 단계에서 테스트로 확인).
@EnableJpaAuditing
public class BackApplication {

    public static void main(String[] args) {
        // 스프링 애플리케이션 컨텍스트(Bean들이 사는 컨테이너)를 만들고 서버를 띄운다.
        SpringApplication.run(BackApplication.class, args);
    }
}
