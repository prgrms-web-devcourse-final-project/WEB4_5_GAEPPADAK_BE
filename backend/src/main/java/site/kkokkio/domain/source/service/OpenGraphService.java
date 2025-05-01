package site.kkokkio.domain.source.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.kkokkio.domain.source.entity.Source;
import site.kkokkio.domain.source.repository.SourceRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class OpenGraphService {

    @Value("${mock.enabled}")
	private Boolean mockEnabled;

    private final SourceRepository sourceRepository;

    /**
     * 비동기로 OpenGraph 정보 보강 (현재는 thumbnail 정보만 가공)
     */
    @Async("asyncExecutor")
    @Transactional
    public void enrichAsync(Source source) {
        if (!mockEnabled) {
            try {
                Document doc = Jsoup.connect(source.getNormalizedUrl()).timeout(2000).get();

                String thumbnail = doc.select("meta[property=og:image]").attr("content");
                if (!thumbnail.isBlank()) {
                    source.setThumbnailUrl(thumbnail);
                }

                sourceRepository.save(source);
            } catch (Exception e) {
                log.warn("OpenGraph 정보 추출 실패: {}", source.getNormalizedUrl());
            }
        }
    }
}
