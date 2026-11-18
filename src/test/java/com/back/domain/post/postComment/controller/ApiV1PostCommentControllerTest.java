package com.back.domain.post.postComment.controller;

// ApiV1PostCommentControllerTest
// 댓글 API(/api/v1/posts/{postId}/comments)를 HTTP 경계에서만 검증하는 테스트. (spec #1 Testing Decisions)
// 글 API 테스트(ApiV1PostControllerTest)와 같은 방식이다: 전체 컨텍스트 + MockMvc로 요청을 보내고 응답만 본다.
//
// 왜 테스트 클래스를 따로 두나: 컨트롤러가 ApiV1PostCommentController로 나뉘었기 때문이다.
//   테스트 클래스 하나 = 컨트롤러 하나로 맞춰 두면, "댓글 API가 무엇을 약속하는지"를 이 파일 하나에서 읽을 수 있다.
//
// 전제: 샘플 데이터(BaseInitData). 1번 글에 댓글 1·2·3, 2번 글에 댓글 4·5, 3번 글은 댓글 없음.

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
// 주의: Boot 4에서 @AutoConfigureMockMvc의 패키지가 바뀌었다.
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// 테스트마다 롤백. 나중에 댓글 작성·수정·삭제 테스트가 생겨도 서로 영향을 주지 않는다.
@Transactional
class ApiV1PostCommentControllerTest {

    // 날짜는 값 대신 "존재 + ISO-8601 형식"만 본다. (ApiV1PostControllerTest와 같은 패턴)
    private static final String DATE_TIME_PATTERN = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?$";

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("댓글 다건 조회: 1번 글의 댓글 3개가 id 내림차순(최신순)으로 온다")
    void getItems() throws Exception {
        // 1. 요청 보내기
        ResultActions resultActions = mvc
                .perform(get("/api/v1/posts/1/comments"))
                .andDo(print());

        // 2. 결과 검증
        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk())
                // ADR 0001: 조회는 RsData 없이 DTO 배열 그대로
                .andExpect(jsonPath("$.length()").value(3));

        // 샘플 데이터: 1번 글의 댓글 1·2·3 = "댓글 N". 최신순이므로 3, 2, 1 순서.
        for (int i = 0; i < 3; i++) {
            long id = 3 - i;
            resultActions
                    .andExpect(jsonPath("$[%d].id".formatted(i)).value(id))
                    .andExpect(jsonPath("$[%d].createDate".formatted(i)).value(matchesPattern(DATE_TIME_PATTERN)))
                    .andExpect(jsonPath("$[%d].modifyDate".formatted(i)).value(matchesPattern(DATE_TIME_PATTERN)))
                    // 소속 글은 엔티티(post 객체) 대신 id 숫자 하나로만 나간다
                    .andExpect(jsonPath("$[%d].postId".formatted(i)).value(1))
                    .andExpect(jsonPath("$[%d].content".formatted(i)).value("댓글 " + id))
                    .andExpect(jsonPath("$[%d].post".formatted(i)).doesNotExist())
                    // PostCommentDto 필드는 정확히 5개: id, createDate, modifyDate, postId, content
                    .andExpect(jsonPath("$[%d].length()".formatted(i)).value(5));
        }
    }

    @Test
    @DisplayName("댓글 다건 조회: 댓글 없는 3번 글은 404가 아니라 빈 배열이다")
    void getItems_emptyPost() throws Exception {
        // 1. 요청 보내기
        ResultActions resultActions = mvc
                .perform(get("/api/v1/posts/3/comments"))
                .andDo(print());

        // 2. 결과 검증
        // "글은 있는데 댓글이 없다"는 오류가 아니라 정상 응답(200 + [])이다. 클라이언트는 "댓글 없음"으로 그리면 된다.
        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("댓글 다건 조회: 없는 글이면 404")
    void getItems_postNotFound() throws Exception {
        // 1. 요청 보내기
        ResultActions resultActions = mvc
                .perform(get("/api/v1/posts/%d/comments".formatted(Integer.MAX_VALUE)))
                .andDo(print());

        // 2. 결과 검증
        // "없는 글"과 "댓글 없는 글"을 구분한다: 앞의 것만 404다. 에러는 RsData 모양(ADR 0001).
        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 데이터가 존재하지 않습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }
}
