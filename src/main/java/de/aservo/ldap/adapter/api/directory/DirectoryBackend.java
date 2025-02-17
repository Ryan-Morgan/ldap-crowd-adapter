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

package de.aservo.ldap.adapter.api.directory;

import de.aservo.ldap.adapter.api.cursor.MappableCursor;
import de.aservo.ldap.adapter.api.database.Row;
import de.aservo.ldap.adapter.api.directory.exception.EntityNotFoundException;
import de.aservo.ldap.adapter.api.entity.EntityType;
import de.aservo.ldap.adapter.api.entity.GroupEntity;
import de.aservo.ldap.adapter.api.entity.UserEntity;
import de.aservo.ldap.adapter.api.query.QueryExpression;
import org.apache.directory.api.ldap.model.schema.SchemaManager;

import java.util.Set;


/**
 * The interface for all directory backends.
 */
public interface DirectoryBackend {

    /**
     * Gets backend ID.
     *
     * @return the backend ID
     */
    String getId();

    /**
     * Startup method.
     */
    void startup();

    /**
     * Shutdown method.
     */
    void shutdown();

    /**
     * Runs a query expression.
     *
     * @param txId          the transaction ID
     * @param schemaManager the schema manager
     * @param expression    the query expression
     * @param entityType    the entity type
     * @return the query generator
     */
    MappableCursor<Row> runQueryExpression(String txId, SchemaManager schemaManager, QueryExpression expression,
                                           EntityType entityType);

    /**
     * Gets group info.
     *
     * @param id the group ID
     * @return the group
     * @throws EntityNotFoundException the entry not found exception
     */
    GroupEntity getGroup(String id)
            throws EntityNotFoundException;

    /**
     * Gets user info.
     *
     * @param id the user ID
     * @return the user
     * @throws EntityNotFoundException the entry not found exception
     */
    UserEntity getUser(String id)
            throws EntityNotFoundException;

    /**
     * Gets info from authenticated user.
     *
     * @param id       the user ID
     * @param password the password
     * @return the authenticated user
     * @throws EntityNotFoundException the entry not found exception
     */
    UserEntity getAuthenticatedUser(String id, String password)
            throws EntityNotFoundException;

    /**
     * Gets all groups.
     *
     * @return the groups
     */
    Set<GroupEntity> getAllGroups();

    /**
     * Gets all groups.
     *
     * @return the groups
     */
    Set<GroupEntity> getAllGroups(int startIndex, int maxResults);

    /**
     * Gets all users.
     *
     * @return the users
     */
    Set<UserEntity> getAllUsers();

    /**
     * Gets all users.
     *
     * @return the users
     */
    Set<UserEntity> getAllUsers(int startIndex, int maxResults);

    /**
     * Gets direct users of group.
     *
     * @param id the group ID
     * @return the direct users of group
     * @throws EntityNotFoundException the entry not found exception
     */
    Set<UserEntity> getDirectUsersOfGroup(String id)
            throws EntityNotFoundException;

    /**
     * Gets direct groups of user.
     *
     * @param id the user ID
     * @return the direct groups of user
     * @throws EntityNotFoundException the entry not found exception
     */
    Set<GroupEntity> getDirectGroupsOfUser(String id)
            throws EntityNotFoundException;

    /**
     * Gets transitive users of group.
     *
     * @param id the group ID
     * @return the transitive users of group
     * @throws EntityNotFoundException the entry not found exception
     */
    Set<UserEntity> getTransitiveUsersOfGroup(String id)
            throws EntityNotFoundException;

    /**
     * Gets transitive groups of user.
     *
     * @param id the user ID
     * @return the transitive groups of user
     * @throws EntityNotFoundException the entry not found exception
     */
    Set<GroupEntity> getTransitiveGroupsOfUser(String id)
            throws EntityNotFoundException;

    /**
     * Gets direct child groups of group.
     *
     * @param id the group ID
     * @return the direct child groups of group
     * @throws EntityNotFoundException the entry not found exception
     */
    Set<GroupEntity> getDirectChildGroupsOfGroup(String id)
            throws EntityNotFoundException;

    /**
     * Gets direct parent groups of group.
     *
     * @param id the group ID
     * @return the direct parent groups of group
     * @throws EntityNotFoundException the entry not found exception
     */
    Set<GroupEntity> getDirectParentGroupsOfGroup(String id)
            throws EntityNotFoundException;

    /**
     * Gets transitive child groups of group.
     *
     * @param id the group ID
     * @return the transitive child groups of group
     * @throws EntityNotFoundException the entry not found exception
     */
    Set<GroupEntity> getTransitiveChildGroupsOfGroup(String id)
            throws EntityNotFoundException;

    /**
     * Gets transitive parent groups of group.
     *
     * @param id the group ID
     * @return the transitive parent groups of group
     * @throws EntityNotFoundException the entry not found exception
     */
    Set<GroupEntity> getTransitiveParentGroupsOfGroup(String id)
            throws EntityNotFoundException;
}
