package it;

import de.aservo.ldap.adapter.api.directory.DirectoryBackend;
import org.junit.jupiter.api.*;
import test.api.AbstractServerTest;
import test.api.helper.ThrowingConsumer;
import test.configuration.server.JsonWithGroupNestingAndAbbrevAttr;

import javax.naming.NamingEnumeration;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AttributeAbbrevAttrTest
        extends AbstractServerTest {

    public AttributeAbbrevAttrTest() {

        super(new JsonWithGroupNestingAndAbbrevAttr(10936));
    }

    @Test
    @Order(1)
    @DisplayName("it should use the correct flags for attribute abbreviation")
    public void test001()
            throws Exception {

        Assertions.assertTrue(getServer().getServerConfig().isAbbreviateSnAttribute());
        Assertions.assertTrue(getServer().getServerConfig().isAbbreviateGnAttribute());
    }

    @Test
    @Order(2)
    @DisplayName("it should show user attributes correctly with abbreviated attributes")
    public void test002()
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
