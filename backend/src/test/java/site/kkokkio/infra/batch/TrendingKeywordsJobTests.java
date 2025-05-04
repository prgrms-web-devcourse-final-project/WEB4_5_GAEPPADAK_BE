//package site.kkokkio.infra.batch;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.batch.test.context.SpringBatchTest;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//
//@Slf4j
//@SpringBatchTest
//@SpringBootTest
//@ActiveProfiles("test")
//public class TrendingKeywordsJobTests {
//    @Autowired
//    private JobLauncherTestUtils jobLauncherTestUtils;
//
//    @Autowired
//    private JobRepositoryTestUtils jobRepositoryTestUtils;
//
//    @Autowired
//    @Qualifier("trendToPostJob")
//    private Job trendingKeywordsJob;
//
//    @Autowired
//    private GoogleTrendsRssService googleTrendsRssService;
//
//    @Configuration
//    static class TestConfig {
//        // Job이 의존하는 외부 API 포트들을 Mock 객체로 만들어 빈으로 등록
//        @Bean
//        NewsApiPort newsApiPort() {
//            return mock(NewsApiPort.class);
//        }
//
//        @Bean
//        VideoApiPort videoApiPort() {
//            return mock(VideoApiPort.class);
//        }
//
//        @Bean
//        GoogleTrendsRssService googleTrendsRssService() {
//            return mock(GoogleTrendsRssService.class);
//        }
//    }
//
//    @Autowired
//    private NewsApiPort newsApiPort;
//
//    @Autowired
//    private VideoApiPort videoApiPort;
//
//    @Autowired private SourceRepository sourceRepository;
//    @Autowired private KeywordSourceRepository keywordSourceRepository;
//
//    // Job 실행 시 메타데이터 정리
//    @BeforeEach
//    public void clearJobExecutions() {
//        jobRepositoryTestUtils.removeJobExecutions();
//    }
//
//    @Test
//    @DisplayName("trendingKeywordsJob 성공 시나리오")
//    public void testTrendingKeywordsJob_SuccessfulRun() throws Exception {
//        /// given
//        // Mock GoogleTrendsRssService가 특정 키워드 목록을 반환하도록 설정
//        List<Keyword> mockKeywords = Arrays.asList(
//                Keyword.builder().id(1L).text("TestKeyword1").build(),
//                Keyword.builder().id(2L).text("TestKeyword2").build()
//        );
//        when(googleTrendsRssService.getTrendingKeywordsFromRss())
//                .thenReturn(mockKeywords);
//
//        // Mock NewsApiPort가 특정 뉴스 목록을 반환하도록 설정
//        List<NewsDto> mockNewsList1 = Arrays.asList(
//                NewsDto.builder().title("News Title 1-1").link("http://link1-1.com")
//                        .pubDate(LocalDateTime.now()).build(),
//                NewsDto.builder().title("News Title 1-2").link("http://link1-2.com")
//                        .pubDate(LocalDateTime.now()).build()
//        );
//        List<NewsDto> mockNewsList2 = Arrays.asList(
//                NewsDto.builder().title("News Title 2-1").link("http://link2-1.com")
//                        .pubDate(LocalDateTime.now()).build()
//        );
//        when(newsApiPort.fetchNews(eq("TestKeyword1"), any(), any(), any()))
//                .thenReturn(Mono.just(mockNewsList1));
//        when(newsApiPort.fetchNews(eq("TestKeyword2"), any(), any(), any()))
//                .thenReturn(Mono.just(mockNewsList2));
//
//        // Mock VideoApiPort가 특정 영상 목록을 반환하도록 설정
//        List<VideoDto> mockVideoList1 = Arrays.asList(
//                VideoDto.builder().id("video1-1").title("Video Title 1-1")
//                        .publishedAt(LocalDateTime.now()).build()
//        );
//        when(videoApiPort.fetchVideos(eq("TestKeyword1"), anyInt()))
//                .thenReturn(Mono.just(mockVideoList1));
//        when(videoApiPort.fetchVideos(eq("TestKeyword2"), anyInt()))
//                .thenReturn(Mono.just(Collections.emptyList()));
//
//        // Job 실행 시 JobParameter 설정
//        JobParameters jobParameters = new JobParametersBuilder()
//                .addString("runTime", LocalDateTime.now().toString())
//                .toJobParameters();
//
//        /// when
//        // Job 실행 시 JobParameter 설정
//        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
//
//        /// then
//        // Job 실행 결과 검증 (Job이 성공적으로 완료되었는지, 데이터가 예상대로 저장되었는지 등)
//        // Job 실행 상태가 COMPLETED인지 확인
//        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
//
//        // 데이터베이스에 데이터가 예상대로 저장되었는지 확인
//        assertThat(sourceRepository.count()).isGreaterThan(0);
//        assertThat(keywordSourceRepository.count()).isGreaterThan(0);
//
//        log.info("Job 실행 결과: {}", jobExecution.getExitStatus());
//    }
//}
