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

import com.brinvex.ecb.api.EcbFetchRequest.DepositFacilityRateRequest;
import com.brinvex.ecb.api.EcbFetchRequest.FxRequest;
import com.brinvex.ecb.api.EcbFetchRequest.HICPInflationRequest;
import com.brinvex.ecb.api.EcbFetchResponse.InflationResponse;
import com.brinvex.ecb.internal.EcbFetchServiceImpl;

import java.time.LocalDate;
import java.util.SequencedMap;

public interface EcbFetchService {

    EcbFetchService INSTANCE = newInstance(HttpClientFacade.DEFAULT_INSTANCE);

    static EcbFetchService newInstance(HttpClientFacade httpClientFacade) {
        return new EcbFetchServiceImpl(httpClientFacade);
    }

    SequencedMap<LocalDate, Double> fetch(FxRequest fxReq);

    SequencedMap<LocalDate, Double> fetch(DepositFacilityRateRequest depoFacilityRateReq);

    SequencedMap<LocalDate, InflationResponse> fetch(HICPInflationRequest depoFacilityRateReq);
}
