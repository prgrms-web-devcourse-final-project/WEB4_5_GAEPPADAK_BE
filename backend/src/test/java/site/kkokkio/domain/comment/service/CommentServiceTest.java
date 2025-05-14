package site.kkokkio.domain.comment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import site.kkokkio.domain.comment.controller.dto.CommentCreateRequest;
import site.kkokkio.domain.comment.dto.CommentDto;
import site.kkokkio.domain.comment.dto.CommentReportRequestDto;
import site.kkokkio.domain.comment.dto.ReportedCommentSummary;
import site.kkokkio.domain.comment.entity.Comment;
import site.kkokkio.domain.comment.entity.CommentReport;
import site.kkokkio.domain.comment.repository.CommentLikeRepository;
import site.kkokkio.domain.comment.repository.CommentReportRepository;
import site.kkokkio.domain.comment.repository.CommentRepository;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.domain.post.repository.PostRepository;
import site.kkokkio.global.enums.ReportReason;
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

	@Test
	@DisplayName("신고된 댓글 목록 조회 - 성공 (기본 파라미터)")
	void test8() {
		/// given
		Pageable inputPageable = PageRequest.of(0, 10, Sort.unsorted());
		Pageable expectedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "reportedAt"));

		// Repository가 반환할 Mock 데이터
		ReportedCommentSummary mockSummary1 = mock(ReportedCommentSummary.class);
		ReportedCommentSummary mockSummary2 = mock(ReportedCommentSummary.class);

		List<ReportedCommentSummary> mockSummaryList = Arrays.asList(mockSummary1, mockSummary2);
		Page<ReportedCommentSummary> mockPage = new PageImpl<>(mockSummaryList, expectedPageable, 100);

		when(commentReportRepository.findReportedCommentSummary(
			eq(null), eq(null), eq(null), eq(null),
			eq(expectedPageable)
		)).thenReturn(mockPage);

		/// when
		Page<ReportedCommentSummary> resultPage = commentService.getReportedCommentsList(inputPageable, null, null);

		/// then
		assertNotNull(resultPage);
		assertEquals(mockPage, resultPage);

		verify(commentReportRepository).findReportedCommentSummary(
			eq(null), eq(null), eq(null), eq(null),
			eq(expectedPageable)
		);
		verify(commentReportRepository, Mockito.never()).findAll();
	}

	@Test
	@DisplayName("신고된 댓글 목록 조회 - 성공 (검색 조건 있음)")
	void test9() {
		/// given
		Pageable inputPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "reportedAt"));
		String searchTarget = "nickname";
		String searchValue = "testUser";

		String expectedSearchNickname = "testUser";
		String expectedSearchPostTitle = null;
		String expectedSearchCommentBody = null;
		String expectedSearchReportReason = null;

		// Repository가 반환할 Mock 데이터
		ReportedCommentSummary mockSummary1 = mock(ReportedCommentSummary.class);

		List<ReportedCommentSummary> mockSummaryList = Arrays.asList(mockSummary1);
		Page<ReportedCommentSummary> mockPage = new PageImpl<>(mockSummaryList, inputPageable, 1);

		// commentReportRepository.findReportedCommentSummary 메서드 호출 시 mockPage 반환 모킹
		when(commentReportRepository.findReportedCommentSummary(
			eq(expectedSearchNickname), eq(expectedSearchPostTitle), eq(expectedSearchCommentBody),
			eq(expectedSearchReportReason),
			eq(inputPageable)
		)).thenReturn(mockPage);

		/// when
		Page<ReportedCommentSummary> resultPage = commentService.getReportedCommentsList(inputPageable, searchTarget,
			searchValue);

		/// then
		assertNotNull(resultPage);
		assertEquals(mockPage, resultPage);

		verify(commentReportRepository).findReportedCommentSummary(
			eq(expectedSearchNickname), eq(expectedSearchPostTitle), eq(expectedSearchCommentBody),
			eq(expectedSearchReportReason),
			eq(inputPageable)
		);
		verify(commentReportRepository, Mockito.never()).findAll();
	}

	@Test
	@DisplayName("신고된 댓글 목록 조회 - 성공 (정렬 조건 있음)")
	void test10() {
		/// given
		Pageable inputPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "reportCount"));

		// Service 로직에서 정렬 속성 이름이 유효한지 검증 후 Repository로 전달될 Pageable
		Pageable expectedPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "reportCount"));

		// Repository가 반환할 Mock 데이터
		ReportedCommentSummary mockSummary1 = mock(ReportedCommentSummary.class);
		ReportedCommentSummary mockSummary2 = mock(ReportedCommentSummary.class);

		List<ReportedCommentSummary> mockSummaryList = Arrays.asList(mockSummary1, mockSummary2);
		Page<ReportedCommentSummary> mockPage = new PageImpl<>(mockSummaryList, expectedPageable, 50);

		// commentReportRepository.findReportedCommentSummary 메서드 호출 시 mockPage 반환 모킹
		when(commentReportRepository.findReportedCommentSummary(
			eq(null), eq(null), eq(null), eq(null),
			eq(expectedPageable)
		)).thenReturn(mockPage);

		/// when
		Page<ReportedCommentSummary> resultPage = commentService.getReportedCommentsList(inputPageable, null, null);

		/// then
		assertNotNull(resultPage);
		assertEquals(mockPage, resultPage);

		verify(commentReportRepository).findReportedCommentSummary(
			eq(null), eq(null), eq(null), eq(null),
			eq(expectedPageable)
		);
		verify(commentReportRepository, Mockito.never()).findAll();
	}

	@Test
	@DisplayName("신고된 댓글 목록 조회 - 실패 (부적절한 검색 옵션)")
	void test10_1() {
		/// given
		Pageable inputPageable = PageRequest.of(0, 10);
		String invalidSearchTarget = "invalidTarget";
		String searchValue = "someValue";

		/// when & then
		ServiceException e = assertThrows(ServiceException.class, () ->
			commentService.getReportedCommentsList(inputPageable, invalidSearchTarget, searchValue));

		assertEquals("400", e.getCode());
		assertEquals("부적절한 검색 옵션입니다.", e.getMessage());

		verify(commentReportRepository, Mockito.never()).findReportedCommentSummary(
			any(), any(), any(), any(), any()
		);
	}

	@Test
	@DisplayName("신고된 댓글 목록 조회 - 실패 (부적절한 정렬 옵션)")
	void test10_2() {
		/// given
		Pageable inputPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "invalidSortProperty"));

		/// when & then
		ServiceException e = assertThrows(ServiceException.class, () ->
			commentService.getReportedCommentsList(inputPageable, null, null));

		assertEquals("400", e.getCode());
		assertEquals("부적절한 정렬 옵션입니다.", e.getMessage());

		verify(commentReportRepository, Mockito.never()).findReportedCommentSummary(
			any(), any(), any(), any(), any()
		);
	}

	@Test
	@DisplayName("신고된 댓글 숨김 처리 - 성공 (여러 개 ID)")
	void test11() {
		/// given
		List<Long> commentIdsToHide = Arrays.asList(1L, 2L, 3L);

		// Comment Mock 객체들을 명시적으로 생성 및 필요한 메서드 Stubbing
		Member mockWriter1 = mock(Member.class);

		Comment mockComment1 = mock(Comment.class);
		doNothing().when(mockComment1).softDelete();

		Member mockWriter2 = mock(Member.class);

		Comment mockComment2 = mock(Comment.class);
		doNothing().when(mockComment2).softDelete();

		Member mockWriter3 = mock(Member.class);

		Comment mockComment3 = mock(Comment.class);
		doNothing().when(mockComment3).softDelete();

		// commentRepository.findById 호출 시 ID별로 모킹된 comment 객체 반환 설정
		when(commentRepository.findById(eq(1L))).thenReturn(Optional.of(mockComment1));
		when(commentRepository.findById(eq(2L))).thenReturn(Optional.of(mockComment2));
		when(commentRepository.findById(eq(3L))).thenReturn(Optional.of(mockComment3));

		// commentRepository.save 메서드 Mocking
		when(commentRepository.save(any(Comment.class)))
			.thenAnswer(invocation -> invocation.getArguments()[0]);

		/// when
		commentService.hideReportedComment(commentIdsToHide);

		/// then
		verify(commentRepository).findById(eq(1L));
		verify(commentRepository).findById(eq(2L));
		verify(commentRepository).findById(eq(3L));
		verify(mockComment1).softDelete();
		verify(mockComment2).softDelete();
		verify(mockComment3).softDelete();
		verify(commentRepository).save(eq(mockComment1));
		verify(commentRepository).save(eq(mockComment2));
		verify(commentRepository).save(eq(mockComment3));

		verify(commentRepository, Mockito.never()).findAllById(anyList());
		verify(commentReportRepository, Mockito.never())
			.findReportedCommentSummary(any(), any(), any(), any(), any());
		verify(commentReportRepository, Mockito.never()).deleteAllByCommentIdIn(anyList());
	}

	@Test
	@DisplayName("신고된 댓글 숨김 처리 - 실패 (요청 ID 중 댓글 없음)")
	void test11_1() {
		/// given
		List<Long> commentIdsToHide = Arrays.asList(1L, 999L, 3L);

		// Comment Mock 객체들을 명시적으로 생성 및 필요한 메서드 Stubbing
		Comment mockComment1 = mock(Comment.class);
		Comment mockComment2 = mock(Comment.class);

		// commentRepository.findAllById 호출 시, 요청된 ID 개수보다 적게 반환하도록 설정
		Mockito.lenient().when(commentRepository.findAllById(eq(commentIdsToHide)))
			.thenReturn(Arrays.asList(mockComment1, mockComment2));

		/// when & then
		ServiceException e = assertThrows(ServiceException.class, () ->
			commentService.hideReportedComment(commentIdsToHide));

		assertEquals("404", e.getCode());
		assertEquals("존재하지 않는 댓글이 포함되어 있습니다.", e.getMessage());

		verify(mockComment1, Mockito.never()).softDelete();
		verify(mockComment2, Mockito.never()).softDelete();
		verify(commentRepository, Mockito.never()).save(any(Comment.class));

		verify(commentReportRepository, Mockito.never())
			.findReportedCommentSummary(any(), any(), any(), any(), any());
		verify(commentReportRepository, Mockito.never()).deleteAllByCommentIdIn(anyList());
	}

	@Test
	@DisplayName("신고된 댓글 신고 거부 처리 - 성공 (여러 개 ID)")
	void test12() {
		/// given
		List<Long> commentIdsToReject = Arrays.asList(1L, 5L, 10L);

		// Comment Mock 엔티티들을 명시적으로 생성. 필요한 메서드 Stubbing.
		Member mockWriter1 = mock(Member.class);
		Comment mockComment1 = mock(Comment.class);

		Member mockWriter2 = mock(Member.class);
		Comment mockComment2 = mock(Comment.class);

		Member mockWriter3 = mock(Member.class);
		Comment mockComment3 = mock(Comment.class);

		// commentRepository.findAllById 호출 시 모킹된 댓글 목록 반환 설정
		when(commentRepository.findAllById(eq(commentIdsToReject)))
			.thenReturn(Arrays.asList(mockComment1, mockComment2, mockComment3));

		// commentReportRepository.deleteAllByCommentIdIn 메서드 Mocking
		doNothing().when(commentReportRepository)
			.deleteAllByCommentIdIn(eq(commentIdsToReject));

		/// when
		commentService.rejectReportedComment(commentIdsToReject);

		/// then
		verify(commentRepository).findAllById(eq(commentIdsToReject));
		verify(commentReportRepository).deleteAllByCommentIdIn(eq(commentIdsToReject));
		verify(mockComment1, Mockito.never()).softDelete();
		verify(mockComment2, Mockito.never()).softDelete();
		verify(mockComment3, Mockito.never()).softDelete();
		verify(commentRepository, Mockito.never()).save(any(Comment.class));

		verify(commentReportRepository, Mockito.never())
			.findReportedCommentSummary(any(), any(), any(), any(), any());
	}

	@Test
	@DisplayName("신고된 댓글 신고 거부 처리 - 실패 (요청 ID 중 댓글 없음)")
	void test12_1() {
		/// given
		List<Long> commentIdsToReject = Arrays.asList(1L, 999L, 3L);

		// Comment Mock 엔티티들을 명시적으로 생성. 필요한 메서드 Stubbing.
		Member mockWriter1 = mock(Member.class);
		Comment mockComment1 = mock(Comment.class);

		Member mockWriter2 = mock(Member.class);
		Comment mockComment2 = mock(Comment.class);

		// commentRepository.findAllById 호출 시, 요청된 ID 개수보다 적게
		when(commentRepository.findAllById(eq(commentIdsToReject)))
			.thenReturn(Arrays.asList(mockComment1, mockComment2));

		/// when
		ServiceException e = assertThrows(ServiceException.class, () ->
			commentService.rejectReportedComment(commentIdsToReject));

		/// then
		assertEquals("404", e.getCode());
		assertEquals("존재하지 않는 댓글이 포함되어 있습니다.", e.getMessage());

		verify(commentReportRepository, Mockito.never())
			.deleteAllByCommentIdIn(anyList());
		verify(commentRepository, Mockito.never()).save(any(Comment.class));
	}
}
