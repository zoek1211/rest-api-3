package com.back.domain.post.post.dto;

// PostWriteReqBody
// "글 작성" 요청 본문(JSON)을 담는 요청 DTO. 요청 흐름: 클라이언트 JSON → (Jackson이 이 record로 변환)
//   → (@Valid가 아래 규칙 검사) → ApiV1PostController.write(...) 인자.
//
// 요청 DTO(Request DTO)란: 클라이언트가 "보낼 수 있는 값"만 모아 둔 전용 상자.
//   왜 엔티티(Post)를 바로 받지 않나:
//   - 엔티티에는 id, createDate 같은 "서버가 정하는 값"도 있다. 엔티티로 받으면 클라이언트가 id를 보내
//     덮어쓰는 식의 사고(mass assignment)가 날 수 있다. 이 record에는 title, content만 있어서 그럴 수 없다.
//   - 검증 규칙(@NotBlank, @Size)은 "이 요청"의 규칙이다. 같은 엔티티라도 요청마다 규칙이 다를 수 있다.
//   응답용 PostDto와 이름을 나눠(ReqBody vs Dto) 들어오는 것/나가는 것을 구분한다.
//
// 이름의 ReqBody: "@RequestBody로 받는 요청 본문"이라는 뜻.

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Bean Validation 애너테이션: 필드에 "규칙"을 붙여 두면, 컨트롤러 인자에 @Valid가 있을 때 Spring이 검사한다.
//   @NotBlank: null, "", "   "(공백만) 모두 거절한다. (@NotNull은 null만, @NotEmpty는 null과 ""만 거절)
//   @Size(min, max): 문자열 길이 범위. 주의: @Size는 null을 "통과"시킨다. 그래서 @NotBlank와 함께 쓴다.
// 주의: 빈 문자열 ""은 @NotBlank와 @Size(min=2)를 "둘 다" 어긴다. 그래서 검증 실패 msg가 두 줄이 된다.
public record PostWriteReqBody(
        @NotBlank
        @Size(min = 2, max = 100)
        String title,

        @NotBlank
        @Size(min = 2, max = 5000)
        String content
) {
}
