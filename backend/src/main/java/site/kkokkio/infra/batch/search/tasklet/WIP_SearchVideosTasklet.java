package site.kkokkio.infra.batch.search.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.kkokkio.domain.keyword.entity.Keyword;
import site.kkokkio.domain.keyword.repository.KeywordRepository;
import site.kkokkio.domain.source.dto.VideoDto;
import site.kkokkio.domain.source.entity.KeywordSource;
import site.kkokkio.domain.source.entity.Source;
import site.kkokkio.domain.source.port.out.VideoApiPort;
import site.kkokkio.domain.source.repository.KeywordSourceRepository;
import site.kkokkio.domain.source.repository.SourceRepository;
import site.kkokkio.global.enums.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional
public class SearchVideosTasklet implements Tasklet {

    private final VideoApiPort videoApiPort;
    private final SourceRepository sourceRepository;
    private final KeywordSourceRepository keywordSourceRepository;
    private final KeywordRepository keywordRepository;

    // Task 1에서 Job Execution Context에 저장한 키워드 정보를
    // 가져오기 위한 변수 (JobExecutionContext 접근 필요)
    // 실제 Task 1 구현에 맞춰 아래 변수들을 채우거나,
    // execute 메소드 안에서 직접 JobContext를 사용해야 함
    // 파이프라인 문서의 'fetchTrendingKeywordsContext' 구조를 참고하되, 실제 구현 확인이 필요

    // Task 1이 JobContext에 List<Long> topKeywordIds, List<String> topKeywordTexts로 저장한다고 가정

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
        throws Exception {
        log.info(">>>>>>>>>>> searchVideosStep 시작");

        // Job Execution Context에서 Task 1이 저장한 상위 키워드 목록 가져오기
        JobExecution jobExecution = chunkContext.getStepContext().getStepExecution().getJobExecution();
        ExecutionContext jobExecutionContext = jobExecution.getExecutionContext();

        // TODO: Task 1에서 Job Execution Context에 저장한 상위 키워드 목록 로직 구현
        // Task 1에서 Job Execution Context에 저장한 상위 키워드 목록 로직 구현
        List<Long> topKeywordIds = (List<Long>) jobExecutionContext.get("TopKeywordIds");
        List<String> topKeywortTexts = (List<String>) jobExecutionContext.get("TopKeywords");

        // API 호출 시 가져올 개수
        int apiCallCount = 10;

        // 테스트 목적 임시 키워드 목록 (위 JobContext 로딩 부분을 주석 처리하고 아래의 코드 주석 해제)
//        log.warn("Test Mode: Job Execution Context 대신 임시 키워드 목록 사용");
//        topKeywordIds = List.of(1L, 2L, 3L);
//        topKeywortTexts = List.of("테스트 키워드 1", "테스트 키워드 2", "테스트 키워드 3");

        // Job Execution Context에서 가져온 키워드 목록이 유효하지 않으면 Step 종료 (임시 로딩 시에도 체크)
        if (topKeywordIds == null || topKeywortTexts == null ||
                topKeywordIds.size() != topKeywortTexts.size() || topKeywordIds.isEmpty()) {
            log.warn("Job Execution Context에서 상위 키워드 목록을 가져오지 못했거나 비어있습니다.");
            return RepeatStatus.FINISHED;
        }

        // TODO: searchSourceContext 맵 구조 결정 및 초기화
        // searchSourceContext 맵 구조 결정 및 초기화
        // 파이프라인 문서의 'searchSourceContext' 구조를 참고
        // 이 맵에 키워드별 수집 결과 등을 담아 다음 Step으로 전달 예정

        // 상위 키워드 목록 순회 및 API 호출, 데이터 변환 및 저장
        List<Source> allCollectedSources = new ArrayList<>();

        for (int i = 0; i < topKeywordIds.size(); i++) {
            Long currentKeywordId = topKeywordIds.get(i);
            String currentKeywordText = topKeywortTexts.get(i);

            // 해당 키워드 엔티티 조회 (KeywordSource 생성 시 필요)
            // *KeywordRepository에서 findById로 조회 실패 시 어떻게 처리할지 결정*
            Optional<Keyword> keywordOptional = keywordRepository.findById(currentKeywordId);

            if (keywordOptional.isEmpty()) {
                log.warn("Keyword ID {} 에 해당하는 Keyword 엔티티를 찾을 수 없습니다. " +
                        "해당 키워드 스킵.", currentKeywordId);
                continue;
            }

            Keyword currentKeyword = keywordOptional.get();

            log.info("키워드 '{}' (ID: {}) 에 대한 YouTube 영상 검색 시작", currentKeywordText, currentKeywordId);

            // 이 키워드로 새로 저장되거나 이미 존재했던 Source 목록
            List<Source> newlySavedSources = new ArrayList<>();

            try {
                // Youtube 어댑터 호출 및 결과 대기 (Block)
                // fetchVideos는 Mono<List<VideoDto>>를 반환하므로, .block()으로 결과 대기
                List<VideoDto> videoDtos = videoApiPort.fetchVideos(currentKeywordText, apiCallCount).block();

                // 어댑터 호출 결과가 비어있으면 다음 키워드로
                if (videoDtos == null || videoDtos.isEmpty()) {
                    log.info("키워드 '{}' 에 대해 수집된 YouTube 영상 결과가 없습니다.", currentKeywordText);
                    // Context 업데이트 시 해당 키워드에 대한 결과 없음을 기록
                    continue;
                }

                log.info("키워드 '{}' 에 대해 {} 개의 YouTube 영상 수집 완료.", currentKeywordText, videoDtos.size());

                // VideoDto 목록을 Source 엔티티 목록으로 변환
                List<Source> sourcesToProcess = videoDtos.stream()
                        .map(videoDto -> videoDto.toEntity(Platform.YOUTUBE))
                        .collect(Collectors.toList());

                // Source 엔티티 저장 (INSERT IGNORE 로직 포함) 및 KeywordSource 엔티티 저장
                List<Source> keywordSourcesToSave = new ArrayList<>();

                for (Source source : sourcesToProcess) {
                    try {
                        // SourceRepository에 INSERT IGNORE 기능을 가진 메소드를 호출하여 Source 저장
                        // 이 메소드는 성공적으로 저장되거나 이미 존재했던 경우 해당 Source 엔티티를 반환해야 됨
                        Source savedSources = sourceRepository.insertIgnoreReturning(source);

                        if (savedSources != null) {
                            newlySavedSources.add(savedSources);

                            // KeywordSource 엔티티 생성
                            KeywordSource keywordSource = KeywordSource.builder()
                                    .keyword(currentKeyword)
                                    .source(savedSources)
                                    // TODO: KeywordSource 엔티티 Builder에 필요한 필드 추가 확인 (BaseTimeEntity 등)
                                    .build();

                            keywordSourcesToSave.add(keywordSource);

                        } else {
                            // insertIgnoreReturning이 null을 반환하는 경우
                            log.warn("Source 저장 또는 조회가 실패했습니다 (키워드 ID: {}, Fingerprint: {}). " +
                                    "KeywordSource 생성 스킵.", currentKeyword, source.getFingerprint());
                        }

                    } catch (Exception e) {
                        // Source 또는 KeywordSource 저장 중 발생할 수 있는 예외 처리
                        log.error("Source 또는 KeywordSource 저장 중 오류 발생 (키워드 ID: {}, Fingerprint: {}): {}",
                                currentKeyword, source.getFingerprint(), e.getMessage());

                        // @Transactional 사용 시 예외 발생하면 자동 롤백
                        // 만약 중복 키 예외만 무시하고 싶다면, 해당 예외만 catch 하고 넘어가도록 상세 예외 타입 지정 필요

                        throw e;
                    }
                }

                // KeywordSourceRepository에 INSERT IGNORE 기능을 가진 메소드를 호출하여 Bulk 저장
                keywordSourceRepository.bulkInsertIgnore(keywordSourcesToSave);
                log.info("키워드 '{}' (ID: {}) 에 대해 {} 개의 KeywordSource 저장 시도 완료.",
                        currentKeywordText, currentKeywordId, newlySavedSources.size());

                // 이 키워드에서 수집 및 처리된 Source 목록을 전체 목록에 추가
                allCollectedSources.addAll(keywordSourcesToSave);

                // searchSourceContext 맵에 이 키워드(currentKeywordId)에 대한 수집 결과 정보 추가

            } catch (Exception e) {
                // 어댑터 호출, 블록킹, 데이터 변환 중 발생할 수 있는 예외 처리
                log.error("키워드 '{}' (ID: {}) 에 대한 YouTube 영상 검색/처리 중 오류 발생: {}",
                        currentKeywordText, currentKeywordId, e.getMessage());

                // TODO: 오류 발생 시 해당 키워드 처리 방식 결정 (계속 진행할지, Step 실패로 볼지 등)

            }
        }

        // Step 완료 전 searchSourceContext를 Job Execution Context에 저장
        log.info(">>>>>>>>>>> searchVideosStep 완료");

        return RepeatStatus.FINISHED;
    }
}
