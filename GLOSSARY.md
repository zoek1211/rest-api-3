# rest-api-3

글과 그 글에 달리는 댓글을 다루는 학습용 게시판 API.

## Language

**글 (Post)**:
제목과 본문을 가진 게시물. 댓글을 0개 이상 가진다.
_Avoid_: Article, 게시글, Board

**댓글 (PostComment)**:
특정 글 하나에 속한 짧은 본문. 소속 글 없이는 존재할 수 없고, 글이 삭제되면 함께 사라진다.
_Avoid_: Comment, Reply, 답글

**샘플 데이터 (Sample data)**:
앱이 시작될 때 DB가 비어 있으면 미리 채워 넣는 글 3개와 댓글 5개. 테스트도 이 데이터가 있다는 것을 전제로 한다.
_Avoid_: Seed, 더미 데이터, 초기 데이터
