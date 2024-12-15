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
package test.com.brinvex.ecb;

import com.brinvex.ecb.api.EcbFetchRequest.DepositFacilityRateRequest;
import com.brinvex.ecb.api.EcbFetchRequest.FxRequest;
import com.brinvex.ecb.api.EcbFetchRequest.HICPInflationRequest;
import com.brinvex.ecb.api.EcbFetchResponse.InflationResponse;
import com.brinvex.ecb.api.EcbFetchService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.SequencedMap;

import static com.brinvex.ecb.api.EcbFetchRequest.DepositFacilityRateRequest.EqualSubsequentValuesHandling.MERGE_EQUAL_SUBSEQUENT_VALUES;
import static com.brinvex.ecb.api.EcbFetchRequest.HICPInflationRequest.MomGrowthCalculation.CALCULATE_MOM_GROWTH;
import static com.brinvex.ecb.api.EcbFetchRequest.HICPInflationRequest.YoyGrowthCalculation.CALCULATE_YOY_GROWTH;
import static com.brinvex.ecb.api.EcbFetchRequest.HICPInflationRequest.YoyGrowthCalculation.DONT_CALCULATE_YOY_GROWTH;
import static java.time.LocalDate.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("ExtractMethodRecommender")
class EcbFetchServiceTest {

    @AfterEach
    void slowDownToAvoidHttpGoAway() {
        try {
            Thread.sleep(Duration.ofSeconds(4));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Test
    void fetchFx_oneDay() {
        EcbFetchService ecbFetchService = EcbFetchService.INSTANCE;
        LocalDate day = parse("2024-08-01");
        SequencedMap<LocalDate, Double> fxRates = ecbFetchService.fetch(
                new FxRequest("CZK", day, day));

        assertEquals(1, fxRates.size());
        assertEquals(day, fxRates.firstEntry().getKey());
        assertEquals(25.454, fxRates.get(day));
    }

    @Test
    void fetchFx_recentDays() {
        EcbFetchService ecbFetchService = EcbFetchService.INSTANCE;
        int nDays = 9;
        SequencedMap<LocalDate, Double> fxRates = ecbFetchService.fetch(
                new FxRequest("CZK", LocalDate.now().minusDays(nDays)));
        int size = fxRates.size();
        assertTrue(size >= 3);
        assertTrue(size <= nDays - 1);
    }

    @Test
    void fetchDepoFacilityInterRates_oneDay() {
        EcbFetchService ecbFetchService = EcbFetchService.INSTANCE;
        LocalDate day = parse("2024-08-01");
        SequencedMap<LocalDate, Double> rates = ecbFetchService.fetch(
                new DepositFacilityRateRequest(day, day));

        assertEquals(1, rates.size());
        assertEquals(day, rates.firstEntry().getKey());
        assertEquals(3.75, rates.get(day));
    }

    @Test
    void fetchDepoFacilityInterRates_oneYear() {
        EcbFetchService ecbFetchService = EcbFetchService.INSTANCE;
        LocalDate startDayIncl = parse("2023-01-01");
        LocalDate endDayIncl = parse("2023-12-31");
        SequencedMap<LocalDate, Double> rates = ecbFetchService.fetch(
                new DepositFacilityRateRequest(startDayIncl, endDayIncl));

        assertEquals(365, rates.size());
        assertEquals(startDayIncl, rates.firstEntry().getKey());
        assertEquals(2.0, rates.get(startDayIncl));
        assertEquals(4.0, rates.get(endDayIncl));
    }

    @Test
    void fetchDepoFacilityInterRates_merge() {
        EcbFetchService ecbFetchService = EcbFetchService.INSTANCE;
        LocalDate startDayIncl = parse("2021-01-01");
        LocalDate endDayIncl = parse("2021-12-31");
        SequencedMap<LocalDate, Double> rates = ecbFetchService.fetch(
                new DepositFacilityRateRequest(startDayIncl, endDayIncl, MERGE_EQUAL_SUBSEQUENT_VALUES));

        assertEquals(1, rates.size());
        assertEquals(startDayIncl, rates.firstEntry().getKey());
        assertEquals(-0.5, rates.get(startDayIncl));
        assertEquals(-0.5, rates.get(startDayIncl));
    }

    @Test
    void fetchDepoFacilityInterRates_now() {
        EcbFetchService ecbFetchService = EcbFetchService.INSTANCE;
        LocalDate yesterday = LocalDate.now().minusDays(1);
        SequencedMap<LocalDate, Double> rates = ecbFetchService.fetch(
                new DepositFacilityRateRequest(yesterday, yesterday));

        assertEquals(1, rates.size());
        assertEquals(yesterday, rates.firstEntry().getKey());
    }

    @Test
    void fetchInflation_oneMonth() {
        EcbFetchService ecbFetchService = EcbFetchService.INSTANCE;
        LocalDate day = parse("2024-07-01");
        SequencedMap<LocalDate, InflationResponse> results = ecbFetchService.fetch(
                new HICPInflationRequest(day, day));

        assertEquals(1, results.size());
        assertEquals(day, results.firstEntry().getKey());
        assertEquals(126.54, results.get(day).cpi().doubleValue());
    }

    @Test
    void fetchInflation_oneYear() {
        EcbFetchService ecbFetchService = EcbFetchService.INSTANCE;
        LocalDate middleMonthStartDayIncl = parse("2023-07-15");
        LocalDate middleMonthEndDayIncl = parse("2024-06-15");
        SequencedMap<LocalDate, InflationResponse> results = ecbFetchService.fetch(
                new HICPInflationRequest(middleMonthStartDayIncl, middleMonthEndDayIncl));

        assertEquals(12, results.size());
        assertEquals(parse("2023-07-01"), results.firstEntry().getKey());
        assertEquals(parse("2024-06-01"), results.lastEntry().getKey());

    }

    @Test
    void fetchInflation_calcMom() {
        EcbFetchService ecbFetchService = EcbFetchService.INSTANCE;
        LocalDate middleMonthStartDayIncl = parse("2023-07-15");
        LocalDate middleMonthEndDayIncl = parse("2024-06-15");
        SequencedMap<LocalDate, InflationResponse> results = ecbFetchService.fetch(
                new HICPInflationRequest(
                        middleMonthStartDayIncl,
                        middleMonthEndDayIncl,
                        CALCULATE_MOM_GROWTH,
                        DONT_CALCULATE_YOY_GROWTH
                ));

        assertEquals(12, results.size());
        assertEquals(parse("2023-07-01"), results.firstEntry().getKey());
        assertEquals(parse("2024-06-01"), results.lastEntry().getKey());
        assertEquals(0, results.get(parse("2024-06-01")).momGrowthFactor().compareTo(new BigDecimal("1.002138")));

    }

    @Test
    void fetchInflation_calcYoy() {
        EcbFetchService ecbFetchService = EcbFetchService.INSTANCE;
        LocalDate middleMonthStartDayIncl = parse("2023-07-15");
        LocalDate middleMonthEndDayIncl = parse("2024-06-15");
        SequencedMap<LocalDate, InflationResponse> results = ecbFetchService.fetch(
                new HICPInflationRequest(
                        middleMonthStartDayIncl,
                        middleMonthEndDayIncl,
                        CALCULATE_MOM_GROWTH,
                        CALCULATE_YOY_GROWTH
                ));

        assertEquals(12, results.size());
        assertEquals(parse("2023-07-01"), results.firstEntry().getKey());
        assertEquals(parse("2024-06-01"), results.lastEntry().getKey());
        assertEquals(0, results.get(parse("2024-06-01")).momGrowthFactor().compareTo(new BigDecimal("1.002138")));
        assertEquals(0, results.get(parse("2024-06-01")).yoyGrowthFactor().compareTo(new BigDecimal("1.025188")));

        assertEquals(0, results.get(parse("2024-01-01")).yoyGrowthFactor().setScale(3, RoundingMode.HALF_UP).compareTo(new BigDecimal("1.028")));
        assertEquals(0, results.get(parse("2024-02-01")).yoyGrowthFactor().setScale(3, RoundingMode.HALF_UP).compareTo(new BigDecimal("1.026")));
        assertEquals(0, results.get(parse("2024-03-01")).yoyGrowthFactor().setScale(3, RoundingMode.HALF_UP).compareTo(new BigDecimal("1.024")));
        assertEquals(0, results.get(parse("2024-04-01")).yoyGrowthFactor().setScale(3, RoundingMode.HALF_UP).compareTo(new BigDecimal("1.024")));
        assertEquals(0, results.get(parse("2024-05-01")).yoyGrowthFactor().setScale(3, RoundingMode.HALF_UP).compareTo(new BigDecimal("1.026")));
        assertEquals(0, results.get(parse("2024-06-01")).yoyGrowthFactor().setScale(3, RoundingMode.HALF_UP).compareTo(new BigDecimal("1.025")));

    }
}