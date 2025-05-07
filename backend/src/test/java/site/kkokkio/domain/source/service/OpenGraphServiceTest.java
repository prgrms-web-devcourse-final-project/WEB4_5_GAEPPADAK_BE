package site.kkokkio.domain.source.service;

import static org.assertj.core.api.AssertionsForInterfaceTypes.*;
import static org.mockito.BDDMockito.*;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import site.kkokkio.domain.source.entity.Source;
import site.kkokkio.domain.source.repository.SourceRepository;

@ExtendWith(MockitoExtension.class)
class OpenGraphServiceTest {

    @InjectMocks
    private OpenGraphService openGraphService;

    @Mock
    private SourceRepository sourceRepository;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(openGraphService, "mockEnabled", false);
    }

    @Test
    @DisplayName("enrichAsync - 정상 추출")
    void enrichAsync_success() throws Exception {
        // given
        Source source = Source.builder()
            .normalizedUrl("https://example.com")
            .build();

        String mockHtml = """
            <html><head>
            <meta property="og:image" content="https://cdn.example.com/image.jpg" />
            </head><body></body></html>
        """;

        Document mockDoc = Jsoup.parse(mockHtml, "https://example.com");

		try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
			Connection mockConnection = mock(Connection.class);
			given(mockConnection.timeout(anyInt())).willReturn(mockConnection);
			given(mockConnection.get()).willReturn(mockDoc);

			jsoupMock.when(() -> Jsoup.connect("https://example.com")).thenReturn(mockConnection);

			// when
			openGraphService.enrichAsync(source);

			// then
			assertThat(source.getThumbnailUrl()).isEqualTo("https://cdn.example.com/image.jpg");
			then(sourceRepository).should().save(source);
		}
    }

    @Test
    @DisplayName("enrichAsync - 실패")
    void enrichAsync_jsoupFails() {
        // given
        Source source = Source.builder()
            .normalizedUrl("https://bad.url")
            .build();

        try (MockedStatic<Jsoup> jsoupMock = Mockito.mockStatic(Jsoup.class)) {
            jsoupMock.when(() -> Jsoup.connect("https://bad.url"))
                .thenThrow(new RuntimeException("연결 실패"));

            // when
            openGraphService.enrichAsync(source);

            // then
            then(sourceRepository).shouldHaveNoInteractions();
        }
    }
}