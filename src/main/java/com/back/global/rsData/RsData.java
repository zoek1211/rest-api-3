package com.back.global.rsData;

// RsData (Result Data)
// 쓰기 응답과 모든 에러 응답을 감싸는 공통 봉투. JSON 모양: {"resultCode": "...", "msg": "...", "data": ...}
// 요청 흐름: 컨트롤러(쓰기) 또는 GlobalExceptionHandler(에러)가 RsData를 만들어 반환 → Jackson이 JSON으로 바꾼다.
//
// 왜 감싸나 (ADR 0001):
//   조회 응답은 데이터 자체가 결과라 DTO를 그대로 보낸다. 하지만 쓰기·에러 응답은
//   "무슨 일이 일어났는지(msg)"와 "어떤 결과인지(resultCode)"를 함께 알려줘야 해서 봉투가 필요하다.
//
// resultCode 형식: "HTTP상태-세부번호" (예: "404-1", "201-1").
//   앞자리는 HTTP 상태 코드와 같고, 뒷자리는 같은 상태 안에서 이유를 구분한다
//   (예: 400-1 = 입력값 검증 실패, 400-2 = JSON 형식 오류).
//   ResponseAspect가 이 앞자리를 읽어 HTTP 상태를 자동으로 맞춘다. 그래서 상태를 정하는 곳은 resultCode 한 곳뿐이다.
//
// 제네릭 <T>: data의 타입은 응답마다 다르다(PostDto, PostCommentDto, 없음=Void).
//   Object로 두면 꺼내 쓸 때마다 형변환이 필요하지만, <T>로 두면 컴파일러가 타입을 확인해 준다.

import com.fasterxml.jackson.annotation.JsonIgnore;

// record: resultCode·msg·data를 담기만 하는 불변 객체. Jackson은 컴포넌트 이름을 그대로 JSON 키로 쓴다.
// 주의: data가 null이어도 키는 빠지지 않고 "data": null로 나간다(Jackson 기본 설정).
//   클라이언트가 항상 같은 키 3개를 기대할 수 있도록 이 기본값을 그대로 둔다.
public record RsData<T>(
        String resultCode,
        String msg,
        T data
) {
    // data가 없는 응답(에러, 수정·삭제 성공)용 생성자. new RsData<>("404-1", "...")처럼 짧게 쓴다.
    public RsData(String resultCode, String msg) {
        this(resultCode, msg, null);
    }

    // resultCode의 앞자리 = HTTP 상태 코드. "201-1" → 201, "400-2" → 400.
    // ResponseAspect가 이 값으로 실제 응답 상태를 정한다(ADR 0001).
    // @JsonIgnore: 응답 JSON에는 넣지 않는다. resultCode에 이미 같은 정보가 있다.
    //   (record의 일반 메서드라 get 접두사가 없어 원래도 직렬화되지 않지만, 의도를 분명히 하려고 붙인다.)
    @JsonIgnore
    public int statusCode() {
        return Integer.parseInt(resultCode.split("-", 2)[0]);
    }
}
