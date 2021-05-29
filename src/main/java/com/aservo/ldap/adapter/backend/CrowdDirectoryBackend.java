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

package com.aservo.ldap.adapter.backend;

import com.aservo.ldap.adapter.adapter.FilterMatcher;
import com.aservo.ldap.adapter.adapter.LdapUtils;
import com.aservo.ldap.adapter.adapter.entity.GroupEntity;
import com.aservo.ldap.adapter.adapter.entity.MembershipEntity;
import com.aservo.ldap.adapter.adapter.entity.UserEntity;
import com.aservo.ldap.adapter.adapter.query.AndLogicExpression;
import com.aservo.ldap.adapter.adapter.query.EqualOperator;
import com.aservo.ldap.adapter.adapter.query.FilterNode;
import com.aservo.ldap.adapter.adapter.query.OrLogicExpression;
import com.aservo.ldap.adapter.backend.exception.DirectoryAccessFailureException;
import com.aservo.ldap.adapter.backend.exception.EntityNotFoundException;
import com.aservo.ldap.adapter.backend.exception.SecurityProblemException;
import com.aservo.ldap.adapter.util.ServerConfiguration;
import com.atlassian.crowd.embedded.api.SearchRestriction;
import com.atlassian.crowd.exception.*;
import com.atlassian.crowd.integration.rest.service.factory.RestCrowdClientFactory;
import com.atlassian.crowd.model.group.Group;
import com.atlassian.crowd.model.group.Membership;
import com.atlassian.crowd.model.user.User;
import com.atlassian.crowd.search.query.entity.restriction.*;
import com.atlassian.crowd.search.query.entity.restriction.constants.GroupTermKeys;
import com.atlassian.crowd.search.query.entity.restriction.constants.UserTermKeys;
import com.atlassian.crowd.service.client.ClientProperties;
import com.atlassian.crowd.service.client.ClientPropertiesImpl;
import com.atlassian.crowd.service.client.CrowdClient;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Crowd client API directory backend.
 */
public class CrowdDirectoryBackend
        implements NestedDirectoryBackend {

    /**
     * The constant CONFIG_READINESS_CHECK.
     */
    public static final String CONFIG_READINESS_CHECK = "readiness-check";

    private final Logger logger = LoggerFactory.getLogger(CrowdDirectoryBackend.class);
    private final CrowdClient crowdClient;
    private final boolean useReadinessCheck;

    /**
     * Instantiates a new Crowd directory backend.
     *
     * @param config the config instance of the server
     */
    public CrowdDirectoryBackend(ServerConfiguration config) {

        Properties properties = config.getBackendProperties();

        useReadinessCheck = Boolean.parseBoolean(properties.getProperty(CONFIG_READINESS_CHECK, "true"));

        ClientProperties props = ClientPropertiesImpl.newInstanceFromProperties(properties);
        crowdClient = new RestCrowdClientFactory().newInstance(props);
    }

    public String getId() {

        return "crowd";
    }

    public void startup() {

        try {

            if (useReadinessCheck)
                crowdClient.testConnection();

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public void shutdown() {

        crowdClient.shutdown();
    }

    public GroupEntity getGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getGroup; ID={}", id);

        try {

            return createGroupEntity(crowdClient.getGroup(id));

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public UserEntity getUser(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getUser; ID={}", id);

        try {

            UserEntity entity = createUserEntity(crowdClient.getUser(id));

            return entity;

        } catch (UserNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public UserEntity getAuthenticatedUser(String id, String password)
            throws EntityNotFoundException {

        logger.info("Backend call: getAuthenticatedUser; ID={}", id);

        try {

            return createUserEntity(crowdClient.authenticateUser(id, password));

        } catch (UserNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (InactiveAccountException |
                ExpiredCredentialException |
                ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<GroupEntity> getGroups(FilterNode filterNode, Optional<FilterMatcher> filterMatcher) {

        return getGroups(filterNode, filterMatcher, 0, Integer.MAX_VALUE);
    }

    public List<GroupEntity> getGroups(FilterNode filterNode, Optional<FilterMatcher> filterMatcher,
                                       int startIndex, int maxResults) {

        logger.info("Backend call: getGroups({}, {})", startIndex, maxResults);

        SearchRestriction restriction =
                removeNullRestrictions(createGroupSearchRestriction(LdapUtils.removeNotExpressions(filterNode)));

        try {

            return crowdClient.searchGroups(restriction, startIndex, maxResults).stream()
                    .map(this::createGroupEntity)
                    .filter(x -> filterMatcher.map(y -> y.matchEntity(x, filterNode)).orElse(true))
                    .collect(Collectors.toList());

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<UserEntity> getUsers(FilterNode filterNode, Optional<FilterMatcher> filterMatcher) {

        return getUsers(filterNode, filterMatcher, 0, Integer.MAX_VALUE);
    }

    public List<UserEntity> getUsers(FilterNode filterNode, Optional<FilterMatcher> filterMatcher,
                                     int startIndex, int maxResults) {

        logger.info("Backend call: getUsers({}, {})", startIndex, maxResults);

        SearchRestriction restriction =
                removeNullRestrictions(createUserSearchRestriction(LdapUtils.removeNotExpressions(filterNode)));

        try {

            return crowdClient.searchUsers(restriction, startIndex, maxResults).stream()
                    .map(this::createUserEntity)
                    .filter(x -> filterMatcher.map(y -> y.matchEntity(x, filterNode)).orElse(true))
                    .collect(Collectors.toList());

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<UserEntity> getDirectUsersOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getDirectUsersOfGroup; ID={}", id);

        try {

            return crowdClient.getUsersOfGroup(id, 0, Integer.MAX_VALUE).stream()
                    .map(this::createUserEntity)
                    .collect(Collectors.toList());

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<GroupEntity> getDirectGroupsOfUser(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getDirectGroupsOfUser; ID={}", id);

        try {

            return crowdClient.getGroupsForUser(id, 0, Integer.MAX_VALUE).stream()
                    .map(this::createGroupEntity)
                    .collect(Collectors.toList());

        } catch (UserNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<UserEntity> getTransitiveUsersOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getTransitiveUsersOfGroup; ID={}", id);

        try {

            return crowdClient.getNestedUsersOfGroup(id, 0, Integer.MAX_VALUE).stream()
                    .map(this::createUserEntity)
                    .collect(Collectors.toList());

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<GroupEntity> getTransitiveGroupsOfUser(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getTransitiveGroupsOfUser; ID={}", id);

        try {

            return crowdClient.getGroupsForNestedUser(id, 0, Integer.MAX_VALUE).stream()
                    .map(this::createGroupEntity)
                    .collect(Collectors.toList());

        } catch (UserNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<GroupEntity> getDirectChildGroupsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getDirectChildGroupsOfGroup; ID={}", id);

        try {

            return crowdClient.getChildGroupsOfGroup(id, 0, Integer.MAX_VALUE).stream()
                    .map(this::createGroupEntity)
                    .collect(Collectors.toList());

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<GroupEntity> getDirectParentGroupsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getDirectParentGroupsOfGroup; ID={}", id);

        try {

            return crowdClient.getParentGroupsForGroup(id, 0, Integer.MAX_VALUE).stream()
                    .map(this::createGroupEntity)
                    .collect(Collectors.toList());

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<GroupEntity> getTransitiveChildGroupsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getTransitiveChildGroupsOfGroup; ID={}", id);

        try {

            return crowdClient.getNestedChildGroupsOfGroup(id, 0, Integer.MAX_VALUE).stream()
                    .map(this::createGroupEntity)
                    .collect(Collectors.toList());

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<GroupEntity> getTransitiveParentGroupsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getTransitiveParentGroupsOfGroup; ID={}", id);

        try {

            return crowdClient.getParentGroupsForNestedGroup(id, 0, Integer.MAX_VALUE).stream()
                    .map(this::createGroupEntity)
                    .collect(Collectors.toList());

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<String> getDirectUserIdsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getDirectUserIdsOfGroup; ID={}", id);

        try {

            return crowdClient.getNamesOfUsersOfGroup(id, 0, Integer.MAX_VALUE);

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<String> getDirectGroupIdsOfUser(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getDirectGroupIdsOfUser; ID={}", id);

        try {

            return crowdClient.getNamesOfGroupsForUser(id, 0, Integer.MAX_VALUE);

        } catch (UserNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<String> getTransitiveUserIdsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getTransitiveUserIdsOfGroup; ID={}", id);

        try {

            return crowdClient.getNamesOfNestedUsersOfGroup(id, 0, Integer.MAX_VALUE);

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<String> getTransitiveGroupIdsOfUser(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getTransitiveGroupIdsOfUser; ID={}", id);

        try {

            return crowdClient.getNamesOfGroupsForNestedUser(id, 0, Integer.MAX_VALUE);

        } catch (UserNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<String> getDirectChildGroupIdsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getDirectChildGroupIdsOfGroup; ID={}", id);

        try {

            return crowdClient.getNamesOfChildGroupsOfGroup(id, 0, Integer.MAX_VALUE);

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<String> getDirectParentGroupIdsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getDirectParentGroupIdsOfGroup; ID={}", id);

        try {

            return crowdClient.getNamesOfParentGroupsForGroup(id, 0, Integer.MAX_VALUE);

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<String> getTransitiveChildGroupIdsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getTransitiveChildGroupIdsOfGroup; ID={}", id);

        try {

            return crowdClient.getNamesOfNestedChildGroupsOfGroup(id, 0, Integer.MAX_VALUE);

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public List<String> getTransitiveParentGroupIdsOfGroup(String id)
            throws EntityNotFoundException {

        logger.info("Backend call: getTransitiveParentGroupIdsOfGroup; ID={}", id);

        try {

            return crowdClient.getNamesOfParentGroupsForNestedGroup(id, 0, Integer.MAX_VALUE);

        } catch (GroupNotFoundException e) {

            throw new EntityNotFoundException(e);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    @Override
    public boolean isGroupDirectGroupMember(String groupId1, String groupId2) {

        logger.info("Backend call: isGroupDirectGroupMember; ID1={} ID2={}", groupId1, groupId2);

        try {

            return crowdClient.isGroupDirectGroupMember(groupId1, groupId2);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    @Override
    public boolean isUserDirectGroupMember(String userId, String groupId) {

        logger.info("Backend call: isGroupDirectGroupMember; ID1={} ID2={}", userId, groupId);

        try {

            return crowdClient.isUserDirectGroupMember(userId, groupId);

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    public Iterable<MembershipEntity> getMemberships() {

        logger.info("Backend call: getMemberships");

        try {

            Iterator<Membership> memberships = crowdClient.getMemberships().iterator();

            return new Iterable<MembershipEntity>() {

                @NotNull
                @Override
                public Iterator<MembershipEntity> iterator() {

                    return new Iterator<MembershipEntity>() {

                        @Override
                        public boolean hasNext() {

                            return memberships.hasNext();
                        }

                        @Override
                        public MembershipEntity next() {

                            Membership membership = memberships.next();

                            return new MembershipEntity(membership.getGroupName(),
                                    membership.getChildGroupNames(),
                                    membership.getUserNames());
                        }
                    };
                }
            };

        } catch (ApplicationPermissionException |
                InvalidAuthenticationException e) {

            throw new SecurityProblemException(e);

        } catch (OperationFailedException e) {

            throw new DirectoryAccessFailureException(e);
        }
    }

    private GroupEntity createGroupEntity(Group group) {

        return new GroupEntity(
                group.getName(),
                group.getDescription()
        );
    }

    private UserEntity createUserEntity(User user) {

        return new UserEntity(
                user.getName(),
                user.getLastName(),
                user.getFirstName(),
                user.getDisplayName(),
                user.getEmailAddress(),
                user.isActive()
        );
    }

    private SearchRestriction createGroupSearchRestriction(FilterNode filterNode) {

        if (filterNode instanceof AndLogicExpression) {

            return new BooleanRestrictionImpl(
                    BooleanRestriction.BooleanLogic.AND,
                    ((AndLogicExpression) filterNode).getChildren().stream()
                            .map(this::createGroupSearchRestriction)
                            .collect(Collectors.toList())
            );

        } else if (filterNode instanceof OrLogicExpression) {

            return new BooleanRestrictionImpl(
                    BooleanRestriction.BooleanLogic.OR,
                    ((OrLogicExpression) filterNode).getChildren().stream()
                            .map(this::createGroupSearchRestriction)
                            .collect(Collectors.toList())
            );

        } else if (filterNode instanceof EqualOperator) {

            switch (LdapUtils.normalizeAttribute(((EqualOperator) filterNode).getAttribute())) {

                case SchemaConstants.CN_AT_OID:

                    return new TermRestriction<>(
                            GroupTermKeys.NAME,
                            MatchMode.EXACTLY_MATCHES,
                            ((EqualOperator) filterNode).getValue()
                    );

                case SchemaConstants.DESCRIPTION_AT_OID:

                    return new TermRestriction<>(
                            GroupTermKeys.DESCRIPTION,
                            MatchMode.EXACTLY_MATCHES,
                            ((EqualOperator) filterNode).getValue()
                    );

                default:
                    break;
            }
        }

        return NullRestrictionImpl.INSTANCE;
    }

    private SearchRestriction createUserSearchRestriction(FilterNode filterNode) {

        if (filterNode instanceof AndLogicExpression) {

            return new BooleanRestrictionImpl(
                    BooleanRestriction.BooleanLogic.AND,
                    ((AndLogicExpression) filterNode).getChildren().stream()
                            .map(this::createUserSearchRestriction)
                            .collect(Collectors.toList())
            );

        } else if (filterNode instanceof OrLogicExpression) {

            return new BooleanRestrictionImpl(
                    BooleanRestriction.BooleanLogic.OR,
                    ((OrLogicExpression) filterNode).getChildren().stream()
                            .map(this::createUserSearchRestriction)
                            .collect(Collectors.toList())
            );

        } else if (filterNode instanceof EqualOperator) {

            switch (LdapUtils.normalizeAttribute(((EqualOperator) filterNode).getAttribute())) {

                case SchemaConstants.UID_AT_OID:
                case SchemaConstants.CN_AT_OID:

                    return new TermRestriction<>(
                            UserTermKeys.USERNAME,
                            MatchMode.EXACTLY_MATCHES,
                            ((EqualOperator) filterNode).getValue()
                    );

                case SchemaConstants.SN_AT_OID:

                    return new TermRestriction<>(
                            UserTermKeys.LAST_NAME,
                            MatchMode.EXACTLY_MATCHES,
                            ((EqualOperator) filterNode).getValue()
                    );

                case SchemaConstants.GN_AT_OID:

                    return new TermRestriction<>(
                            UserTermKeys.FIRST_NAME,
                            MatchMode.EXACTLY_MATCHES,
                            ((EqualOperator) filterNode).getValue()
                    );

                case SchemaConstants.DISPLAY_NAME_AT_OID:

                    return new TermRestriction<>(
                            UserTermKeys.DISPLAY_NAME,
                            MatchMode.EXACTLY_MATCHES,
                            ((EqualOperator) filterNode).getValue()
                    );

                case SchemaConstants.MAIL_AT_OID:

                    return new TermRestriction<>(
                            UserTermKeys.EMAIL,
                            MatchMode.EXACTLY_MATCHES,
                            ((EqualOperator) filterNode).getValue()
                    );

                default:
                    break;
            }
        }

        return NullRestrictionImpl.INSTANCE;
    }

    private SearchRestriction removeNullRestrictions(SearchRestriction restriction) {

        if (restriction instanceof BooleanRestriction) {

            List<SearchRestriction> sr =
                    ((BooleanRestriction) restriction).getRestrictions().stream()
                            .map(this::removeNullRestrictions)
                            .filter(x -> !(x instanceof NullRestriction))
                            .collect(Collectors.toList());

            if (sr.size() == 0)
                return NullRestrictionImpl.INSTANCE;
            else if (sr.size() == 1)
                return removeNullRestrictions(sr.get(0));
            else
                return new BooleanRestrictionImpl(((BooleanRestriction) restriction).getBooleanLogic(), sr);
        }

        return restriction;
    }
}
