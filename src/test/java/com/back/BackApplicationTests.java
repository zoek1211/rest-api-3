package com.back;

// BackApplicationTests
// 이 프로젝트의 첫 번째 테스트. "스프링 애플리케이션이 에러 없이 뜨는가"만 확인한다.
// 아직 API가 하나도 없지만, 이 테스트가 통과한다는 건 다음이 모두 맞다는 뜻이다:
//   - 의존성(build.gradle.kts)이 서로 충돌하지 않는다
//   - application.yml + application-test.yml 설정이 올바르다
//   - DB(H2 메모리)에 연결되고 JPA가 초기화된다
// 이후 티켓의 테스트(ApiV1PostControllerTest 등)도 같은 방식으로 전체 컨텍스트를 띄운다.

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// @SpringBootTest: 실제 앱과 똑같이 스프링 컨텍스트(모든 Bean)를 통째로 띄워서 테스트한다.
//   다른 선택지인 @WebMvcTest, @DataJpaTest는 일부 Bean만 띄워 빠르지만, 이 프로젝트는
//   "HTTP 경계 하나에서 외부 동작만 검증한다"는 원칙이라 전체 컨텍스트를 쓴다 (spec #1 Testing Decisions).
@SpringBootTest
// @ActiveProfiles("test"): test 프로필을 켠다 → application-test.yml이 application.yml 위에 덮어써진다.
//   그래서 테스트는 개발용 파일 DB(./db_dev)가 아니라 매번 새로 만들어지는 메모리 DB를 쓴다.
// 주의: 이걸 빠뜨리면 테스트가 개발 DB 파일을 건드리게 되어, 개발 중 넣은 데이터에 따라 결과가 달라질 수 있다.
@ActiveProfiles("test")
class BackApplicationTests {

    // 본문이 비어 있어도 의미 있는 테스트다.
    // 컨텍스트를 띄우는 데 실패하면 테스트 메서드에 들어오기도 전에 실패로 처리된다.
    @Test
    void contextLoads() {
    }
}
