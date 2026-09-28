package com.miniproject1.ai.program;

import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 공고 조회 흐름을 처리하는 서비스입니다.
 */
@Service
public class ProgramService {

    private final ProgramRepository programRepository;

    public ProgramService(ProgramRepository programRepository) {
        this.programRepository = programRepository;
    }

    /**
     * 전달받은 공고 ID로 공고 한 건을 조회합니다.
     */
    public Optional<ProgramResponse> findByPblancId(String pblancId) {
        return programRepository.findByPblancId(pblancId);
    }
}
