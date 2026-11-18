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
//
// message 속성: 검증 실패 때 보여 줄 문구를 직접 정한다. (012에서 추가)
//   왜 기본 메시지를 쓰지 않나: 기본 메시지("must not be blank" / "공백일 수 없습니다")는 요청의 Locale
//   (Accept-Language 헤더, 없으면 서버 JVM의 언어 설정)에 따라 언어가 바뀐다. 그러면 같은 요청이라도
//   서버가 도는 환경마다 응답이 달라지고, 영어 문구를 고정한 테스트는 한국어 환경에서 깨진다.
//   문구를 한국어로 못 박아 두면 환경과 상관없이 항상 같은 응답이 나온다.
//   대안: ValidationMessages.properties에 문구를 모아 두는 방법도 있지만, 언어별 파일(_ko, _en)이 있으면
//   다시 Locale을 따라가므로 "한 가지 언어로 고정"이라는 목적에는 애너테이션에 바로 쓰는 쪽이 단순하다.
public record PostWriteReqBody(
        @NotBlank(message = "제목을 입력해 주세요.")
        @Size(min = 2, max = 100, message = "제목은 2자 이상 100자 이하로 입력해 주세요.")
        String title,

        @NotBlank(message = "본문을 입력해 주세요.")
        @Size(min = 2, max = 5000, message = "본문은 2자 이상 5000자 이하로 입력해 주세요.")
        String content
) {
}
