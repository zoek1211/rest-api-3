package com.back.global.sampleData;

// SampleData
// 앱이 시작될 때 샘플 데이터(글 3개, 댓글 5개)를 넣는 컴포넌트. (GLOSSARY.md "샘플 데이터")
// 요청 흐름과는 별개로, 앱이 뜬 직후 딱 한 번 실행된다: 컨텍스트 생성 → 서버 시작 → ApplicationRunner.run().
//
// 이름: 011까지는 global.initData.BaseInitData였다. GLOSSARY가 "초기 데이터"를 피하고 "샘플 데이터"로 부르기로 했으므로
//   012에서 global.sampleData.SampleData로 바꿨다. 코드의 이름과 대화에서 쓰는 용어가 같아야 찾기 쉽다.
//
// 왜 모든 프로필(dev, test)에서 동작하나:
//   이후 테스트들이 "1번 글에 댓글 3개가 있다" 같은 샘플 데이터를 전제로 쓰기 때문이다 (spec #1).
//   그래서 @Profile을 붙이지 않았다.

import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
// @RequiredArgsConstructor(Lombok): final 필드를 받는 생성자를 만들어 준다 → 스프링이 생성자 주입으로 저장소를 넣는다.
@RequiredArgsConstructor
// ApplicationRunner: 스프링 컨텍스트가 다 준비된 뒤 자동으로 run()이 호출되는 인터페이스.
//   @SpringBootTest로 띄운 테스트 컨텍스트에서도 실행된다.
public class SampleData implements ApplicationRunner {

    private final PostRepository postRepository;

    // @Transactional: run() 전체를 하나의 트랜잭션으로 묶는다.
    //   트랜잭션이란 "전부 성공하거나 전부 취소되는" 작업 단위다. 중간에 실패하면 글만 들어가고 댓글은 빠지는 일이 없다.
    //   또 트랜잭션 안에서는 저장된 글이 영속성 컨텍스트에 붙어 있어서, addPostComment()로 넣은 댓글이
    //   cascade PERSIST로 커밋 시점에 함께 INSERT된다.
    // 주의: @Transactional은 스프링이 만든 프록시를 거쳐 호출될 때만 동작한다.
    //   run()은 스프링이 Bean(프록시)을 통해 부르므로 괜찮지만, 같은 클래스 안에서 this.다른메서드()로 부르면 무시된다.
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 글이 하나라도 있으면 이미 넣은 것으로 보고 건너뛴다.
        // dev는 파일 DB라 재시작해도 데이터가 남는다. 이 검사가 없으면 bootRun 할 때마다 3개씩 쌓인다.
        if (postRepository.count() > 0) return;

        Post post1 = postRepository.save(new Post("제목 1", "내용 1"));
        Post post2 = postRepository.save(new Post("제목 2", "내용 2"));
        postRepository.save(new Post("제목 3", "내용 3")); // 3번 글은 댓글 없음

        post1.addPostComment("댓글 1");
        post1.addPostComment("댓글 2");
        post1.addPostComment("댓글 3");
        post2.addPostComment("댓글 4");
        post2.addPostComment("댓글 5");
    }
}
