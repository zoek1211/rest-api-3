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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    @DisplayName("글 단건 조회: 1번 글이 DTO 그대로(래퍼 없이) 온다")
    void getItem() throws Exception {
        // 1. 요청 보내기
        ResultActions resultActions = mvc
                .perform(get("/api/v1/posts/1"))
                .andDo(print());

        // 2. 결과 검증
        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("getItem"))
                .andExpect(status().isOk())
                // 단건 조회도 ADR 0001에 따라 RsData 없이 PostDto 객체 그대로다.
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.createDate").value(matchesPattern(DATE_TIME_PATTERN)))
                .andExpect(jsonPath("$.modifyDate").value(matchesPattern(DATE_TIME_PATTERN)))
                .andExpect(jsonPath("$.title").value("제목 1"))
                .andExpect(jsonPath("$.content").value("내용 1"))
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    @DisplayName("글 단건 조회: 없는 글이면 404와 공통 에러 형식(RsData)이 온다")
    void getItem_notFound() throws Exception {
        // 1. 요청 보내기: 샘플 글은 1·2·3번뿐이라 999번은 없다
        ResultActions resultActions = mvc
                .perform(get("/api/v1/posts/999"))
                .andDo(print());

        // 2. 결과 검증
        // ADR 0001 Consequences: HTTP 상태와 resultCode를 둘 다 보고 서로 맞는지 확인한다.
        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("getItem"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 데이터가 존재하지 않습니다."))
                // 주의: jsonPath("$.data").doesNotExist()는 "키가 아예 없음"과 "값이 null"을 구분하지 않는다.
                //   spec은 "data: null"(키는 있고 값이 null)이므로 value(nullValue())로 값을 보고,
                //   아래 length() == 3으로 키 3개(resultCode, msg, data)가 모두 있는지 확인한다.
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("글 작성: 201과 \"4번 글이 작성되었습니다.\", data는 작성된 글. 다시 다건 조회하면 4개다")
    void write() throws Exception {
        // 1. 요청 보내기
        // 요청 본문(JSON)은 텍스트 블록(""" ... """)으로 그대로 적는다.
        //   ObjectMapper로 만들 수도 있지만, "클라이언트가 실제로 보내는 바이트"를 눈으로 보는 편이 HTTP 경계 테스트답다.
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/posts")
                                // 주의: Content-Type을 빼면 @RequestBody가 JSON으로 읽지 못해 415가 난다.
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "새 제목",
                                            "content": "새 내용"
                                        }
                                        """)
                )
                .andDo(print());

        // 2. 결과 검증
        // 샘플 글이 1·2·3번이므로 새 글은 4번이다.
        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("write"))
                // ADR 0001: 컨트롤러는 RsData만 반환하고, ResponseAspect가 "201-1"의 앞자리로 201을 정한다.
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("4번 글이 작성되었습니다."))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.createDate").value(matchesPattern(DATE_TIME_PATTERN)))
                .andExpect(jsonPath("$.data.modifyDate").value(matchesPattern(DATE_TIME_PATTERN)))
                .andExpect(jsonPath("$.data.title").value("새 제목"))
                .andExpect(jsonPath("$.data.content").value("새 내용"))
                .andExpect(jsonPath("$.data.length()").value(5));

        // 정말 저장됐는지도 "DB를 직접 조회"하지 않고 HTTP로 확인한다(테스트 경계는 HTTP 하나).
        mvc.perform(get("/api/v1/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].id").value(4));
    }

    @Test
    @DisplayName("글 작성: 제목이 비었으면 400 \"400-1\", msg에 어떤 필드가 왜 틀렸는지 나온다")
    void write_blankTitle() throws Exception {
        // 1. 요청 보내기
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/posts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "",
                                            "content": "새 내용"
                                        }
                                        """)
                )
                .andDo(print());

        // 2. 결과 검증
        // 빈 문자열 ""은 @NotBlank와 @Size(min = 2)를 둘 다 어긴다 → 두 줄.
        // 줄 형식: 필드명-규칙-메시지. 메시지는 Bean Validation 기본 메시지다.
        //   (MockMvc 요청의 기본 Locale은 영어라 영어 메시지가 나온다. 실제 서버에서는 Accept-Language에 따라 달라진다.)
        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value("""
                        title-NotBlank-must not be blank
                        title-Size-size must be between 2 and 100""".stripIndent()))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("글 작성: 본문이 비었으면 400 \"400-1\"")
    void write_blankContent() throws Exception {
        // 1. 요청 보내기
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/posts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "새 제목",
                                            "content": ""
                                        }
                                        """)
                )
                .andDo(print());

        // 2. 결과 검증
        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value("""
                        content-NotBlank-must not be blank
                        content-Size-size must be between 2 and 5000""".stripIndent()))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("글 작성: JSON 형식이 깨졌으면 400 \"400-2\"")
    void write_malformedJson() throws Exception {
        // 1. 요청 보내기: 마지막 따옴표와 닫는 중괄호가 없다
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/posts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "새 제목",
                                            "content": "새 내용
                                        """)
                )
                .andDo(print());

        // 2. 결과 검증
        // JSON 변환(@RequestBody)에서 실패하므로 검증(@Valid)까지 가지도 않는다. 그래서 400-1이 아니라 400-2.
        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-2"))
                .andExpect(jsonPath("$.msg").value("요청 본문이 올바르지 않습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.length()").value(3));
    }
}
