/*
 * Copyright (c) 2019 ASERVO Software GmbH
 * contact@aservo.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.aservo.ldap.adapter.api.database.result;

import de.aservo.ldap.adapter.api.database.Row;

import java.util.List;
import java.util.function.Function;


/**
 * Result for a non empty list of values.
 */
public interface IndexedNonEmptySeqResult
        extends EnrichedResult {

    /**
     * Transforms rows to expected types.
     *
     * @param f the function used to map a single row
     * @return the non empty list of values of an expected type
     */
    <T> List<T> transform(Function<Row, T> f);
}
