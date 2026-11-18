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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

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
    // ResponseEntity: 본문(RsData)과 함께 HTTP 상태 코드까지 정해서 돌려주는 상자.
    //   ADR 0001은 상태 코드를 ResponseAspect가 resultCode 앞자리로 정하게 하지만, 아직 ResponseAspect가 없어서
    //   이번 단계에서는 여기서 직접 404를 정한다. ResponseAspect가 생기면 RsData만 반환하도록 바꿀 수 있다.
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<RsData<Void>> handle(NoSuchElementException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new RsData<>("404-1", "해당 데이터가 존재하지 않습니다."));
    }
}
