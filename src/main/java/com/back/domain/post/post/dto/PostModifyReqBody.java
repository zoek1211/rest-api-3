package com.back.domain.post.post.dto;

// PostModifyReqBody
// "글 수정"(PUT /api/v1/posts/{id}) 요청 본문을 담는 요청 DTO.
//   요청 흐름: 클라이언트 JSON → (Jackson이 이 record로 변환) → (@Valid 검사) → ApiV1PostController.modify(...) 인자.
//
// 규칙은 작성(PostWriteReqBody)과 똑같다(spec: 수정도 작성과 같은 검증).
// 그런데 왜 PostWriteReqBody를 재사용하지 않고 따로 만드나:
//   - 지금은 같아도 "작성 요청"과 "수정 요청"은 서로 다른 이유로 바뀔 수 있다.
//     예: 나중에 수정 사유 필드가 생기거나, PATCH처럼 일부만 보내게 되면 수정 쪽만 바뀐다.
//   - 이름만 보고도 어느 API의 요청인지 알 수 있다.
//   대가로 애너테이션 몇 줄이 중복되지만, 학습 단계에서는 "요청마다 DTO 하나"가 더 읽기 쉽다.
//
// PUT은 자원 전체를 바꾸는 요청이라 두 필드 모두 필수(@NotBlank)다.

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 검증 문구(message)도 작성과 같다. 왜 문구를 직접 쓰는지는 PostWriteReqBody 주석 참고(언어 고정).
public record PostModifyReqBody(
        @NotBlank(message = "제목을 입력해 주세요.")
        @Size(min = 2, max = 100, message = "제목은 2자 이상 100자 이하로 입력해 주세요.")
        String title,

        @NotBlank(message = "본문을 입력해 주세요.")
        @Size(min = 2, max = 5000, message = "본문은 2자 이상 5000자 이하로 입력해 주세요.")
        String content
) {
}
