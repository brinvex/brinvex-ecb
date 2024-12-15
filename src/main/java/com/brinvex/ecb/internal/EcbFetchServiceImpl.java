/*
 * Copyright © 2024 Brinvex (dev1@brinvex.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.brinvex.ecb.internal;

import com.brinvex.ecb.api.EcbFetchRequest.DepositFacilityRateRequest;
import com.brinvex.ecb.api.EcbFetchRequest.FxRequest;
import com.brinvex.ecb.api.EcbFetchRequest.HICPInflationRequest;
import com.brinvex.ecb.api.EcbFetchResponse.InflationResponse;
import com.brinvex.ecb.api.EcbFetchService;
import com.brinvex.ecb.api.HttpClientFacade;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.SequencedMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElseGet;

@SuppressWarnings({"DuplicatedCode", "UnnecessaryLocalVariable"})
public class EcbFetchServiceImpl implements EcbFetchService {

    private final String baseUrl = "https://data-api.ecb.europa.eu/service/data";

    private static class Lazy {
        static final Pattern DAY_VALUE_PATTERN = Pattern.compile("ObsDimension value=\"(\\d{4}-\\d{2}-\\d{2})\"/>\\s*<generic:ObsValue value=\"([^\"]*)\"/>");
        static final Pattern MONTH_VALUE_PATTERN = Pattern.compile("ObsDimension value=\"(\\d{4}-\\d{2})\"/>\\s*<generic:ObsValue value=\"([^\"]*)\"/>");
    }

    private final HttpClientFacade httpClientFacade;

    public EcbFetchServiceImpl(HttpClientFacade httpClientFacade) {
        this.httpClientFacade = httpClientFacade;
    }

    @Override
    public SequencedMap<LocalDate, Double> fetch(FxRequest fxReq) {
        String quoteCcy = requireNonNull(fxReq.quoteCcy(), "quoteCcy must not be null");
        if ("EUR".equals(quoteCcy)) {
            throw new IllegalArgumentException("quotedCcy must not be EUR");
        }
        LocalDate startDayIncl = requireNonNull(fxReq.startDayIncl(), "startDayIncl must not be null");
        LocalDate endDayIncl = requireNonNullElseGet(fxReq.endDayIncl(), LocalDate::now);

        String url = baseUrl + "/EXR/D.%s.EUR.SP00.A?startPeriod=%s&endPeriod=%s&detail=dataonly"
                .formatted(quoteCcy, startDayIncl, endDayIncl);

        String rawContent = fetchRaw(url);

        LinkedHashMap<LocalDate, Double> results = parseDayValues(rawContent, LinkedHashMap::new);
        return results;
    }

    @Override
    public SequencedMap<LocalDate, Double> fetch(DepositFacilityRateRequest depoFacilityRateReq) {
        LocalDate startDayIncl = requireNonNull(depoFacilityRateReq.startDayIncl());
        LocalDate endDayIncl = requireNonNullElseGet(depoFacilityRateReq.endDayIncl(), LocalDate::now);

        String url = baseUrl + "/FM/D.U2.EUR.4F.KR.DFR.LEV?startPeriod=%s&endPeriod=%s&detail=dataonly"
                .formatted(startDayIncl, endDayIncl);

        String rawContent = fetchRaw(url);

        LinkedHashMap<LocalDate, Double> results = parseDayValues(rawContent, LinkedHashMap::new);
        switch (depoFacilityRateReq.equalSubsequentValuesHandling()) {
            case MERGE_EQUAL_SUBSEQUENT_VALUES -> CollectionUtil.removeAdjacentDuplicates(results.values(), Double::equals);
            case DONT_MERGE_EQUAL_SUBSEQUENT_VALUES -> { /* no-op */ }
        }
        return results;
    }

    @Override
    public SequencedMap<LocalDate, InflationResponse> fetch(HICPInflationRequest hicpReq) {
        LocalDate startDayIncl = requireNonNull(hicpReq.startDayIncl()).withDayOfMonth(1);
        LocalDate endDayIncl = requireNonNullElseGet(hicpReq.endDayIncl(), LocalDate::now).withDayOfMonth(1).plusMonths(1).minusDays(1);

        boolean calculateMomGrowthFactor = switch (hicpReq.momGrowthCalculation()) {
            case CALCULATE_MOM_GROWTH -> true;
            case DONT_CALCULATE_MOM_GROWTH -> false;
        };
        boolean calculateYoyGrowthFactor = switch (hicpReq.yoyGrowthCalculation()) {
            case CALCULATE_YOY_GROWTH -> true;
            case DONT_CALCULATE_YOY_GROWTH -> false;
        };
        LocalDate extStartDayIncl;
        if (calculateYoyGrowthFactor) {
            extStartDayIncl = startDayIncl.minusMonths(12);
        } else if (calculateMomGrowthFactor) {
            extStartDayIncl = startDayIncl.minusMonths(1);
        } else {
            extStartDayIncl = startDayIncl;
        }
        String url = baseUrl + "/ICP/M.U2.N.000000.4.INX?startPeriod=%s&endPeriod=%s&detail=dataonly"
                .formatted(extStartDayIncl, endDayIncl);

        String rawContent = fetchRaw(url);

        LinkedHashMap<LocalDate, Double> cpiResults = parseMonthValues(rawContent, LinkedHashMap::new);

        LinkedHashMap<LocalDate, InflationResponse> results = new LinkedHashMap<>();

        for (Map.Entry<LocalDate, Double> e : cpiResults.entrySet()) {
            LocalDate yearMonth = e.getKey();
            if (yearMonth.isBefore(startDayIncl)) {
                continue;
            }
            BigDecimal cpiDecimal = BigDecimal.valueOf(e.getValue());
            BigDecimal yoyGrowthFactor;
            BigDecimal momGrowthFactor;
            if (calculateYoyGrowthFactor) {
                Double prevYearCpi = cpiResults.get(yearMonth.minusMonths(12));
                yoyGrowthFactor = cpiDecimal.divide(BigDecimal.valueOf(prevYearCpi), 6, RoundingMode.HALF_UP);
            } else {
                yoyGrowthFactor = null;
            }
            if (calculateMomGrowthFactor) {
                Double prevMonthCpi = cpiResults.get(yearMonth.minusMonths(1));
                momGrowthFactor = cpiDecimal.divide(BigDecimal.valueOf(prevMonthCpi), 6, RoundingMode.HALF_UP);
            } else {
                momGrowthFactor = null;
            }
            results.put(yearMonth, new InflationResponse(cpiDecimal, momGrowthFactor, yoyGrowthFactor));
        }
        return results;
    }

    protected String fetchRaw(String url) {
        try {
            HttpClientFacade.Response resp = httpClientFacade.doGet(URI.create(url), Collections.emptyMap(), UTF_8);
            int status = resp.status();
            if (200 <= status && status <= 299) {
                return resp.body();
            } else {
                throw new IOException("Unexpected response: %s, %s".formatted(resp, url));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected <MAP extends Map<LocalDate, Double>> MAP parseDayValues(String content, Supplier<MAP> mapSupplier) {
        MAP results = mapSupplier.get();
        new Scanner(content)
                .findAll(Lazy.DAY_VALUE_PATTERN)
                .forEach(mr -> results.put(
                        LocalDate.parse(mr.group(1)),
                        Double.valueOf(mr.group(2))
                ));
        return results;
    }

    protected <MAP extends Map<LocalDate, Double>> MAP parseMonthValues(String content, Supplier<MAP> mapSupplier) {
        MAP results = mapSupplier.get();
        new Scanner(content)
                .findAll(Lazy.MONTH_VALUE_PATTERN)
                .forEach(mr -> results.put(
                        LocalDate.parse(mr.group(1) + "-01"),
                        Double.valueOf(mr.group(2))
                ));
        return results;
    }


}
