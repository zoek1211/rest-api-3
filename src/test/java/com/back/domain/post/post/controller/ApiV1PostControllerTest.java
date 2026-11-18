package com.back.domain.post.post.controller;

// ApiV1PostControllerTest
// 글 API(/api/v1/posts)를 HTTP 경계에서만 검증하는 테스트. (spec #1 Testing Decisions)
//
// 요청 흐름 전체(컨트롤러 → 서비스 → 저장소 → DB)를 실제로 태우고, "응답"만 본다.
// 서비스나 저장소를 따로 테스트하거나 Mockito로 가짜를 끼우지 않는 이유:
//   내부 구조를 바꿔도(리팩터링) 외부 동작이 같으면 테스트가 깨지지 않아야 하기 때문이다.
//   테스트가 내부 구현에 묶이면, 코드를 고칠 때마다 테스트도 같이 고쳐야 하는 짐이 된다.
//
// MockMvc: 실제 톰캣 서버를 띄우지 않고, 가짜 HTTP 요청을 DispatcherServlet에 직접 넣어 주는 도구.
//   네트워크를 타지 않아 빠르지만, 필터·컨트롤러·JSON 변환·예외 처리는 실제와 똑같이 거친다.

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
// 주의: Boot 4에서 @AutoConfigureMockMvc의 패키지가 바뀌었다.
//   (예전: org.springframework.boot.test.autoconfigure.web.servlet)
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @SpringBootTest: 전체 Spring 컨텍스트(모든 Bean)를 띄운다. 샘플 데이터(BaseInitData)도 이때 들어간다.
@SpringBootTest
// @AutoConfigureMockMvc: MockMvc Bean을 만들어 주입받을 수 있게 한다.
@AutoConfigureMockMvc
// test 프로필 = H2 메모리 DB (src/test/resources/application-test.yml)
@ActiveProfiles("test")
// @Transactional: 테스트마다 트랜잭션을 열고 끝나면 롤백한다.
//   그래서 한 테스트가 글을 지우거나 고쳐도 다음 테스트는 항상 샘플 데이터 그대로에서 시작한다.
@Transactional
class ApiV1PostControllerTest {

    // LocalDateTime이 JSON 문자열로 바뀐 모양: 2026-11-18T10:15:30 (+ 소수점 이하 초는 있을 수도 없을 수도)
    // 날짜는 실행할 때마다 값이 달라서, 정확한 값 대신 "존재 + ISO-8601 형식"만 확인한다.
    private static final String DATE_TIME_PATTERN = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?$";

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("글 다건 조회: 샘플 글 3개가 id 내림차순(최신순)으로 온다")
    void getItems() throws Exception {
        // 1. 요청 보내기
        ResultActions resultActions = mvc
                .perform(get("/api/v1/posts"))
                .andDo(print()); // 요청·응답 전체를 콘솔에 찍는다. 실패했을 때 원인 파악용.

        // 2. 결과 검증
        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk())
                // 래퍼(RsData) 없이 배열 그대로 (ADR 0001: 조회는 DTO 그대로)
                .andExpect(jsonPath("$.length()").value(3));

        // 샘플 데이터: 글 1·2·3 = ("제목 N", "내용 N"). 최신순이므로 3, 2, 1 순서.
        for (int i = 0; i < 3; i++) {
            long id = 3 - i;
            resultActions
                    .andExpect(jsonPath("$[%d].id".formatted(i)).value(id))
                    .andExpect(jsonPath("$[%d].createDate".formatted(i)).value(matchesPattern(DATE_TIME_PATTERN)))
                    .andExpect(jsonPath("$[%d].modifyDate".formatted(i)).value(matchesPattern(DATE_TIME_PATTERN)))
                    .andExpect(jsonPath("$[%d].title".formatted(i)).value("제목 " + id))
                    .andExpect(jsonPath("$[%d].content".formatted(i)).value("내용 " + id))
                    // 엔티티 내부 구조(댓글 목록)는 응답에 나오면 안 된다
                    .andExpect(jsonPath("$[%d].comments".formatted(i)).doesNotExist())
                    // PostDto 필드는 정확히 5개: id, createDate, modifyDate, title, content
                    .andExpect(jsonPath("$[%d].length()".formatted(i)).value(5));
        }
    }
}
