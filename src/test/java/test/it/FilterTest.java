package test.it;

import com.aservo.ldap.adapter.api.LdapUtils;
import com.aservo.ldap.adapter.api.directory.DirectoryBackend;
import com.aservo.ldap.adapter.api.entity.EntityType;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.naming.NamingEnumeration;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import test.api.AbstractServerTest;
import test.configuration.server.JsonWithGroupNesting;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisabledIfEnvironmentVariable(named = "TEST_MODE", matches = "(unit-only)")
public class FilterTest
        extends AbstractServerTest {

    public FilterTest() {

        super(new JsonWithGroupNesting(10935));
    }

    @Test
    @Order(1)
    @DisplayName("it should list all entries")
    public void test001()
            throws Exception {

        DirectoryBackend directory = getServer().getDirectoryBackendFactory().getPermanentDirectory();

        String base = "dc=json";

        String filter =
                SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.TOP_OC;

        InitialDirContext context = createContext("UserA", "pw-user-a");

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assertions.assertTrue(results.hasMore());

        getLdapAssertions().assertCorrectEntry(directory, ((SearchResult) results.next()).getAttributes(),
                EntityType.DOMAIN, directory.getId());

        Assertions.assertTrue(results.hasMore());

        getLdapAssertions().assertCorrectEntry(directory, ((SearchResult) results.next()).getAttributes(),
                EntityType.GROUP_UNIT, LdapUtils.OU_GROUPS);

        Assertions.assertTrue(results.hasMore());

        getLdapAssertions().assertCorrectEntry(directory, ((SearchResult) results.next()).getAttributes(),
                EntityType.USER_UNIT, LdapUtils.OU_USERS);

        getLdapAssertions().assertCorrectEntries(directory, results,
                Stream.concat(directory.getAllGroups().stream(), directory.getAllUsers().stream())
                        .collect(Collectors.toSet()));

        context.close();
    }

    @Test
    @Order(2)
    @DisplayName("it should show domain entry")
    public void test002()
            throws Exception {

        DirectoryBackend directory = getServer().getDirectoryBackendFactory().getPermanentDirectory();

        String base = "dc=json";

        String filter =
                SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.DOMAIN_OC;

        InitialDirContext context = createContext("UserA", "pw-user-a");

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assertions.assertTrue(results.hasMore());

        getLdapAssertions().assertCorrectEntry(directory, ((SearchResult) results.next()).getAttributes(),
                EntityType.DOMAIN, directory.getId());

        Assertions.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    @Order(3)
    @DisplayName("it should list all OU entries")
    public void test003()
            throws Exception {

        DirectoryBackend directory = getServer().getDirectoryBackendFactory().getPermanentDirectory();

        String base = "dc=json";

        String filter =
                SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.ORGANIZATIONAL_UNIT_OC;

        InitialDirContext context = createContext("UserA", "pw-user-a");

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assertions.assertTrue(results.hasMore());

        getLdapAssertions().assertCorrectEntry(directory, ((SearchResult) results.next()).getAttributes(),
                EntityType.GROUP_UNIT, LdapUtils.OU_GROUPS);

        Assertions.assertTrue(results.hasMore());

        getLdapAssertions().assertCorrectEntry(directory, ((SearchResult) results.next()).getAttributes(),
                EntityType.USER_UNIT, LdapUtils.OU_USERS);

        Assertions.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    @Order(4)
    @DisplayName("it should handle and-expression correctly to get group OU entry")
    public void test004()
            throws Exception {

        DirectoryBackend directory = getServer().getDirectoryBackendFactory().getPermanentDirectory();

        String base = "dc=json";

        String filter =
                "(&" +
                        "(" + SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.ORGANIZATIONAL_UNIT_OC + ")" +
                        "(" + SchemaConstants.OU_AT + "=" + LdapUtils.OU_GROUPS + ")" +
                        ")";

        InitialDirContext context = createContext("UserA", "pw-user-a");

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assertions.assertTrue(results.hasMore());

        getLdapAssertions().assertCorrectEntry(directory, ((SearchResult) results.next()).getAttributes(),
                EntityType.GROUP_UNIT, LdapUtils.OU_GROUPS);

        Assertions.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    @Order(5)
    @DisplayName("it should handle and-expression correctly to get users OU entry")
    public void test005()
            throws Exception {

        DirectoryBackend directory = getServer().getDirectoryBackendFactory().getPermanentDirectory();

        String base = "dc=json";

        String filter =
                "(&" +
                        "(" + SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.ORGANIZATIONAL_UNIT_OC + ")" +
                        "(" + SchemaConstants.OU_AT + "=" + LdapUtils.OU_USERS + ")" +
                        ")";

        InitialDirContext context = createContext("UserA", "pw-user-a");

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assertions.assertTrue(results.hasMore());

        getLdapAssertions().assertCorrectEntry(directory, ((SearchResult) results.next()).getAttributes(),
                EntityType.USER_UNIT, LdapUtils.OU_USERS);

        Assertions.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    @Order(6)
    @DisplayName("it should handle or-expression correctly to get all OU and DC entries")
    public void test006()
            throws Exception {

        DirectoryBackend directory = getServer().getDirectoryBackendFactory().getPermanentDirectory();

        String base = "dc=json";

        String filter =
                "(|" +
                        "(" + SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.ORGANIZATIONAL_UNIT_OC + ")" +
                        "(" + SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.DOMAIN_OC + ")" +
                        ")";

        InitialDirContext context = createContext("UserA", "pw-user-a");

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assertions.assertTrue(results.hasMore());

        getLdapAssertions().assertCorrectEntry(directory, ((SearchResult) results.next()).getAttributes(),
                EntityType.DOMAIN, directory.getId());

        Assertions.assertTrue(results.hasMore());

        getLdapAssertions().assertCorrectEntry(directory, ((SearchResult) results.next()).getAttributes(),
                EntityType.GROUP_UNIT, LdapUtils.OU_GROUPS);

        Assertions.assertTrue(results.hasMore());

        getLdapAssertions().assertCorrectEntry(directory, ((SearchResult) results.next()).getAttributes(),
                EntityType.USER_UNIT, LdapUtils.OU_USERS);

        Assertions.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    @Order(7)
    @DisplayName("it should handle not-expression correctly to get user entries")
    public void test007()
            throws Exception {

        DirectoryBackend directory = getServer().getDirectoryBackendFactory().getPermanentDirectory();

        String base = "dc=json";

        String filter =
                "(&" +
                        "(!(" + SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.DOMAIN_OC + "))" +
                        "(!(" + SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.ORGANIZATIONAL_UNIT_OC + "))" +
                        "(!(" + SchemaConstants.OBJECT_CLASS_AT + "=" + SchemaConstants.GROUP_OF_NAMES_OC + "))" +
                        ")";

        InitialDirContext context = createContext("UserA", "pw-user-a");

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        getLdapAssertions().assertCorrectEntries(directory, results, directory.getAllUsers());

        context.close();
    }

    /*
    @Test
    @Order(8)
    @DisplayName("it should handle existence filter expressions correctly")
    public void test008()
            throws Exception {

        DirectoryBackend directory = getServer().getDirectoryBackendFactory().getPermanentDirectory();

        String base = "dc=json";

        String filter =
                "(" + SchemaConstants.DESCRIPTION_AT + "=" + "*" + ")";

        InitialDirContext context = createContext("UserA", "pw-user-a");

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assertions.assertTrue(results.hasMore());
        checkRootEntry(((SearchResult) results.next()).getAttributes());

        Assertions.assertTrue(results.hasMore());
        checkGroupsEntry(((SearchResult) results.next()).getAttributes());

        Assertions.assertTrue(results.hasMore());
        checkUsersEntry(((SearchResult) results.next()).getAttributes());

        for (GroupEntity group : directoryBackend.getAllGroups()) {

            Assertions.assertTrue(results.hasMore());
            Attributes attributes = ((SearchResult) results.next()).getAttributes();
            Assertions.assertEquals(group.getId(), getAndCheckGroupEntry(attributes, false));
        }

        Assertions.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    @Order(9)
    @DisplayName("it should filter by DN attribute values")
    public void test009()
            throws Exception {

        DirectoryBackend directory = getServer().getDirectoryBackendFactory().getPermanentDirectory();

        String base = "dc=json";

        String filter =
                "(" + SchemaConstants.MEMBER_AT + "=" + "cn=UserB,ou=users,dc=json" + ")";

        InitialDirContext context = createContext("UserA", "pw-user-a");

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assertions.assertTrue(results.hasMore());
        Attributes attributes1 = ((SearchResult) results.next()).getAttributes();
        Assertions.assertEquals(("GroupA").toLowerCase(), getAndCheckGroupEntry(attributes1, false));

        Assertions.assertTrue(results.hasMore());
        Attributes attributes2 = ((SearchResult) results.next()).getAttributes();
        Assertions.assertEquals(("GroupB").toLowerCase(), getAndCheckGroupEntry(attributes2, false));

        Assertions.assertFalse(results.hasMore());

        context.close();
    }

    @Test
    @Order(10)
    @DisplayName("it should be able to process complex filter expressions")
    public void test010()
            throws Exception {

        DirectoryBackend directory = getServer().getDirectoryBackendFactory().getPermanentDirectory();

        String base = "ou=groups,dc=json";

        // ['GroupB', 'GroupC', 'GroupD']

        String filter =
                "(&" +
                        "(objectClass=groupOfNames)" +
                        "(|(cn=GroupB)(cn=GroupC)(cn=GroupD)(cn=" + Rdn.escapeValue("GroupE+,") + "))" +
                        ")";

        InitialDirContext context = createContext("UserA", "pw-user-a");

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results = context.search(base, filter, sc);

        Assertions.assertTrue(results.hasMore());
        Attributes attributes1 = ((SearchResult) results.next()).getAttributes();
        Assertions.assertEquals(("GroupB").toLowerCase(), getAndCheckGroupEntry(attributes1, false));

        Assertions.assertTrue(results.hasMore());
        Attributes attributes2 = ((SearchResult) results.next()).getAttributes();
        Assertions.assertEquals(("GroupC").toLowerCase(), getAndCheckGroupEntry(attributes2, false));

        Assertions.assertTrue(results.hasMore());
        Attributes attributes3 = ((SearchResult) results.next()).getAttributes();
        Assertions.assertEquals(("GroupD").toLowerCase(), getAndCheckGroupEntry(attributes3, false));

        Assertions.assertTrue(results.hasMore());
        Attributes attributes4 = ((SearchResult) results.next()).getAttributes();
        Assertions.assertEquals(("GroupE+,").toLowerCase(), getAndCheckGroupEntry(attributes4, false));

        Assertions.assertFalse(results.hasMore());

        context.close();
    }
    */
}
