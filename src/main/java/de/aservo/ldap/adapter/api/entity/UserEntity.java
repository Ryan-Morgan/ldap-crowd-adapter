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


/**
 * The user entity.
 */
public class UserEntity
        extends Entity {

    private final String username;
    private final String lastName;
    private final String firstName;
    private final String displayName;
    private final String email;
    private final boolean active;

    /**
     * Instantiates a new User.
     *
     * @param username    the username
     * @param lastName    the last name
     * @param firstName   the first name
     * @param displayName the display name
     * @param email       the email
     * @param active      the active flag
     */
    public UserEntity(String username, String lastName, String firstName, String displayName, String email,
                      boolean active) {

        super(username.toLowerCase());
        this.username = username;
        this.lastName = lastName;
        this.firstName = firstName;
        this.displayName = displayName;
        this.email = email;
        this.active = active;
    }

    /**
     * Gets username.
     *
     * @return the username
     */
    public String getUsername() {

        return username;
    }

    /**
     * Gets last name.
     *
     * @return the last name
     */
    public String getLastName() {

        return lastName;
    }

    /**
     * Gets first name.
     *
     * @return the first name
     */
    public String getFirstName() {

        return firstName;
    }

    /**
     * Gets display name.
     *
     * @return the display name
     */
    public String getDisplayName() {

        return displayName;
    }

    /**
     * Gets email.
     *
     * @return the email
     */
    public String getEmail() {

        return email;
    }

    /**
     * Check if user is active.
     *
     * @return the boolean
     */
    public boolean isActive() {

        return active;
    }

    /**
     * Gets the entity type.
     *
     * @return the entity type
     */
    public EntityType getEntityType() {

        return EntityType.USER;
    }

    protected Object findColumn(String columnName) {

        switch (columnName) {

            case ColumnNames.TYPE:
                return getEntityType().toString();

            case ColumnNames.ID:
                return getId();

            case ColumnNames.USERNAME:
                return getUsername();

            case ColumnNames.LAST_NAME:
                return getLastName();

            case ColumnNames.FIRST_NAME:
                return getFirstName();

            case ColumnNames.DISPLAY_NAME:
                return getDisplayName();

            case ColumnNames.EMAIL:
                return getEmail();

            case ColumnNames.ACTIVE:
                return isActive();

            default:
                throw new UnknownColumnException("Cannot find column " + columnName + " for user entity.");
        }
    }
}
