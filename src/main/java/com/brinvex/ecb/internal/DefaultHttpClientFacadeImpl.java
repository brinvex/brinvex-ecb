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

import com.brinvex.ecb.api.HttpClientFacade;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.util.Map;

public class DefaultHttpClientFacadeImpl implements HttpClientFacade {

    private static final class Lazy {
        private static final java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .followRedirects(java.net.http.HttpClient.Redirect.ALWAYS)
                .build();
    }

    @Override
    public Response doGet(URI uri, Map<String, Object> headers, Charset respCharset) throws IOException {
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder(uri);
        if (headers != null) {
            for (Map.Entry<String, Object> e : headers.entrySet()) {
                reqBuilder.header(e.getKey(), String.valueOf(e.getValue()));
            }
        }
        HttpResponse<String> resp;
        try {
            resp = Lazy.httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString(respCharset));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Thread was interrupted while fetching %s".formatted(uri), e);
        }
        return new Response(resp.statusCode(), resp.body());
    }
}
