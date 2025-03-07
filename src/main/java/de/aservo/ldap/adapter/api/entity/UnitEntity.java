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

import de.aservo.ldap.adapter.api.database.exception.UnknownColumnException;


public abstract class UnitEntity
        extends Entity
        implements DescribableEntity {

    private final String description;

    /**
     * Instantiates a new Unit.
     *
     * @param id          the id
     * @param description the description
     */
    public UnitEntity(String id, String description) {

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

    protected Object findColumn(String columnName) {

        switch (columnName) {

            case ColumnNames.TYPE:
                return getEntityType().toString();

            case ColumnNames.ID:
                return getId();

            case ColumnNames.DESCRIPTION:
                return getDescription();

            default:
                throw new UnknownColumnException("Cannot find column " + columnName + " for unit entity.");
        }
    }
}
