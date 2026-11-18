package com.back.global.test;

// ApiTestSupport
// 여러 API 테스트 클래스가 함께 쓰는 상수와 검증 묶음. (012 코드 리뷰에서 중복을 모았다)
// 테스트 코드 전용이라 src/test 아래에 둔다. 운영 코드는 이 클래스를 모른다.
//
// 왜 상속(부모 테스트 클래스)이 아니라 static 멤버인가:
//   부모 클래스를 두면 @SpringBootTest 같은 설정까지 숨어 버려, 테스트 파일만 읽고는 무엇이 켜져 있는지 알기 어렵다.
//   static import로 가져다 쓰면 각 테스트 클래스의 설정은 그대로 보이고, 공통 "검증"만 빌려 쓴다.

import org.springframework.test.web.servlet.ResultMatcher;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class ApiTestSupport {

    private ApiTestSupport() {
        // 상수·static 메서드만 담는 클래스라 객체를 만들 일이 없다.
    }

    // LocalDateTime이 JSON 문자열로 바뀐 모양: 2026-11-18T10:15:30 (+ 소수점 이하 초는 있을 수도 없을 수도)
    // 날짜는 실행할 때마다 값이 달라서, 정확한 값 대신 "존재 + ISO-8601 형식"만 확인한다.
    public static final String DATE_TIME_PATTERN = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?$";

    // "없는 id": 글·댓글 테스트 모두 이 값 하나를 쓴다.
    // 왜 999가 아니라 Integer.MAX_VALUE인가: id는 auto_increment라 테스트에서 글·댓글을 계속 쓰면 언젠가 999에 닿을 수 있다.
    //   int 최댓값은 샘플 규모에서 절대 생기지 않는 번호라 "없음"이 확실하다.
    public static final long NOT_EXISTING_ID = Integer.MAX_VALUE;

    // 에러 응답 검증 묶음: HTTP 상태 + RsData 세 키(resultCode, msg, data=null).
    // ADR 0001 Consequences: 상태와 resultCode를 둘 다 보고 서로 맞는지 확인한다.
    // ResultMatcher는 "응답 하나를 받아 검증하는 함수"(함수형 인터페이스)라, 람다로 여러 검증을 하나로 묶을 수 있다.
    //   그러면 andExpect(rsDataError(...)) 한 번으로 다섯 가지를 모두 확인한다.
    // 주의: 예전 Spring의 ResultMatcher.matchAll(...)은 Spring Framework 7에서 사라졌다. 그래서 직접 묶는다.
    // 주의: jsonPath("$.data").doesNotExist()는 "키가 아예 없음"과 "값이 null"을 구분하지 않는다.
    //   spec은 "data: null"(키는 있고 값이 null)이므로 value(nullValue())로 값을 보고,
    //   length() == 3으로 키 3개가 모두(그리고 그것만) 있는지 확인한다.
    public static ResultMatcher rsDataError(int httpStatus, String resultCode, String msg) {
        ResultMatcher[] matchers = {
                status().is(httpStatus),
                jsonPath("$.resultCode").value(resultCode),
                jsonPath("$.msg").value(msg),
                jsonPath("$.data").value(nullValue()),
                jsonPath("$.length()").value(3)
        };

        return result -> {
            for (ResultMatcher matcher : matchers) {
                matcher.match(result);
            }
        };
    }

    // 가장 흔한 에러 두 가지는 이름을 붙여 둔다. 없는 데이터(404-1)와 입력값 검증 실패(400-1).
    public static ResultMatcher notFound() {
        return rsDataError(404, "404-1", "해당 데이터가 존재하지 않습니다.");
    }

    public static ResultMatcher validationFailed(String msg) {
        return rsDataError(400, "400-1", msg);
    }
}
