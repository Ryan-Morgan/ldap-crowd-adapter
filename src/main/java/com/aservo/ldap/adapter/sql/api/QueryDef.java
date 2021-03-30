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

package com.aservo.ldap.adapter.sql.api;

import com.aservo.ldap.adapter.sql.api.result.Result;


/**
 * The query definition used to build queries in fluent style.
 */
public interface QueryDef {

    /**
     * Adds an argument to the query.
     *
     * @param key   the ID of the parameter
     * @param value the value of the parameter
     * @return the query definition object
     */
    <T> QueryDef on(String key, T value);

    /**
     * Executes a query.
     *
     * @param clazz the type of the expected result set
     * @return the result set
     */
    <T extends Result> T execute(Class<T> clazz);
}
