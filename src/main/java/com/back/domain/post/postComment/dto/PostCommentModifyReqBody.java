package com.back.domain.post.postComment.dto;

// PostCommentModifyReqBody
// "댓글 수정" 요청 본문(JSON)을 담는 요청 DTO. 요청 흐름: 클라이언트 JSON → (Jackson이 이 record로 변환)
//   → (@Valid가 아래 규칙 검사) → ApiV1PostCommentController.modify(...) 인자.
//
// 왜 작성용(PostCommentWriteReqBody)을 재사용하지 않나: 지금은 필드와 규칙이 똑같지만,
//   "작성"과 "수정"은 나중에 따로 바뀔 수 있는 별개의 요청이다(예: 수정에만 사유 필드 추가).
//   글 쪽(PostWriteReqBody / PostModifyReqBody)과 같은 방식으로, 요청마다 DTO 하나를 둔다.
// 바꿀 수 있는 것은 content 하나뿐이다. 어느 글의 몇 번 댓글인지는 URL 경로({postId}, {id})로 정한다.

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 규칙은 작성과 같다: 2–100자, 공백만은 안 된다.
// 주의: @Size는 null을 통과시키므로 @NotBlank와 함께 쓴다.
public record PostCommentModifyReqBody(
        @NotBlank
        @Size(min = 2, max = 100)
        String content
) {
}
