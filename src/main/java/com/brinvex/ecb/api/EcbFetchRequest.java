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
package com.brinvex.ecb.api;

import java.time.LocalDate;

import static com.brinvex.ecb.api.EcbFetchRequest.HICPInflationRequest.MomGrowthCalculation.DONT_CALCULATE_MOM_GROWTH;
import static com.brinvex.ecb.api.EcbFetchRequest.HICPInflationRequest.YoyGrowthCalculation.DONT_CALCULATE_YOY_GROWTH;

public sealed interface EcbFetchRequest {

    record FxRequest(
            String quoteCcy,
            LocalDate startDayIncl,
            LocalDate endDayIncl
    ) implements EcbFetchRequest {
        public FxRequest(String quoteCcy, LocalDate startDayIncl) {
            this(quoteCcy, startDayIncl, null);
        }
    }

    record DepositFacilityRateRequest(
            LocalDate startDayIncl,
            LocalDate endDayIncl,
            EqualSubsequentValuesHandling equalSubsequentValuesHandling
    ) implements EcbFetchRequest {
        public enum EqualSubsequentValuesHandling {
            MERGE_EQUAL_SUBSEQUENT_VALUES,
            DONT_MERGE_EQUAL_SUBSEQUENT_VALUES,
        }

        public DepositFacilityRateRequest(LocalDate startDayIncl) {
            this(startDayIncl, null, EqualSubsequentValuesHandling.DONT_MERGE_EQUAL_SUBSEQUENT_VALUES);
        }

        public DepositFacilityRateRequest(LocalDate startDayIncl, LocalDate endDayIncl) {
            this(startDayIncl, endDayIncl, EqualSubsequentValuesHandling.DONT_MERGE_EQUAL_SUBSEQUENT_VALUES);
        }

        public DepositFacilityRateRequest(LocalDate startDayIncl, EqualSubsequentValuesHandling equalSubsequentValuesHandling) {
            this(startDayIncl, null, equalSubsequentValuesHandling);
        }
    }

    record HICPInflationRequest(
            LocalDate startDayIncl,
            LocalDate endDayIncl,
            MomGrowthCalculation momGrowthCalculation,
            YoyGrowthCalculation yoyGrowthCalculation
    ) implements EcbFetchRequest {

        public enum MomGrowthCalculation {
            CALCULATE_MOM_GROWTH,
            DONT_CALCULATE_MOM_GROWTH,
        }

        public enum YoyGrowthCalculation {
            CALCULATE_YOY_GROWTH,
            DONT_CALCULATE_YOY_GROWTH,
        }

        public HICPInflationRequest(LocalDate startDayIncl) {
            this(startDayIncl, null, DONT_CALCULATE_MOM_GROWTH, DONT_CALCULATE_YOY_GROWTH);
        }

        public HICPInflationRequest(LocalDate startDayIncl, MomGrowthCalculation momGrowthCalculation, YoyGrowthCalculation yoyGrowthCalculation) {
            this(startDayIncl, null, momGrowthCalculation, yoyGrowthCalculation);
        }

        public HICPInflationRequest(LocalDate startDayIncl, LocalDate endDayIncl) {
            this(startDayIncl, endDayIncl, DONT_CALCULATE_MOM_GROWTH, DONT_CALCULATE_YOY_GROWTH);
        }
    }


}
