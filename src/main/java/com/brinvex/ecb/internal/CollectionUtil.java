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

import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiPredicate;

@SuppressWarnings("UnusedReturnValue")
public class CollectionUtil {

    public static <E> boolean removeAdjacentDuplicates(Collection<E> collection, BiPredicate<E, E> equalityPredicate) {
        Iterator<E> iterator = collection.iterator();

        if (!iterator.hasNext()) {
            return false;
        }

        boolean modified = false;
        E prev = iterator.next();
        while (iterator.hasNext()) {
            E current = iterator.next();
            if (equalityPredicate.test(prev, current)) {
                iterator.remove();
                modified = true;
            } else {
                prev = current;
            }
        }
        return modified;
    }

}
