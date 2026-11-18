# 조회는 DTO 그대로, 쓰기는 RsData로 감싸고 HTTP 상태는 ResponseAspect가 정한다

조회(다건·단건)는 `PostDto`/`PostCommentDto`를 그대로 JSON으로 반환하고, 작성·수정·삭제와 모든 에러는 `RsData{resultCode, msg, data}`로 감싼다. 컨트롤러는 `ResponseEntity`로 상태 코드를 직접 정하지 않고, `ResponseAspect`(AOP)가 `resultCode`의 앞자리(`"201-1"` → 201)를 읽어 HTTP 상태를 설정한다. 조회 응답은 데이터 자체가 결과라 래퍼가 군더더기이고, 쓰기 응답은 "무슨 일이 일어났는지"(msg)를 함께 전달해야 하기 때문이다. 상태 코드를 `resultCode` 한 곳에서만 정하면 둘이 어긋날 일이 없다.

## Considered Options

- **모든 응답을 RsData로 감싸기**: 일관되지만 조회 응답이 `data` 한 겹 깊어져 클라이언트 코드가 번거롭다.
- **컨트롤러마다 `ResponseEntity.status(...)`**: AOP 없이 단순하지만, `resultCode`와 실제 HTTP 상태를 두 군데에서 따로 관리하게 되어 어긋날 수 있다.

## Consequences

컨트롤러 메서드가 `RsData`를 반환하면 상태 코드가 자동으로 바뀐다. 그래서 테스트는 `status()`와 `$.resultCode`를 둘 다 검증해 둘이 일치하는지 확인한다.
