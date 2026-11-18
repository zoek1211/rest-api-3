package com.back.global.globalExceptionHandler;

// GlobalExceptionHandler
// 모든 컨트롤러에서 던져진 예외를 한 곳에서 받아 RsData 에러 응답으로 바꾼다.
// 요청 흐름: 컨트롤러 → 서비스에서 예외 발생 → (컨트롤러 밖으로 튀어나옴) → DispatcherServlet이
//   "이 예외를 처리할 @ExceptionHandler가 있나?" 찾음 → 여기 메서드가 응답을 만든다.
//
// 왜 전역 처리기인가:
//   컨트롤러마다 try-catch로 404를 만들면 같은 코드가 엔드포인트 수만큼 복사되고, 응답 모양이 조금씩 어긋나기 쉽다.
//   여기 한 곳에 모으면 컨트롤러는 "성공 흐름"만 쓰고, 에러 응답 형식(resultCode, msg, data)은 항상 같다.
//
// 처리기가 없으면? 예외가 DispatcherServlet 밖으로 나가 서블릿 컨테이너(톰캣)의 기본 에러 응답(500)이 된다.
//   MockMvc에서는 ServletException으로 테스트가 바로 터진다.

import com.back.global.rsData.RsData;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

// @RestControllerAdvice = @ControllerAdvice + @ResponseBody.
//   @ControllerAdvice: 여러 컨트롤러에 공통으로 적용할 것(예외 처리 등)을 모아 두는 클래스라는 표시.
//   @ResponseBody: 반환값을 JSON 응답 본문으로 쓴다(@RestController와 같은 이유).
@RestControllerAdvice
public class GlobalExceptionHandler {

    // NoSuchElementException → 404 "해당 데이터가 존재하지 않습니다."
    // 서비스가 Optional.orElseThrow()로 "찾는 데이터가 없다"를 알리면 이 예외가 날아온다.
    // 주의: 이 예외를 404로 바꾸는 것은 "없는 데이터"의 의미로만 쓴다는 약속이다.
    //   다른 이유로 NoSuchElementException이 나면(예: 빈 Iterator.next()) 버그인데도 404로 보일 수 있다.
    //   그런 혼동이 생기면 전용 예외 클래스를 만드는 것이 대안이다.
    //
    // 반환 타입이 ResponseEntity가 아니라 RsData인 이유 (ADR 0001):
    //   HTTP 상태는 ResponseAspect가 resultCode 앞자리("404-1" → 404)로 정한다. 그래서 여기서는 상태를 따로 적지 않는다.
    //   (이전 단계에서는 ResponseAspect가 없어 ResponseEntity.status(404)로 직접 정했다.)
    @ExceptionHandler(NoSuchElementException.class)
    public RsData<Void> handle(NoSuchElementException e) {
        return new RsData<>("404-1", "해당 데이터가 존재하지 않습니다.");
    }

    // MethodArgumentNotValidException → 400 "400-1" (입력값 검증 실패)
    // @Valid가 붙은 @RequestBody가 @NotBlank, @Size 같은 규칙을 어기면 이 예외가 난다.
    //   예외 안의 BindingResult에 "어떤 필드가 어떤 규칙을 어겼는지"(FieldError)가 모두 들어 있다.
    //
    // msg 형식: 한 줄에 하나씩 "필드명-규칙-메시지", 정렬해서 줄바꿈(\n)으로 잇는다.
    //   예) title-NotBlank-must not be blank
    //       title-Size-size must be between 2 and 100
    //   - 필드명(getField): 클라이언트가 고쳐야 할 JSON 키.
    //   - 규칙(getCode): 어긴 애너테이션의 짧은 이름(NotBlank, Size).
    //   - 메시지(getDefaultMessage): Bean Validation이 만든 사람용 설명.
    // 주의: 검증 결과의 순서는 보장되지 않는다(내부적으로 Set). 정렬하지 않으면 같은 요청에도 줄 순서가
    //   바뀌어 테스트가 들쭉날쭉해진다. 줄 전체를 정렬하면 필드명 순 → 같은 필드는 규칙 이름 순이 된다.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public RsData<Void> handle(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + "-" + fieldError.getCode() + "-" + fieldError.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("\n"));

        return new RsData<>("400-1", msg);
    }

    // HttpMessageNotReadableException → 400 "400-2" (JSON 형식 오류)
    // @RequestBody 변환 단계에서 Jackson이 본문을 읽지 못하면(괄호·따옴표 누락, 타입 불일치 등) 이 예외가 난다.
    //   변환에 실패했으니 @Valid 검증은 시작도 못 한다. 그래서 검증 실패(400-1)와 코드를 나눈다.
    // 왜 500이 아니라 400인가: 서버 고장이 아니라 "클라이언트가 잘못 보낸 요청"이기 때문이다.
    //   400(Bad Request)이어야 클라이언트가 "내 요청을 고쳐야 한다"는 것을 안다.
    // 주의: 예외 메시지(e.getMessage())를 그대로 내보내지 않는다. 내부 클래스 이름 같은 구현 정보가 섞여 있다.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public RsData<Void> handle(HttpMessageNotReadableException e) {
        return new RsData<>("400-2", "요청 본문이 올바르지 않습니다.");
    }
}
