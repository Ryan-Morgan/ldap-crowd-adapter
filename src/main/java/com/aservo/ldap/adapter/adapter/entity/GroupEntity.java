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

package com.aservo.ldap.adapter.adapter.entity;


/**
 * The group entity.
 */
public class GroupEntity
        extends Entity
        implements DescribableEntity {

    private final String description;

    /**
     * Instantiates a new Group.
     *
     * @param id          the id
     * @param description the description
     */
    public GroupEntity(String id, String description) {

        super(id);
        this.description = description;
    }

    /**
     * Gets description.
     *
     * @return the description
     */
    public String getDescription() {

        return description;
    }

    /**
     * Gets the entity type.
     *
     * @return the entity type
     */
    public EntityType getEntityType() {

        return EntityType.GROUP;
    }
}
