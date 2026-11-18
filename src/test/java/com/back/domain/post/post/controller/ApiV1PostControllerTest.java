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

import com.jayway.jsonpath.JsonPath;
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
// delete는 static import하지 않는다: 테스트 메서드 이름 delete()와 겹쳐 헷갈리므로 클래스 이름을 붙여 부른다.
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import static com.back.global.test.ApiTestSupport.DATE_TIME_PATTERN;
import static com.back.global.test.ApiTestSupport.NOT_EXISTING_ID;
import static com.back.global.test.ApiTestSupport.notFound;
import static com.back.global.test.ApiTestSupport.rsDataError;
import static com.back.global.test.ApiTestSupport.validationFailed;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @SpringBootTest: 전체 Spring 컨텍스트(모든 Bean)를 띄운다. 샘플 데이터(SampleData)도 이때 들어간다.
@SpringBootTest
// @AutoConfigureMockMvc: MockMvc Bean을 만들어 주입받을 수 있게 한다.
@AutoConfigureMockMvc
// test 프로필 = H2 메모리 DB (src/test/resources/application-test.yml)
@ActiveProfiles("test")
// @Transactional: 테스트마다 트랜잭션을 열고 끝나면 롤백한다.
//   그래서 한 테스트가 글을 지우거나 고쳐도 다음 테스트는 항상 샘플 데이터 그대로에서 시작한다.
@Transactional
class ApiV1PostControllerTest {


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
        // 1. 요청 보내기: 샘플 글은 1·2·3번뿐이라 NOT_EXISTING_ID(int 최댓값)번 글은 없다(ApiTestSupport)
        ResultActions resultActions = mvc
                .perform(get("/api/v1/posts/%d".formatted(NOT_EXISTING_ID)))
                .andDo(print());

        // 2. 결과 검증
        // ADR 0001 Consequences: HTTP 상태와 resultCode를 둘 다 보고 서로 맞는지 확인한다.
        // notFound(): 404 + RsData{"404-1", "해당 데이터가 존재하지 않습니다.", null} 검증 묶음(ApiTestSupport).
        //   글·댓글 테스트에서 똑같이 반복되던 다섯 줄을 012에서 하나로 모았다.
        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("getItem"))
                .andExpect(notFound());
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
        // 줄 형식: 필드명-규칙-메시지. 메시지는 요청 DTO의 애너테이션에 적어 둔 한국어 문구다(message 속성).
        //   MockMvc 요청의 기본 Locale은 영어인데도 한국어가 나온다 = 문구가 요청 언어를 따라가지 않는다는 뜻이다.
        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("write"))
                .andExpect(validationFailed("""
                        title-NotBlank-제목을 입력해 주세요.
                        title-Size-제목은 2자 이상 100자 이하로 입력해 주세요.""".stripIndent()));
    }

    @Test
    @DisplayName("글 작성: 검증 실패 msg는 요청 언어(Accept-Language)와 상관없이 항상 같다")
    void write_blankTitle_sameMsgForAnyLocale() throws Exception {
        // 같은 잘못된 요청을 영어·한국어 두 언어로 보내고, msg가 완전히 같은지 본다.
        // 주의: Bean Validation 기본 메시지는 "요청의 Locale"(Accept-Language, 없으면 서버 JVM 언어)에 따라
        //   "must not be blank" / "공백일 수 없습니다"처럼 바뀐다. 그러면 같은 API가 환경마다 다른 응답을 준다.
        for (String language : new String[]{"en", "ko-KR"}) {
            mvc
                    .perform(
                            post("/api/v1/posts")
                                    .header("Accept-Language", language)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                                "title": "",
                                                "content": "새 내용"
                                            }
                                            """)
                    )
                    .andDo(print())
                    .andExpect(validationFailed("""
                            title-NotBlank-제목을 입력해 주세요.
                            title-Size-제목은 2자 이상 100자 이하로 입력해 주세요.""".stripIndent()));
        }
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
                .andExpect(validationFailed("""
                        content-NotBlank-본문을 입력해 주세요.
                        content-Size-본문은 2자 이상 5000자 이하로 입력해 주세요.""".stripIndent()));
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
                .andExpect(rsDataError(400, "400-2", "요청 본문이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("글 수정: 200 \"1번 글이 수정되었습니다.\", 다시 조회하면 제목·본문이 바뀌고 수정일이 갱신된다")
    void modify() throws Exception {
        // 0. 준비: 수정 전 수정일(modifyDate)을 HTTP로 읽어 둔다. 수정 후 값과 비교하기 위해서다.
        //   JsonPath.read: 응답 JSON 문자열에서 경로 하나의 값을 꺼낸다(jsonPath() 검증과 같은 문법).
        String oldModifyDate = JsonPath.read(
                mvc.perform(get("/api/v1/posts/1"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.modifyDate"
        );

        // 1. 요청 보내기
        // PUT: 자원 "전체"를 보낸 값으로 바꾼다. 그래서 title, content 둘 다 보낸다.
        //   (일부 필드만 고치는 PATCH는 spec의 범위 밖이다.)
        ResultActions resultActions = mvc
                .perform(
                        put("/api/v1/posts/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "수정된 제목",
                                            "content": "수정된 내용"
                                        }
                                        """)
                )
                .andDo(print());

        // 2. 결과 검증
        // 수정은 새로 만든 것이 없으니 201이 아니라 200, 돌려줄 데이터도 없어 data는 null이다.
        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("modify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("1번 글이 수정되었습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.length()").value(3));

        // 정말 바뀌었는지는 같은 API(단건 조회)로 다시 확인한다.
        mvc.perform(get("/api/v1/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 제목"))
                .andExpect(jsonPath("$.content").value("수정된 내용"))
                .andExpect(jsonPath("$.modifyDate").value(matchesPattern(DATE_TIME_PATTERN)))
                .andExpect(jsonPath("$.modifyDate").value(not(oldModifyDate)));
    }

    @Test
    @DisplayName("글 수정: 없는 글이면 404 \"404-1\"")
    void modify_notFound() throws Exception {
        // 1. 요청 보내기: 본문은 올바르지만 NOT_EXISTING_ID번 글이 없다
        ResultActions resultActions = mvc
                .perform(
                        put("/api/v1/posts/%d".formatted(NOT_EXISTING_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "수정된 제목",
                                            "content": "수정된 내용"
                                        }
                                        """)
                )
                .andDo(print());

        // 2. 결과 검증
        // 서비스의 findById(...).orElseThrow()가 던진 예외를 GlobalExceptionHandler가 404로 바꾼다(단건 조회와 같은 길).
        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("modify"))
                .andExpect(notFound());
    }

    @Test
    @DisplayName("글 수정: 제목이 비었으면 400 \"400-1\"이고, 글은 바뀌지 않는다")
    void modify_blankTitle() throws Exception {
        // 1. 요청 보내기
        ResultActions resultActions = mvc
                .perform(
                        put("/api/v1/posts/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "",
                                            "content": "수정된 내용"
                                        }
                                        """)
                )
                .andDo(print());

        // 2. 결과 검증: 작성과 같은 검증 규칙이라 같은 msg가 나온다.
        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("modify"))
                .andExpect(validationFailed("""
                        title-NotBlank-제목을 입력해 주세요.
                        title-Size-제목은 2자 이상 100자 이하로 입력해 주세요.""".stripIndent()));

        // @Valid에서 막혀 서비스까지 가지 않았으므로, 본문도 옛 값 그대로다.
        mvc.perform(get("/api/v1/posts/1"))
                .andExpect(jsonPath("$.title").value("제목 1"))
                .andExpect(jsonPath("$.content").value("내용 1"));
    }

    @Test
    @DisplayName("글 삭제: 댓글 3개가 달린 1번 글을 지우면 200 \"200-1\"이고, 이후 단건 조회는 404다")
    void delete() throws Exception {
        // 1. 요청 보내기
        // 샘플 데이터 전제: 1번 글에는 댓글이 3개 있다. 댓글이 달린 글을 지워도 오류가 없어야 한다.
        ResultActions resultActions = mvc
                .perform(
                        MockMvcRequestBuilders.delete("/api/v1/posts/1")
                )
                .andDo(print());

        // 2. 결과 검증
        // 삭제도 "새로 생긴 것"이 없으므로 200("200-1"), 돌려줄 데이터가 없어 data는 null이다.
        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("1번 글이 삭제되었습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.length()").value(3));

        // 정말 지워졌는지는 DB를 들여다보지 않고 같은 API(단건 조회)로 확인한다.
        mvc.perform(get("/api/v1/posts/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("글 삭제: 없는 글이면 404 \"404-1\"")
    void delete_notFound() throws Exception {
        // 1. 요청 보내기
        ResultActions resultActions = mvc
                .perform(
                        MockMvcRequestBuilders.delete("/api/v1/posts/%d".formatted(NOT_EXISTING_ID))
                )
                .andDo(print());

        // 2. 결과 검증: 단건 조회·수정과 같은 길(orElseThrow → GlobalExceptionHandler)로 404가 된다.
        resultActions
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("delete"))
                .andExpect(notFound());
    }
}
