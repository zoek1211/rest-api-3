package com.back.domain.post.post.controller;

// ApiV1PostController
// 글(Post) API의 입구. 요청 흐름: HTTP 요청 → DispatcherServlet → "컨트롤러" → 서비스 → 저장소 → DB.
// 컨트롤러는 URL·HTTP 메서드를 메서드에 연결하고, 서비스 결과(엔티티)를 응답용 DTO로 바꾸는 일만 한다.
//
// 이름의 ApiV1: URL이 /api/v1/... 이라는 뜻. 나중에 응답 모양을 크게 바꿔야 하면 /api/v2를 따로 만들어
//   기존 클라이언트(v1)를 깨지 않고 새 버전을 낼 수 있다(API 버저닝).

import com.back.domain.post.post.dto.PostDto;
import com.back.domain.post.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// @RestController = @Controller + @ResponseBody.
//   메서드의 반환값을 HTML 뷰 이름이 아니라 "응답 본문"으로 보고, Jackson이 JSON으로 바꿔 쓴다.
@RestController
// 이 클래스의 모든 메서드 URL 앞에 /api/v1/posts가 붙는다.
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class ApiV1PostController {

    private final PostService postService;

    // GET /api/v1/posts: 글 다건 조회(최신순).
    // ADR 0001: 조회는 RsData로 감싸지 않고 DTO 배열을 그대로 반환한다. 상태 코드는 기본값 200.
    @GetMapping
    public List<PostDto> getItems() {
        return postService.findAllByOrderByIdDesc()
                .stream()
                .map(PostDto::new) // 엔티티 → DTO. 엔티티는 응답 밖으로 나가지 않는다.
                .toList();
    }
}
