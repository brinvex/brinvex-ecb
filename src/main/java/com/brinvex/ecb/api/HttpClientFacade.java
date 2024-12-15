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

import com.brinvex.ecb.internal.DefaultHttpClientFacadeImpl;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;

@FunctionalInterface
public interface HttpClientFacade {

    HttpClientFacade DEFAULT_INSTANCE = new DefaultHttpClientFacadeImpl();
    
    record Response(int status, String body) {
    }

    Response doGet(URI uri, Map<String, Object> headers, Charset respCharset) throws IOException;

}
