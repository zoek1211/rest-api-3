package com.back.global.globalExceptionHandler;

// GlobalExceptionHandlerTest
// "모든 에러 응답은 같은 RsData 모양 {resultCode, msg, data}"(spec #1 스토리 31)를 HTTP 경계에서 검증한다.
// 글·댓글 테스트에 이미 있는 404-1(없는 데이터), 400-1(검증 실패), 400-2(JSON 형식 오류) 말고,
//   Spring MVC가 컨트롤러에 닿기 전에 막는 에러들을 여기서 다룬다. 대표 요청 하나씩이다. (012)
//
// 왜 컨트롤러별 테스트가 아니라 따로 두나: 이 에러들은 특정 API의 규칙이 아니라 "서버 전체의 약속"이다.
//   없는 URL은 어느 컨트롤러에도 속하지 않는다.

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static com.back.global.test.ApiTestSupport.rsDataError;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("경로 변수 타입 오류: /posts/abc는 400 \"400-3\" RsData")
    void pathVariableTypeMismatch() throws Exception {
        // {id}는 long인데 "abc"는 숫자로 바꿀 수 없다. 컨트롤러 메서드가 호출되기 전, 인자 변환 단계에서 실패한다.
        mvc
                .perform(get("/api/v1/posts/abc"))
                .andDo(print())
                .andExpect(rsDataError(400, "400-3", "요청 값의 형식이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("없는 URL: 404 \"404-2\" RsData (없는 데이터 404-1과 구분)")
    void noSuchUrl() throws Exception {
        // 매핑된 컨트롤러가 없는 주소다. "주소는 맞는데 데이터가 없음"(404-1)과 달리 "주소 자체가 없음"이다.
        mvc
                .perform(get("/api/v1/no-such-url"))
                .andDo(print())
                .andExpect(rsDataError(404, "404-2", "요청한 주소가 존재하지 않습니다."));
    }

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드: PATCH /posts/1은 405 \"405-1\" RsData")
    void methodNotSupported() throws Exception {
        // /api/v1/posts/{id}에는 GET·PUT·DELETE만 있다. 주소는 맞지만 메서드(PATCH)가 없다 → 404가 아니라 405.
        mvc
                .perform(patch("/api/v1/posts/1"))
                .andDo(print())
                .andExpect(rsDataError(405, "405-1", "지원하지 않는 HTTP 메서드입니다."))
                // 405 응답은 "그럼 무엇이 되는지"를 Allow 헤더로 알려 주는 것이 HTTP 규칙이다(RFC 9110).
                .andExpect(header().string("Allow", containsString("GET")));
    }

    @Test
    @DisplayName("지원하지 않는 Content-Type: 글 작성에 text/plain을 보내면 415 \"415-1\" RsData")
    void mediaTypeNotSupported() throws Exception {
        // @RequestBody는 JSON(application/json)만 읽는다. 본문 내용이 맞아도 Content-Type이 다르면 읽기 전에 거절한다.
        mvc
                .perform(
                        post("/api/v1/posts")
                                .contentType(MediaType.TEXT_PLAIN)
                                .content("제목: 새 제목")
                )
                .andDo(print())
                .andExpect(rsDataError(415, "415-1", "지원하지 않는 Content-Type입니다."));
    }
}
