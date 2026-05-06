package com.example.homeprotect.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.homeprotect.client.VectorDbClient;
import com.example.homeprotect.dto.response.ContractClauseResult.Clause;
import com.example.homeprotect.dto.response.PrecedentSearchResult;
import com.example.homeprotect.dto.response.PrecedentSearchResult.PrecedentMatch;
import com.example.homeprotect.filter.RiskClauseAnalyzer;
import com.example.homeprotect.util.RedisUtil;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class PrecedentService {

    private static final double MIN_SCORE = 0.80;
    private static final int TOP_K = 3;

    private final RedisUtil redisUtil;
    private final RiskClauseAnalyzer riskClauseAnalyzer;
    private final VectorDbClient vectorDbClient;

    public PrecedentService(
        RedisUtil redisUtil,
        RiskClauseAnalyzer riskClauseAnalyzer,
        VectorDbClient vectorDbClient
    ) {
        this.redisUtil = redisUtil;
        this.riskClauseAnalyzer = riskClauseAnalyzer;
        this.vectorDbClient = vectorDbClient;
    }

    public Mono<List<PrecedentSearchResult>> search(String documentId) {
        return redisUtil.getClauseResult(documentId)
            .flatMap(clauseResult ->
                // Redis 캐시 있으면 재사용, 없으면 Claude 호출 후 저장
                redisUtil.getRiskClauses(documentId)
                    .switchIfEmpty(
                        riskClauseAnalyzer.analyze(clauseResult.getClauses())
                            .flatMap(riskClauses ->
                                redisUtil.saveRiskClauses(documentId, riskClauses)
                                    .thenReturn(riskClauses)
                            )
                    )
                    .flatMap(riskClauses -> {
                        if (riskClauses.isEmpty()) {
                            return Mono.just(List.of());
                        }
                        return Flux.fromIterable(riskClauses)
                            .flatMapSequential(clause ->
                                vectorDbClient.search(clause.getOriginalText(), TOP_K)
                                    .map(matches -> buildResult(clause, matches))
                                    .onErrorResume(e -> Mono.just(buildResult(clause, List.of())))
                            )
                            .collectList();
                    })
            );
    }

    private PrecedentSearchResult buildResult(Clause clause, List<PrecedentMatch> matches) {
        List<PrecedentMatch> qualified = matches.stream()
            .filter(m -> m.getScore() >= MIN_SCORE)
            .toList();

        String level = qualified.isEmpty() ? "caution" : "danger";

        return PrecedentSearchResult.builder()
            .clauseIndex(clause.getIndex())
            .section(clause.getSection())
            .originalText(clause.getOriginalText())
            .level(level)
            .matches(qualified)
            .build();
    }
}
