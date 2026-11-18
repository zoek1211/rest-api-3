package com.back.domain.post.post.controller;

// ApiV1PostController
// 글(Post) API의 입구. 요청 흐름: HTTP 요청 → DispatcherServlet → "컨트롤러" → 서비스 → 저장소 → DB.
// 컨트롤러는 URL·HTTP 메서드를 메서드에 연결하고, 서비스 결과(엔티티)를 응답용 DTO로 바꾸는 일만 한다.
//
// 이름의 ApiV1: URL이 /api/v1/... 이라는 뜻. 나중에 응답 모양을 크게 바꿔야 하면 /api/v2를 따로 만들어
//   기존 클라이언트(v1)를 깨지 않고 새 버전을 낼 수 있다(API 버저닝).

import com.back.domain.post.post.dto.PostDto;
import com.back.domain.post.post.dto.PostModifyReqBody;
import com.back.domain.post.post.dto.PostWriteReqBody;
import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.service.PostService;
import com.back.global.rsData.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    // GET /api/v1/posts/{id}: 글 단건 조회.
    // @PathVariable: URL 경로의 {id} 자리 값을 메서드 인자로 받는다. "/posts/1"이면 id = 1.
    //   (?id=1 같은 쿼리 문자열은 @RequestParam. "어떤 자원 하나"를 가리킬 때는 경로에 넣는 게 REST 관례다.)
    // 없는 글이면 서비스가 예외를 던지고, 이 메서드는 거기서 끝난다.
    //   404 응답을 만드는 일은 GlobalExceptionHandler가 맡으므로 여기엔 if/null 검사가 없다.
    @GetMapping("/{id}")
    public PostDto getItem(@PathVariable long id) {
        return new PostDto(postService.findById(id));
    }

    // POST /api/v1/posts: 글 작성.
    // @RequestBody: 요청 본문(JSON)을 Jackson이 PostWriteReqBody로 바꿔 준다.
    //   주의: JSON이 깨져 있으면 이 변환 단계에서 HttpMessageNotReadableException이 나고, 메서드는 아예 실행되지 않는다.
    // @Valid: 변환된 객체의 검증 애너테이션(@NotBlank, @Size)을 검사하라는 표시.
    //   규칙을 어기면 MethodArgumentNotValidException이 나고, 역시 메서드 본문은 실행되지 않는다.
    //   주의: @Valid를 빼면 애너테이션이 붙어 있어도 아무 검사도 하지 않는다(빈 제목이 그대로 저장된다).
    // 두 예외 모두 GlobalExceptionHandler가 400 응답으로 바꾼다. 그래서 이 메서드는 "정상 흐름"만 쓴다.
    //
    // 반환 타입 RsData: ADR 0001에 따라 쓰기 응답은 RsData로 감싼다.
    //   상태 코드 201(Created, "새 자원이 생겼다")은 여기서 정하지 않는다. ResponseAspect가 "201-1"을 보고 정한다.
    @PostMapping
    public RsData<PostDto> write(@RequestBody @Valid PostWriteReqBody reqBody) {
        Post post = postService.write(reqBody.title(), reqBody.content());

        return new RsData<>(
                "201-1",
                "%d번 글이 작성되었습니다.".formatted(post.getId()),
                new PostDto(post)
        );
    }

    // PUT /api/v1/posts/{id}: 글 수정.
    // PUT은 "이 자원을 보낸 내용 전체로 바꿔라"라는 뜻이라, 제목과 본문을 둘 다 받는다.
    //   (일부 필드만 고치는 PATCH는 spec 범위 밖.) 같은 PUT을 여러 번 보내도 결과가 같다(멱등성).
    // 검증 실패(400)와 없는 글(404)은 작성·단건 조회와 똑같이 GlobalExceptionHandler가 처리한다.
    //
    // 반환: 수정은 "새로 생긴 것"이 없으니 201이 아니라 200("200-1")이고, 돌려줄 데이터도 없어 data는 null이다.
    //   RsData의 2개 인자 생성자가 data를 null로 채운다.
    @PutMapping("/{id}")
    public RsData<Void> modify(
            @PathVariable long id,
            @RequestBody @Valid PostModifyReqBody reqBody
    ) {
        postService.modify(id, reqBody.title(), reqBody.content());

        return new RsData<>(
                "200-1",
                "%d번 글이 수정되었습니다.".formatted(id)
        );
    }

    // DELETE /api/v1/posts/{id}: 글 삭제. 그 글의 댓글도 함께 지워진다(Post.comments의 cascade REMOVE).
    // DELETE도 PUT처럼 멱등이다: 같은 요청을 두 번 보내도 "그 글이 없다"는 최종 상태는 같다.
    //   (두 번째 응답은 404지만, 멱등성은 응답 코드가 아니라 서버 상태가 같으냐의 문제다.)
    // 반환: 수정과 같이 200("200-1"), data는 null.
    //   대안으로 204 No Content(본문 없음)도 흔하지만, ADR 0001에 따라 모든 쓰기 응답을 RsData 본문으로 통일했다.
    @DeleteMapping("/{id}")
    public RsData<Void> delete(@PathVariable long id) {
        postService.delete(id);

        return new RsData<>(
                "200-1",
                "%d번 글이 삭제되었습니다.".formatted(id)
        );
    }
}
