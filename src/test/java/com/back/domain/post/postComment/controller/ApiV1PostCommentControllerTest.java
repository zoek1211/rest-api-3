package com.back.domain.post.postComment.controller;

// ApiV1PostCommentControllerTest
// 댓글 API(/api/v1/posts/{postId}/comments)를 HTTP 경계에서만 검증하는 테스트. (spec #1 Testing Decisions)
// 글 API 테스트(ApiV1PostControllerTest)와 같은 방식이다: 전체 컨텍스트 + MockMvc로 요청을 보내고 응답만 본다.
//
// 왜 테스트 클래스를 따로 두나: 컨트롤러가 ApiV1PostCommentController로 나뉘었기 때문이다.
//   테스트 클래스 하나 = 컨트롤러 하나로 맞춰 두면, "댓글 API가 무엇을 약속하는지"를 이 파일 하나에서 읽을 수 있다.
//
// 전제: 샘플 데이터(BaseInitData). 1번 글에 댓글 1·2·3, 2번 글에 댓글 4·5, 3번 글은 댓글 없음.

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
// 주의: Boot 4에서 @AutoConfigureMockMvc의 패키지가 바뀌었다.
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    @DisplayName("댓글 단건 조회: 1번 글의 1번 댓글이 모든 필드와 함께 온다")
    void getItem() throws Exception {
        // 1. 요청 보내기
        ResultActions resultActions = mvc
                .perform(get("/api/v1/posts/1/comments/1"))
                .andDo(print());

        // 2. 결과 검증
        // ADR 0001: 조회는 RsData 없이 DTO 그대로. 다건 조회의 배열 원소와 같은 모양(PostCommentDto)이다.
        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("getItem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.createDate").value(matchesPattern(DATE_TIME_PATTERN)))
                .andExpect(jsonPath("$.modifyDate").value(matchesPattern(DATE_TIME_PATTERN)))
                .andExpect(jsonPath("$.postId").value(1))
                .andExpect(jsonPath("$.content").value("댓글 1"))
                .andExpect(jsonPath("$.post").doesNotExist())
                .andExpect(jsonPath("$.length()").value(5));
    }

    // 단건 조회의 404 세 가지. 모두 같은 RsData 에러 모양(ADR 0001)이라 검증을 한곳에 모았다.
    private void expectGetItemNotFound(String url) throws Exception {
        mvc
                .perform(get(url))
                .andDo(print())
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("getItem"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 데이터가 존재하지 않습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("댓글 단건 조회: 없는 댓글이면 404")
    void getItem_commentNotFound() throws Exception {
        expectGetItemNotFound("/api/v1/posts/1/comments/%d".formatted(Integer.MAX_VALUE));
    }

    @Test
    @DisplayName("댓글 단건 조회: 다른 글의 댓글이면 404 (4번 댓글은 2번 글 소속)")
    void getItem_commentOfOtherPost() throws Exception {
        // 4번 댓글은 존재한다. 하지만 1번 글의 댓글이 아니므로 /posts/1/comments/4 라는 자원은 없다.
        expectGetItemNotFound("/api/v1/posts/1/comments/4");
    }

    @Test
    @DisplayName("댓글 단건 조회: 없는 글이면 404")
    void getItem_postNotFound() throws Exception {
        expectGetItemNotFound("/api/v1/posts/%d/comments/1".formatted(Integer.MAX_VALUE));
    }

    @Test
    @DisplayName("댓글 작성: 201 \"201-1\", data는 새 댓글(PostCommentDto). 다시 조회하면 1번 글의 댓글이 4개다")
    void write() throws Exception {
        // 1. 요청 보내기
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/posts/1/comments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "content": "새 댓글"
                                        }
                                        """)
                )
                .andDo(print());

        // 2. 결과 검증
        // ADR 0001: 쓰기 응답은 RsData로 감싼다. 상태 코드는 resultCode 앞 3자리(201)와 같다.
        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.length()").value(3))
                // 새 댓글의 id는 샘플 댓글(1–5)보다 크다. 정확한 값은 고정하지 않는다:
                //   auto_increment 값은 롤백되어도 되돌아가지 않아서, 다른 테스트가 먼저 댓글을 쓰면 번호가 밀린다.
                .andExpect(jsonPath("$.data.id").value(greaterThan(5)))
                .andExpect(jsonPath("$.data.createDate").value(matchesPattern(DATE_TIME_PATTERN)))
                .andExpect(jsonPath("$.data.modifyDate").value(matchesPattern(DATE_TIME_PATTERN)))
                .andExpect(jsonPath("$.data.postId").value(1))
                .andExpect(jsonPath("$.data.content").value("새 댓글"))
                .andExpect(jsonPath("$.data.length()").value(5));

        // msg의 N은 응답에 담긴 새 댓글 id와 같아야 한다.
        int id = JsonPath.read(resultActions.andReturn().getResponse().getContentAsString(), "$.data.id");
        resultActions.andExpect(jsonPath("$.msg").value("%d번 댓글이 작성되었습니다.".formatted(id)));

        // 3. 다시 조회: 정말 저장되었는지 HTTP로 확인한다(DB를 직접 보지 않는다).
        //   1번 글의 댓글은 샘플 3개 + 새 댓글 1개 = 4개이고, 최신순이라 새 댓글이 맨 앞이다.
        mvc
                .perform(get("/api/v1/posts/1/comments"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].content").value("새 댓글"));
    }

    @Test
    @DisplayName("댓글 작성: 본문이 비었으면 400 \"400-1\"")
    void write_blankContent() throws Exception {
        // 1. 요청 보내기
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/posts/1/comments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "content": ""
                                        }
                                        """)
                )
                .andDo(print());

        // 2. 결과 검증
        // 빈 문자열 ""은 @NotBlank와 @Size(min = 2)를 둘 다 어긴다 → 두 줄 (글 작성과 같은 형식).
        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value("""
                        content-NotBlank-must not be blank
                        content-Size-size must be between 2 and 100""".stripIndent()))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("댓글 작성: 없는 글이면 404")
    void write_postNotFound() throws Exception {
        mvc
                .perform(
                        post("/api/v1/posts/%d/comments".formatted(Integer.MAX_VALUE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "content": "새 댓글"
                                        }
                                        """)
                )
                .andDo(print())
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 데이터가 존재하지 않습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }
}
