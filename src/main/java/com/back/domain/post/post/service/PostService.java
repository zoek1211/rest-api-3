package com.back.domain.post.post.service;

// PostService
// 글(Post)에 대한 업무 로직을 담당한다. 요청 흐름: 컨트롤러 → "서비스" → 저장소(PostRepository) → DB.
//
// 왜 컨트롤러에서 저장소를 바로 부르지 않나:
//   컨트롤러는 "HTTP 요청/응답 다루기"만, 서비스는 "무엇을 할지(업무 규칙)"만 맡게 나누기 위해서다.
//   그러면 트랜잭션 경계도 서비스 메서드 하나 = 업무 하나로 깔끔하게 잡힌다.

import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
// @RequiredArgsConstructor(Lombok): final 필드를 받는 생성자를 만들어 준다.
//   생성자가 하나뿐이면 Spring이 그 생성자로 Bean을 주입한다(생성자 주입). 필드 주입(@Autowired)보다
//   의존성이 명확하고, final이라 바뀌지 않으며, 테스트에서 직접 new 하기도 쉽다.
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    // @Transactional(readOnly = true): 읽기 전용 트랜잭션. 변경 감지(dirty checking)용 스냅샷을 만들지 않아
    //   조금 가볍고, "이 메서드는 아무것도 바꾸지 않는다"는 의도를 코드로 드러낸다.
    @Transactional(readOnly = true)
    public List<Post> findAllByOrderByIdDesc() {
        // 최신순 = id 내림차순. id는 auto_increment라 나중에 쓴 글일수록 크다.
        // createDate로 정렬하지 않는 이유: 같은 밀리초에 저장된 글은 순서가 뒤섞일 수 있지만 id는 항상 유일하다.
        return postRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional(readOnly = true)
    public Post findById(long id) {
        // findById는 Optional<Post>를 돌려준다. "없을 수도 있다"를 타입으로 드러내는 상자다.
        // orElseThrow(): 값이 있으면 꺼내고, 없으면 NoSuchElementException을 던진다.
        // 주의: optional.get()도 없으면 같은 예외를 던지지만, "없을 수 있음을 알고 처리했다"는 의도가
        //   드러나지 않아 orElseThrow()를 쓴다.
        // 왜 null을 반환하지 않나: 호출하는 쪽마다 null 검사를 해야 하고, 빠뜨리면 엉뚱한 곳에서
        //   NullPointerException이 난다. 예외를 던지면 GlobalExceptionHandler 한 곳에서 404로 바꿀 수 있다.
        return postRepository.findById(id).orElseThrow();
    }

    // 쓰기 메서드라 readOnly 없이 @Transactional. 메서드가 끝나면 커밋(예외가 나면 롤백)된다.
    @Transactional
    public Post write(String title, String content) {
        // save()는 INSERT 후 id가 채워진 엔티티를 돌려준다(id 생성 전략이 IDENTITY라 INSERT 즉시 id를 안다).
        //   createDate·modifyDate도 저장 직전에 JPA Auditing이 채운다.
        return postRepository.save(new Post(title, content));
    }

    // 글 수정. 없는 글이면 findById와 같이 NoSuchElementException → GlobalExceptionHandler가 404로 바꾼다.
    @Transactional
    public void modify(long id, String title, String content) {
        Post post = postRepository.findById(id).orElseThrow();

        post.modify(title, content);

        // 주의: 여기에 postRepository.save(post)가 없는 것은 실수가 아니다.
        // 변경 감지(dirty checking):
        //   1. findById로 읽은 엔티티는 "영속성 컨텍스트"(트랜잭션 동안 JPA가 엔티티를 기억해 두는 1차 캐시)에
        //      들어가고, JPA는 읽은 순간의 값을 스냅샷으로 따로 저장해 둔다.
        //   2. 트랜잭션이 커밋되기 직전(flush 시점)에 JPA가 엔티티의 현재 값과 스냅샷을 비교한다.
        //   3. 다른 필드가 있으면 UPDATE SQL을 자동으로 만들어 실행한다.
        //   그래서 "트랜잭션 안에서 읽은 엔티티는 값만 바꾸면 저장된다".
        // 주의: 이것은 @Transactional 안에서만 동작한다. 트랜잭션 밖에서 바꾼 엔티티(준영속 상태)는 아무도 비교하지
        //   않으므로 UPDATE가 나가지 않는다. readOnly = true 트랜잭션도 스냅샷을 만들지 않아 변경 감지를 하지 않는다.
        // modifyDate도 이때 갱신된다: UPDATE 직전에 JPA Auditing(@LastModifiedDate)이 현재 시각을 넣는다.

        // flush(): "커밋 직전"까지 기다리지 않고, 변경 감지 → UPDATE를 지금 바로 실행한다(커밋은 아니다).
        // 왜 필요한가: 이 메서드를 감싸는 더 바깥 트랜잭션이 있으면(예: @Transactional 테스트), 이 메서드가 끝나도
        //   커밋되지 않아 flush도, @LastModifiedDate 갱신도 일어나지 않는다. 그 상태로 같은 트랜잭션에서 단건 조회를 하면
        //   findById는 영속성 컨텍스트(1차 캐시)의 엔티티를 SQL 없이 그대로 돌려주므로, 옛 modifyDate가 보인다.
        //   지금 flush해 두면 "수정 API가 끝난 시점에 modifyDate는 이미 갱신되어 있다"가 항상 성립한다.
        // 주의: flush를 해도 롤백은 여전히 가능하다. 실행된 UPDATE는 트랜잭션이 끝날 때 커밋되거나 롤백된다.
        postRepository.flush();
    }
}
