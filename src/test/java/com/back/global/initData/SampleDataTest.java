package com.back.global.initData;

// SampleDataTest
// 앱이 시작될 때 샘플 데이터(글 3개, 댓글 5개, 분배 3/2/0)가 들어가는지 확인하는 테스트.
//
// 왜 저장소(Repository)를 직접 보나:
//   spec #1의 원칙은 "HTTP(MockMvc) 경계 하나에서만 검증"이다. 하지만 이 단계(001)에는 아직 엔드포인트가 없다.
//   그래서 임시로 전체 컨텍스트를 띄운 뒤 저장소로 개수만 확인하는 최소한의 테스트를 둔다.
//   이후 티켓에서 GET /api/v1/posts 같은 API가 생기면 샘플 데이터는 API 테스트로 검증되고,
//   그때 이 테스트는 지워도 된다.

import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
// @Transactional: 테스트 메서드 전체를 하나의 트랜잭션으로 묶는다.
//   여기서는 post.getComments()가 지연 로딩(LAZY)이라, 트랜잭션(영속성 컨텍스트)이 열려 있어야 댓글을 읽을 수 있다.
//   또 테스트가 끝나면 롤백되어 다른 테스트에 영향을 주지 않는다.
@Transactional
class SampleDataTest {

    @Autowired
    private PostRepository postRepository;

    @Test
    void 앱이_시작되면_글_3개와_댓글_5개가_3_2_0으로_들어가_있다() {
        List<Post> posts = postRepository.findAll(Sort.by("id"));

        assertThat(posts).hasSize(3);
        // id 오름차순 = 글 1, 2, 3. 댓글 1·2·3은 1번 글, 4·5는 2번 글, 3번 글은 댓글 없음.
        assertThat(posts.stream().map(p -> p.getComments().size()).toList())
                .containsExactly(3, 2, 0);
    }

    @Test
    void 작성일과_수정일이_자동으로_채워진다() {
        Post post = postRepository.findAll().getFirst();

        assertThat(post.getCreateDate()).isNotNull();
        assertThat(post.getModifyDate()).isNotNull();
    }
}
