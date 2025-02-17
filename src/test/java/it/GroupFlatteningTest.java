package it;

import de.aservo.ldap.adapter.api.directory.DirectoryBackend;
import org.junit.jupiter.api.*;
import test.api.AbstractServerTest;
import test.api.helper.ThrowingConsumer;
import test.configuration.server.JsonWithGroupFlattening;

import javax.naming.NamingEnumeration;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GroupFlatteningTest
        extends AbstractServerTest {

    public GroupFlatteningTest() {

        super(new JsonWithGroupFlattening(10933));
    }

    @Test
    @Order(1)
    @DisplayName("it should use the correct flags for attribute abbreviation")
    public void test001()
            throws Exception {

        Assertions.assertFalse(getServer().getServerConfig().isAbbreviateSnAttribute());
        Assertions.assertFalse(getServer().getServerConfig().isAbbreviateGnAttribute());
    }

    @Test
    @Disabled
    @Order(2)
    @DisplayName("it should show group attributes correctly in flattening mode")
    public void test002()
            throws Exception {

        getServer().getDirectoryBackendFactory().withSession((ThrowingConsumer<DirectoryBackend>) directory -> {

            String base = "ou=groups,dc=json";
            String filter = "objectClass=groupOfUniqueNames";

            InitialDirContext context = createContext("UserA", "pw-user-a");

            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

            NamingEnumeration results = context.search(base, filter, sc);

            getLdapAssertions().assertCorrectEntries(directory, results, directory.getAllGroups());

            context.close();
        });
    }

    @Test
    @Disabled
    @Order(3)
    @DisplayName("it should show user attributes correctly in flattening mode")
    public void test003()
            throws Exception {

        getServer().getDirectoryBackendFactory().withSession((ThrowingConsumer<DirectoryBackend>) directory -> {

            String base = "ou=users,dc=json";
            String filter = "objectClass=inetOrgPerson";

            InitialDirContext context = createContext("UserA", "pw-user-a");

            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

            NamingEnumeration results = context.search(base, filter, sc);

            getLdapAssertions().assertCorrectEntries(directory, results, directory.getAllUsers());

            context.close();
        });
    }
}
