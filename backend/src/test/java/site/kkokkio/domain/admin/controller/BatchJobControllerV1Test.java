package site.kkokkio.domain.admin.controller;

import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.global.auth.AuthChecker;
import site.kkokkio.global.auth.CustomUserDetails;
import site.kkokkio.global.auth.CustomUserDetailsService;
import site.kkokkio.global.config.SecurityConfig;
import site.kkokkio.global.enums.MemberRole;
import site.kkokkio.global.util.JwtUtils;

@WebMvcTest(controllers = BatchJobControllerV1.class)
@Import({SecurityConfig.class})
class BatchJobControllerV1Test {
	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	AuthChecker authChecker;

	@MockitoBean
	private JobLauncher jobLauncher;

	@MockitoBean
	private Job trendToPostJob;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	@MockitoBean
	private RedisTemplate<String, String> redisTemplate;

	@MockitoBean
	private JwtUtils jwtUtils;

	@Test
	@DisplayName("배치 작업 성공 - Admin")
	void batchJobControllerV1Test1() throws Exception {
		Member member = mock(Member.class);
		when(member.getRole()).thenReturn(MemberRole.ADMIN);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/batch/run/trend")
				.with(user(new CustomUserDetails("test@email.com", member.getRole().toString(), true)))
				.with(csrf())
				.contentType(APPLICATION_JSON))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("배치 작업 실패 - USER 403")
	void batchJobControllerV1Test2() throws Exception {
		Member member = Member.builder()
			.email("user@test.com")
			.role(MemberRole.USER)
			.build();
		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/batch/run/trend")
				.with(user(new CustomUserDetails(member.getEmail(), member.getRole().toString(), true)))
				.with(csrf())
				.contentType(APPLICATION_JSON))
			.andExpect(authenticated().withRoles("USER"))
			.andExpect(status().isForbidden());
	}

}
