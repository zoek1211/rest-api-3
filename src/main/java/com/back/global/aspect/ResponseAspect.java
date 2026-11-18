package com.back.global.aspect;

// ResponseAspect
// 컨트롤러·예외 처리기가 RsData를 반환하면, resultCode 앞자리("201-1" → 201)로 HTTP 응답 상태를 설정한다. (ADR 0001)
// 요청 흐름: DispatcherServlet → (프록시) → "ResponseAspect" → 컨트롤러 메서드 실행 → RsData 반환
//   → 여기서 response.setStatus(201) → Jackson이 RsData를 JSON 본문으로 쓴다.
//
// AOP(Aspect-Oriented Programming, 관점 지향 프로그래밍)란:
//   "여러 메서드에 공통으로 끼워 넣을 일"(로깅, 트랜잭션, 여기서는 상태 코드 설정)을 메서드 본문 밖으로 빼서
//   한 곳에 모으는 방법이다. 용어:
//   - Aspect: 공통 기능을 담은 클래스(이 클래스).
//   - Pointcut: "어느 메서드에 끼울지" 고르는 식(아래 @Around 안의 문자열).
//   - Advice: 끼워 넣는 코드 자체(아래 메서드). @Around는 대상 메서드의 "앞뒤를 감싸서" 실행한다.
//   Spring AOP는 Bean을 프록시(대리 객체)로 감싸 동작한다. 컨트롤러 Bean 대신 프록시가 호출을 먼저 받고,
//   advice를 실행한 뒤 진짜 메서드를 부른다. @Transactional도 같은 원리다.
//
// 왜 AOP인가 (대안과 비교):
//   - 컨트롤러마다 ResponseEntity.status(201).body(...)를 쓰면, resultCode("201-1")와 실제 상태(201)를
//     두 군데에서 따로 적게 되어 어긋날 수 있다. 여기서는 resultCode 한 곳만 보고 상태를 정한다.
//   - 컨트롤러는 "무슨 결과인지(RsData)"만 말하고, "HTTP로 어떻게 표현할지"는 이 Aspect가 맡는다.
//
// 주의: Spring AOP 프록시는 "Bean 바깥에서 들어오는 호출"만 가로챈다.
//   같은 클래스 안에서 this.otherMethod()로 부르면 프록시를 거치지 않아 advice가 실행되지 않는다(self-invocation).

import com.back.global.rsData.RsData;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// @Aspect: 이 클래스가 Aspect라는 표시. @Component로 Bean 등록까지 해야 Spring이 실제로 적용한다.
@Aspect
@Component
public class ResponseAspect {

    // Pointcut 읽는 법:
    //   execution(public com.back.global.rsData.RsData *(..))
    //     = 반환 타입이 RsData인 public 메서드, 이름(*)·인자(..)는 무엇이든.
    //   within(@...RestController *) || within(@...RestControllerAdvice *)
    //     = @RestController 클래스(컨트롤러)이거나 @RestControllerAdvice 클래스(GlobalExceptionHandler) 안의 메서드만.
    //   그래서 조회 메서드(List<PostDto>, PostDto 반환)는 대상이 아니고, 상태는 기본값 200 그대로다.
    @Around("""
            execution(public com.back.global.rsData.RsData *(..))
            && (
                within(@org.springframework.web.bind.annotation.RestController *)
                || within(@org.springframework.web.bind.annotation.RestControllerAdvice *)
            )
            """)
    public Object setStatusFromResultCode(ProceedingJoinPoint joinPoint) throws Throwable {
        // proceed(): 진짜 대상 메서드(컨트롤러의 write 등)를 실행하고 반환값을 받는다.
        Object result = joinPoint.proceed();

        // RequestContextHolder: 지금 처리 중인 요청/응답을 꺼내는 곳(요청마다 스레드에 묶여 있다).
        //   컨트롤러 인자로 HttpServletResponse를 받지 않아도 여기서 응답 객체에 접근할 수 있다.
        // 주의: 응답 본문을 쓰기 "전"에 상태를 정해야 한다. @Around는 Jackson이 본문을 쓰기 전,
        //   메서드 반환 직후에 실행되므로 이 시점의 setStatus가 그대로 반영된다.
        if (result instanceof RsData<?> rsData) {
            HttpServletResponse response =
                    ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getResponse();
            if (response != null) {
                response.setStatus(rsData.statusCode());
            }
        }

        return result;
    }
}
