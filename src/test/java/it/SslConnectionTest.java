package it;

import de.aservo.ldap.adapter.api.directory.DirectoryBackend;
import de.aservo.ldap.adapter.api.entity.EntityType;
import org.junit.jupiter.api.*;
import test.api.AbstractServerTest;
import test.api.helper.ThrowingConsumer;
import test.configuration.server.JsonWithGroupNestingAndSsl;

import javax.naming.NamingEnumeration;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SslConnectionTest
        extends AbstractServerTest {

    public SslConnectionTest() {

        super(new JsonWithGroupNestingAndSsl(10931));
    }

    @Test
    @Order(1)
    @DisplayName("it should be able to connect via SSL")
    public void test001()
            throws Exception {

        getServer().getDirectoryBackendFactory().withSession((ThrowingConsumer<DirectoryBackend>) directory -> {

            String base = "cn=UserA,ou=users,dc=json";
            String filter = "objectClass=inetOrgPerson";

            InitialDirContext context = createContext("UserA", "pw-user-a");

            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

            NamingEnumeration results = context.search(base, filter, sc);

            Assertions.assertTrue(results.hasMore());

            getLdapAssertions().assertCorrectEntry(directory, ((SearchResult) results.next()).getAttributes(),
                    EntityType.USER, ("UserA").toLowerCase());

            Assertions.assertFalse(results.hasMore());

            context.close();
        });
    }
}
