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
}
