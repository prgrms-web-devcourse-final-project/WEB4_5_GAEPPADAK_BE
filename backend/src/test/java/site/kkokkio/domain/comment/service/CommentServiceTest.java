package site.kkokkio.domain.comment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import site.kkokkio.domain.comment.controller.dto.CommentCreateRequest;
import site.kkokkio.domain.comment.dto.CommentDto;
import site.kkokkio.domain.comment.dto.CommentReportRequestDto;
import site.kkokkio.domain.comment.entity.Comment;
import site.kkokkio.domain.comment.repository.CommentLikeRepository;
import site.kkokkio.domain.comment.repository.CommentRepository;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.domain.post.repository.PostRepository;
import site.kkokkio.domain.comment.entity.CommentReport;
import site.kkokkio.domain.comment.repository.CommentReportRepository;
import site.kkokkio.global.enums.ReportReason;
import site.kkokkio.global.exception.ServiceException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.*;

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

	@Mock
	private CommentReportRepository commentReportRepository;

	@Test
	@DisplayName("댓글 목록 조회 성공")
	void test1() {
		Post post = Post.builder().build();
		Member member = Member.builder().build();
		ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(member, "nickname", "testUser");
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
		ReflectionTestUtils.setField(member, "nickname", "testUser");

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
		ReflectionTestUtils.setField(member, "nickname", "testUser");

		Comment comment = Comment.builder().member(member).body("기존 댓글").build();
		ReflectionTestUtils.setField(comment, "id", 1L);
		ReflectionTestUtils.setField(comment, "createdAt", LocalDateTime.now());
		CommentCreateRequest request = new CommentCreateRequest("수정된 댓글");

		when(commentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(comment));

		CommentDto result = commentService.updateComment(1L, member.getId(), request);

		assertEquals("수정된 댓글", result.body());
	}

	@Test
	@DisplayName("댓글 수정 실패 - 없는 댓글")
	void test3_1() {
		Member member = Member.builder().build();
		ReflectionTestUtils.setField(member, "id", UUID.randomUUID());

		CommentCreateRequest request = new CommentCreateRequest("수정된 댓글");

		when(commentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

		assertThrows(ServiceException.class, () -> commentService.updateComment(1L, member.getId(), request));
	}

	@Test
	@DisplayName("댓글 수정 실패 - 본인 아님")
	void test3_2() {
		Member member = Member.builder().build();
		ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(member, "nickname", "testUser1");

		Member writer = Member.builder().build();
		ReflectionTestUtils.setField(writer, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(writer, "nickname", "testUser2");
		Comment comment = Comment.builder().member(writer).body("댓글").build();
		CommentCreateRequest request = new CommentCreateRequest("수정된 댓글");

		when(commentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(comment));

		assertThrows(ServiceException.class, () -> commentService.updateComment(1L, member.getId(), request));
	}

	@Test
	@DisplayName("댓글 삭제 성공")
	void test4() {
		Member member = Member.builder().build();
		ReflectionTestUtils.setField(member, "id", UUID.randomUUID());

		Comment comment = Comment.builder().member(member).body("댓글").build();

		when(commentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(comment));

		commentService.deleteCommentById(1L, member.getId());

		verify(commentRepository).save(any(Comment.class)); // softDelete 후 저장
	}

	@Test
	@DisplayName("댓글 삭제 실패 - 없는 댓글")
	void test4_1() {
		Member member = Member.builder().build();
		ReflectionTestUtils.setField(member, "id", UUID.randomUUID());

		when(commentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

		assertThrows(ServiceException.class, () -> commentService.deleteCommentById(1L, member.getId()));
	}

	@Test
	@DisplayName("댓글 삭제 실패 - 본인 아님")
	void test4_2() {
		Member member = Member.builder().build();
		ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(member, "nickname", "testUser1");

		Member writer = Member.builder().build();
		ReflectionTestUtils.setField(writer, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(writer, "nickname", "testUser2");
		Comment comment = Comment.builder().member(writer).body("댓글").build();

		when(commentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(comment));

		assertThrows(ServiceException.class, () -> commentService.deleteCommentById(1L, member.getId()));
	}

	@Test
	@DisplayName("댓글 좋아요 성공")
	void test5() {
		Member member1 = Member.builder().build();
		ReflectionTestUtils.setField(member1, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(member1, "nickname", "testUser1");
		Member member2 = Member.builder().build();
		ReflectionTestUtils.setField(member2, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(member2, "nickname", "testUser2");

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

		Member writer = Member.builder().build();
		ReflectionTestUtils.setField(writer, "id", UUID.randomUUID());

		Comment comment = Comment.builder().member(writer).body("댓글").build();

		when(commentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(comment));
		when(commentLikeRepository.existsByComment(comment)).thenReturn(true);

		assertThrows(ServiceException.class, () -> commentService.likeComment(1L, member));
	}

	@Test
	@DisplayName("댓글 좋아요 취소 성공")
	void test6() {
		Member member1 = Member.builder().build();
		ReflectionTestUtils.setField(member1, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(member1, "nickname", "testUser1");
		Member member2 = Member.builder().build();
		ReflectionTestUtils.setField(member2, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(member2, "nickname", "testUser2");

		Comment comment = Comment.builder().member(member2).body("댓글").likeCount(1).build();
		ReflectionTestUtils.setField(comment, "id", 1L);
		ReflectionTestUtils.setField(comment, "createdAt", LocalDateTime.now());

		when(commentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(comment));
		when(commentLikeRepository.existsByComment(comment)).thenReturn(true);

		CommentDto result = commentService.unlikeComment(1L, member1);

		assertEquals(0, result.likeCount());
	}

	@Test
	@DisplayName("댓글 신고 성공")
	void test7() {
		Long commentId = 1L;
		UUID reporterId = UUID.randomUUID();
		ReportReason reportReason = ReportReason.BAD_CONTENT;
		CommentReportRequestDto request = new CommentReportRequestDto(reportReason);

		// 댓글 작성자 Member 모킹
		Member commentWriter = mock(Member.class);
		when(commentWriter.getId()).thenReturn(UUID.randomUUID());

		// 신고 대상 댓글 Comment 모킹
		Comment comment = Comment.builder().member(commentWriter).body("댓글 내용").build();
		ReflectionTestUtils.setField(comment, "id", commentId);
		ReflectionTestUtils.setField(comment, "deletedAt", null);
		ReflectionTestUtils.setField(comment, "reportCount", 0);

		// commentRepository.findById 호출 시 모킹된 comment 객체 반환
		when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

		// 신고하는 사용자 Member 모킹
		Member reporter = Member.builder().build();
		ReflectionTestUtils.setField(reporter, "id", reporterId);

		// commentReportRepository.existsByCommentAndReporter 호출 시 false 반환 모킹
		when(commentReportRepository.existsByCommentAndReporter(comment, reporter)).thenReturn(false);

		// CommentReport 저장 모킹
		when(commentReportRepository.save(any(CommentReport.class))).thenReturn(mock(CommentReport.class));

		// commentRepository.save 호출 시 comment 객체 인자로 받아서 comment 객체 반환 모킹
		when(commentRepository.save(comment)).thenReturn(comment);

		// Service 메서드 호출
		commentService.reportComment(commentId, reporter, request.reason());

		/// 검증
		verify(commentRepository).findById(commentId);
		verify(commentReportRepository).existsByCommentAndReporter(comment, reporter);
		verify(commentReportRepository).save(any(CommentReport.class));
		verify(commentRepository).save(comment);

		assertEquals(1, comment.getReportCount());
	}

	@Test
	@DisplayName("댓글 신고 실패 - 댓글 찾을 수 없음")
	void test7_1() {
		Long commentId = 999L;
		ReportReason reportReason = ReportReason.BAD_CONTENT;
		CommentReportRequestDto request = new CommentReportRequestDto(reportReason);
		Member reporter = Member.builder().build();
		ReflectionTestUtils.setField(reporter, "id", UUID.randomUUID());

		// commentRepository.findById 호출 시 Optional.empty() 반환 모킹
		when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

		// ServiceException 발생 예상 및 검증
		ServiceException exception = assertThrows(ServiceException.class, () ->
				commentService.reportComment(commentId, reporter, request.reason()));

		// 예외 메시지 및 코드 검증
		assertEquals("404", exception.getCode());
		assertEquals("존재하지 않는 댓글입니다.", exception.getMessage());

		/// 검증
		verify(commentRepository).findById(commentId);
		verify(commentReportRepository, Mockito.never()).existsByCommentAndReporter(any(), any());
		verify(commentReportRepository, Mockito.never()).save(any());
	}

	@Test
	@DisplayName("댓글 신고 실패 - 삭제된 댓글")
	void test7_2() {
		Long commentId = 2L;
		ReportReason reportReason = ReportReason.BAD_CONTENT;
		CommentReportRequestDto request = new CommentReportRequestDto(reportReason);
		Member reporter = Member.builder().build();
		ReflectionTestUtils.setField(reporter, "id", UUID.randomUUID());

		// commentRepository.findById 호출 시 Optional.empty() 반환 모킹
		when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

		// ServiceException 발생 예상 및 검증
		ServiceException exception = assertThrows(ServiceException.class, () ->
				commentService.reportComment(commentId, reporter, request.reason()));
		
		// 예외 메시지 및 코드 검증
		assertEquals("404", exception.getCode());
		assertEquals("존재하지 않는 댓글입니다.", exception.getMessage());

		/// 검증
		verify(commentRepository).findById(commentId);
		verify(commentReportRepository, Mockito.never()).existsByCommentAndReporter(any(), any());
		verify(commentReportRepository, Mockito.never()).save(any());
	}

	@Test
	@DisplayName("댓글 신고 실패 - 본인 댓글 신고")
	void test7_3() {
		Long commentId = 3L;
		UUID reporterId = UUID.randomUUID();
		ReportReason reportReason = ReportReason.BAD_CONTENT;
		CommentReportRequestDto request = new CommentReportRequestDto(reportReason);

		// 신고하는 사용자 Member 실제 객체 생성 및 필드 설정
		Member reporter = Member.builder().build();
		ReflectionTestUtils.setField(reporter, "id", reporterId);

		// 신고 대상 댓글 Comment 실제 객체 생성
		Comment comment = Comment.builder().member(reporter).body("댓글 내용").build();
		ReflectionTestUtils.setField(comment, "id", commentId);
		ReflectionTestUtils.setField(comment, "deletedAt", null);
		ReflectionTestUtils.setField(comment, "reportCount", 0);

		when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

		// ServiceException 발생 예상 및 검증
		ServiceException exception = assertThrows(ServiceException.class, () ->
				commentService.reportComment(commentId, reporter, request.reason()));

		// 예외 메시지 및 코드 검증
		assertEquals("403", exception.getCode());
		assertEquals("본인의 댓글은 신고할 수 없습니다.", exception.getMessage());

		/// 검증
		verify(commentRepository).findById(commentId);
		verify(commentReportRepository, Mockito.never()).existsByCommentAndReporter(any(), any());
		verify(commentReportRepository, Mockito.never()).save(any());
	}

	@Test
	@DisplayName("댓글 신고 실패 - 중복 신고")
	void test7_4() {
		Long commentId = 4L;
		UUID reporterId = UUID.randomUUID();
		ReportReason reportReason = ReportReason.BAD_CONTENT;
		CommentReportRequestDto request = new CommentReportRequestDto(reportReason);

		// 댓글 작성자 Member 실제 객체 생성 및 필드 설정
		Member commentWriter = Member.builder().build();
		ReflectionTestUtils.setField(commentWriter, "id", UUID.randomUUID());

		// 신고 대상 댓글 Comment 실제 객체 생성 및 필드 설정
		Comment comment = Comment.builder().member(commentWriter).body("댓글 내용").build();
		ReflectionTestUtils.setField(comment, "id", commentId);
		ReflectionTestUtils.setField(comment, "deletedAt", null);
		ReflectionTestUtils.setField(comment, "reportCount", 0);

		when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

		// 신고하는 사용자 Member 실제 객체 생성
		Member reporter = Member.builder().build();
		ReflectionTestUtils.setField(reporter, "id", reporterId);

		// 중복 신고 발생 모킹
		when(commentReportRepository.existsByCommentAndReporter(comment, reporter)).thenReturn(true);

		// ServiceException 발생 예상 및 검증
		ServiceException exception = assertThrows(ServiceException.class, () ->
				commentService.reportComment(commentId, reporter, request.reason()));

		// 예외 메시지 및 코드 검증
		assertEquals("400", exception.getCode());
		assertEquals("이미 신고한 댓글입니다.", exception.getMessage());

		/// 검증
		verify(commentRepository).findById(commentId);
		verify(commentReportRepository).existsByCommentAndReporter(comment, reporter);
		verify(commentReportRepository, Mockito.never()).save(any());
	}
}
