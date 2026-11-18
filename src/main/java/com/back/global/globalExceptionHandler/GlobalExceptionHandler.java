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
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.NoSuchElementException;
import java.util.Set;
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
    //   예) title-NotBlank-제목을 입력해 주세요.
    //       title-Size-제목은 2자 이상 100자 이하로 입력해 주세요.
    //   - 필드명(getField): 클라이언트가 고쳐야 할 JSON 키.
    //   - 규칙(getCode): 어긴 애너테이션의 짧은 이름(NotBlank, Size).
    //   - 메시지(getDefaultMessage): 사람용 설명. 요청 DTO 애너테이션의 message 속성에 적은 문구가 그대로 나온다.
    // 주의: message를 적지 않으면 Bean Validation 기본 메시지가 나오는데, 그 문구는 요청 언어(Accept-Language,
    //   없으면 서버 JVM 언어)에 따라 "must not be blank" / "공백일 수 없습니다"로 바뀐다. 환경마다 응답이 달라지지 않도록
    //   012부터 모든 요청 DTO에 한국어 message를 직접 적는다(PostWriteReqBody 주석).
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

    // ── 아래는 012에서 추가: Spring MVC가 "컨트롤러 메서드에 닿기 전에" 막는 에러들 ──
    // 이 처리기들이 없어도 Spring은 알맞은 상태 코드(400/404/405/415)를 돌려준다. 하지만 본문이 RsData가 아니라
    //   Spring 기본 에러 모양이라, "모든 에러 응답은 같은 {resultCode, msg, data} 모양"(spec #1 스토리 31)이 깨진다.
    //   클라이언트가 에러마다 다른 모양을 처리하지 않아도 되게 여기서 모두 RsData로 바꾼다.

    // MethodArgumentTypeMismatchException → 400 "400-3" (경로 변수·쿼리 파라미터 타입 오류)
    // 예) GET /api/v1/posts/abc: {id}는 long인데 "abc"는 숫자로 바꿀 수 없다. 메서드를 부르기 전 인자 변환에서 실패한다.
    // 왜 400-1·400-2와 따로인가: 400-1은 본문 "검증 규칙" 위반, 400-2는 본문 "JSON 형식" 오류,
    //   400-3은 URL에 담긴 값의 "타입" 오류다. 고칠 곳(본문 vs 주소)이 달라 코드를 나눈다.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public RsData<Void> handle(MethodArgumentTypeMismatchException e) {
        return new RsData<>("400-3", "요청 값의 형식이 올바르지 않습니다.");
    }

    // NoResourceFoundException → 404 "404-2" (없는 URL)
    // 매핑된 컨트롤러가 없으면 Spring MVC는 요청을 정적 리소스 처리기(ResourceHttpRequestHandler)에 넘기고,
    //   거기에도 파일이 없으면 이 예외를 던진다(Spring 6.1+). 그래서 "없는 URL" = 이 예외다.
    // 왜 404-1과 나누나: 404-1은 "주소는 맞는데 그 id의 데이터가 없음", 404-2는 "주소 자체가 없음(오타 등)"이다.
    //   클라이언트가 고칠 것이 다르다(id vs URL).
    @ExceptionHandler(NoResourceFoundException.class)
    public RsData<Void> handle(NoResourceFoundException e) {
        return new RsData<>("404-2", "요청한 주소가 존재하지 않습니다.");
    }

    // HttpRequestMethodNotSupportedException → 405 "405-1" (지원하지 않는 HTTP 메서드)
    // 예) PATCH /api/v1/posts/1: 주소는 있지만 그 주소에 PATCH 매핑이 없다. 404(주소 없음)가 아니라 405다.
    // Allow 헤더: 405 응답에는 "이 주소에서 되는 메서드"를 Allow 헤더로 알려 줘야 한다(HTTP 규칙, RFC 9110).
    //   Spring 기본 처리는 이 헤더를 넣어 주지만, 우리가 처리기를 가로챘으니 직접 넣는다.
    //   @ExceptionHandler 메서드도 컨트롤러처럼 HttpServletResponse를 인자로 받을 수 있다.
    // 주의: 상태 코드(405)는 여기서 정하지 않는다. ResponseAspect가 resultCode 앞자리로 정한다(ADR 0001).
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public RsData<Void> handle(HttpRequestMethodNotSupportedException e, HttpServletResponse response) {
        Set<HttpMethod> supportedMethods = e.getSupportedHttpMethods();
        if (supportedMethods != null) {
            response.setHeader(
                    HttpHeaders.ALLOW,
                    supportedMethods.stream().map(HttpMethod::name).sorted().collect(Collectors.joining(", "))
            );
        }

        return new RsData<>("405-1", "지원하지 않는 HTTP 메서드입니다.");
    }

    // HttpMediaTypeNotSupportedException → 415 "415-1" (지원하지 않는 Content-Type)
    // 예) POST /api/v1/posts에 Content-Type: text/plain. @RequestBody는 JSON 변환기(Jackson)로 읽는데,
    //   text/plain을 읽을 변환기가 없어 본문을 읽기도 전에 거절한다.
    // 왜 400-2(JSON 형식 오류)가 아닌가: 400-2는 "JSON이라고 했는데 깨졌다", 415는 "애초에 JSON이 아니라고 했다"이다.
    //   HTTP에는 이 경우를 위한 전용 상태(415 Unsupported Media Type)가 있다.
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public RsData<Void> handle(HttpMediaTypeNotSupportedException e) {
        return new RsData<>("415-1", "지원하지 않는 Content-Type입니다.");
    }
}
