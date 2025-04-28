package site.kkokkio.domain.member.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Member API", description = "회원 관련 기능을 제공")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class MemberController {
}
