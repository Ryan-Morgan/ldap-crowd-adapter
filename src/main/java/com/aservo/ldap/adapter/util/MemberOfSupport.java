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

package com.aservo.ldap.adapter.util;


/**
 * The enum type for compatibility mode.
 */
public enum MemberOfSupport {

    /**
     * For no support of MemberOf attribute or nested-groups.
     */
    OFF("off"),
    /**
     * For support of MemberOf attribute.
     */
    NORMAL("normal"),
    /**
     * For support of MemberOf attribute and nested-groups.
     */
    NESTED_GROUPS("nested-groups"),
    /**
     * For support of MemberOf attribute and resolved nested-groups.
     */
    FLATTENING("flattening");

    private final String text;

    MemberOfSupport(String text) {

        this.text = text;
    }

    @Override
    public String toString() {

        return text;
    }

    /**
     * Indicates whether the MemberOf attribute is allowed.
     *
     * @return the boolean
     */
    public boolean allowMemberOfAttribute() {

        return !this.equals(MemberOfSupport.OFF);
    }
}
