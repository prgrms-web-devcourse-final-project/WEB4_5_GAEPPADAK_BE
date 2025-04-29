package site.kkokkio.domain.comment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import site.kkokkio.domain.comment.controller.dto.CommentCreateRequest;
import site.kkokkio.domain.comment.dto.CommentDto;
import site.kkokkio.domain.comment.entity.Comment;
import site.kkokkio.domain.comment.repository.CommentLikeRepository;
import site.kkokkio.domain.comment.repository.CommentRepository;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.domain.post.repository.PostRepository;
import site.kkokkio.global.exception.ServiceException;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {
	@InjectMocks
	private CommentService commentService;

	@Mock
	private CommentRepository commentRepository;

	@Mock
	private PostRepository postRepository;

	@Mock
	private CommentLikeRepository commentLikeRepository;

	@Test
	@DisplayName("댓글 목록 조회 성공")
	void test1() {
		Post post = Post.builder().build();
		Member member = Member.builder().build();
		ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
		Comment comment = Comment.builder().post(post).member(member).body("댓글").build();
		ReflectionTestUtils.setField(comment, "id", 1L);
		ReflectionTestUtils.setField(comment, "createdAt", LocalDateTime.now());
		Page<Comment> comments = new PageImpl<>(List.of(comment));

		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(commentRepository.findAllByPostAndDeletedAtIsNull(eq(post), any())).thenReturn(comments);

		Page<CommentDto> result = commentService.getCommentListByPostId(1L, PageRequest.of(0, 10));

		assertEquals(1, result.getTotalElements());
	}

	@Test
	@DisplayName("댓글 목록 조회 실패 - 없는 포스트")
	void test1_1() {
		when(postRepository.findById(1L)).thenReturn(Optional.empty());

		assertThrows(ServiceException.class, () -> commentService.getCommentListByPostId(1L, PageRequest.of(0, 10)));
	}

	@Test
	@DisplayName("댓글 작성 성공")
	void test2() {
		Post post = Post.builder().build();
		CommentCreateRequest request = new CommentCreateRequest("새 댓글");
		when(postRepository.findById(1L)).thenReturn(Optional.of(post));

		Member member = Member.builder().build();
		ReflectionTestUtils.setField(member, "id", UUID.randomUUID());

		Comment comment = Comment.builder()
			.post(post)
			.member(member)
			.body(request.body())
			.build();
		ReflectionTestUtils.setField(comment, "id", 1L);
		ReflectionTestUtils.setField(comment, "createdAt", LocalDateTime.now());
		given(commentRepository.save(any(Comment.class))).willReturn(comment);

		CommentDto result = commentService.createComment(1L, member, request);

		assertEquals("새 댓글", result.body());
	}

	@Test
	@DisplayName("댓글 작성 실패 - 없는 포스트")
	void test2_1() {
		Member member = Member.builder().build();
		ReflectionTestUtils.setField(member, "id", UUID.randomUUID());

		CommentCreateRequest request = new CommentCreateRequest("댓글");

		when(postRepository.findById(1L)).thenReturn(Optional.empty());

		assertThrows(ServiceException.class, () -> commentService.createComment(1L, member, request));
	}

	@Test
	@DisplayName("댓글 수정 성공")
	void test3() {
		Member member = Member.builder().build();
		ReflectionTestUtils.setField(member, "id", UUID.randomUUID());

		Comment comment = Comment.builder().member(member).body("기존 댓글").build();
		ReflectionTestUtils.setField(comment, "id", 1L);
		ReflectionTestUtils.setField(comment, "createdAt", LocalDateTime.now());
		CommentCreateRequest request = new CommentCreateRequest("수정된 댓글");

		when(commentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(comment));

		CommentDto result = commentService.updateComment(1L, member, request);

		assertEquals("수정된 댓글", result.body());
	}

	@Test
	@DisplayName("댓글 수정 실패 - 없는 댓글")
	void test3_1() {
		Member member = Member.builder().build();
		ReflectionTestUtils.setField(member, "id", UUID.randomUUID());

		CommentCreateRequest request = new CommentCreateRequest("수정된 댓글");

		when(commentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

		assertThrows(ServiceException.class, () -> commentService.updateComment(1L, member, request));
	}

	@Test
	@DisplayName("댓글 수정 실패 - 본인 아님")
	void test3_2() {
		Member member = Member.builder().build();
		ReflectionTestUtils.setField(member, "id", UUID.randomUUID());

		Comment comment = Comment.builder().member(mock(Member.class)).body("댓글").build();
		CommentCreateRequest request = new CommentCreateRequest("수정된 댓글");

		when(commentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(comment));

		assertThrows(ServiceException.class, () -> commentService.updateComment(1L, member, request));
	}

	@Test
	@DisplayName("댓글 삭제 성공")
	void test4() {
		Member member = Member.builder().build();
		ReflectionTestUtils.setField(member, "id", UUID.randomUUID());

		Comment comment = Comment.builder().member(member).body("댓글").build();

		when(commentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(comment));

		commentService.deleteCommentById(1L, member);

		verify(commentRepository).save(any(Comment.class)); // softDelete 후 저장
	}

	@Test
	@DisplayName("댓글 삭제 실패 - 없는 댓글")
	void test4_1() {
		Member member = Member.builder().build();
		ReflectionTestUtils.setField(member, "id", UUID.randomUUID());

		when(commentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

		assertThrows(ServiceException.class, () -> commentService.deleteCommentById(1L, member));
	}

	@Test
	@DisplayName("댓글 삭제 실패 - 본인 아님")
	void test4_2() {
		Member member = Member.builder().build();
		ReflectionTestUtils.setField(member, "id", UUID.randomUUID());

		Comment comment = Comment.builder().member(mock(Member.class)).body("댓글").build();

		when(commentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(comment));

		assertThrows(ServiceException.class, () -> commentService.deleteCommentById(1L, member));
	}

	@Test
	@DisplayName("댓글 좋아요 성공")
	void test5() {
		Member member1 = Member.builder().build();
		ReflectionTestUtils.setField(member1, "id", UUID.randomUUID());
		Member member2 = Member.builder().build();
		ReflectionTestUtils.setField(member2, "id", UUID.randomUUID());

		Comment comment = Comment.builder().member(member2).body("댓글").likeCount(0).build();
		ReflectionTestUtils.setField(comment, "id", 1L);
		ReflectionTestUtils.setField(comment, "createdAt", LocalDateTime.now());

		when(commentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(comment));
		when(commentLikeRepository.existsByComment(comment)).thenReturn(false);

		CommentDto result = commentService.likeComment(1L, member1);

		assertEquals(1, result.likeCount());
	}

	@Test
	@DisplayName("댓글 좋아요 실패 - 없는 댓글")
	void test5_1() {
		Member member = Member.builder().build();
		ReflectionTestUtils.setField(member, "id", UUID.randomUUID());

		when(commentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

		assertThrows(ServiceException.class, () -> commentService.likeComment(1L, member));
	}

	@Test
	@DisplayName("댓글 좋아요 실패 - 본인 댓글")
	void test5_2() {
		Member member = Member.builder().build();
		ReflectionTestUtils.setField(member, "id", UUID.randomUUID());

		Comment comment = Comment.builder().member(member).body("댓글").build();

		when(commentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(comment));

		assertThrows(ServiceException.class, () -> commentService.likeComment(1L, member));
	}

	@Test
	@DisplayName("댓글 좋아요 실패 - 이미 좋아요 누름")
	void test5_3() {
		Member member = Member.builder().build();
		ReflectionTestUtils.setField(member, "id", UUID.randomUUID());

		Comment comment = Comment.builder().member(mock(Member.class)).body("댓글").build();

		when(commentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(comment));
		when(commentLikeRepository.existsByComment(comment)).thenReturn(true);

		assertThrows(ServiceException.class, () -> commentService.likeComment(1L, member));
	}

}