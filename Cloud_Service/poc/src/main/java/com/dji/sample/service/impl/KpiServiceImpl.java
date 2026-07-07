package com.dji.sample.service.impl;

import com.dji.sample.dto.kpi.response.KpiSummaryResponse;
import com.dji.sample.service.KpiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KpiServiceImpl implements KpiService {

    @Override
    public KpiSummaryResponse getSummary() {
        return null;
    }
}