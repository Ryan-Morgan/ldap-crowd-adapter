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

package de.aservo.ldap.adapter.api.entity;

import de.aservo.ldap.adapter.api.LdapUtils;


/**
 * The group unit entity.
 */
public class GroupUnitEntity
        extends UnitEntity {

    /**
     * Instantiates a new Unit.
     *
     * @param description the description
     */
    public GroupUnitEntity(String description) {

        super(LdapUtils.OU_GROUPS, description);
    }

    /**
     * Gets the entity type.
     *
     * @return the entity type
     */
    public EntityType getEntityType() {

        return EntityType.GROUP_UNIT;
    }
}
