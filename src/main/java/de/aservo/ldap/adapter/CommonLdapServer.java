/*
 * Initiator:
 * Copyright (c) 2012 Dieter Wimberger
 * http://dieter.wimpi.net
 *
 * Maintenance:
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

package de.aservo.ldap.adapter;

import org.apache.commons.io.FileUtils;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.registries.SchemaLoader;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authn.Authenticator;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.directory.server.ldap.handlers.response.CompareResponseHandler;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * The main application starts ApacheDS and initializes Crowd client api.
 */
public class CommonLdapServer {

    private final Logger logger = LoggerFactory.getLogger(CommonLdapServer.class);

    private final ServerConfiguration serverConfig;
    private final DirectoryBackendFactory directoryBackendFactory;
    private final DirectoryService directoryService;

    /**
     * Instantiates a new LDAP server.
     *
     * @param serverConfig the server config
     */
    public CommonLdapServer(ServerConfiguration serverConfig) {

        this.serverConfig = serverConfig;
        this.directoryBackendFactory = new DirectoryBackendFactory(serverConfig);

        try {

            if (Files.exists(serverConfig.getDsCacheDir()))
                FileUtils.deleteDirectory(serverConfig.getDsCacheDir().toFile());

            Files.createDirectories(serverConfig.getDsCacheDir());

        } catch (IOException e) {

            throw new UncheckedIOException(e);
        }

        createNewLoaders();
        directoryService = initDirectoryService();
    }

    /**
     * Gets the server config.
     *
     * @return the server config
     */
    public ServerConfiguration getServerConfig() {

        return serverConfig;
    }

    /**
     * Gets the directory backend factory.
     *
     * @return the directory backend factory
     */
    public DirectoryBackendFactory getDirectoryBackendFactory() {

        return directoryBackendFactory;
    }

    /**
     * Startup method.
     */
    public void startup() {

        try {

            directoryBackendFactory.startup();
            directoryService.startup();

            // https://directory.apache.org/apacheds/configuration/ads-2.0-configuration.html
            LdapServer server = new LdapServer();
            Transport transport = new TcpTransport(serverConfig.getHost(), serverConfig.getPort());

            // SSL support
            if (serverConfig.isSslEnabled()) {

                transport.setEnableSSL(true);
                server.setKeystoreFile(serverConfig.getKeyStoreFile().toString());
                server.setCertificatePassword(serverConfig.getKeyStorePassword());
                server.addExtendedOperationHandler(new StartTlsHandler());
            }

            transport.setBackLog(serverConfig.getConnectionBackLog());
            transport.setNbThreads(serverConfig.getConnectionActiveThreads());
            server.setTransports(transport);
            server.setDirectoryService(directoryService);
            server.setMaxSizeLimit(serverConfig.getResponseMaxSizeLimit());
            server.setMaxTimeLimit(serverConfig.getResponseMaxTimeLimit());
            server.setCompareHandlers(new CompareRequestHandler(), new CompareResponseHandler());

            server.start();

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    /**
     * Shutdown method.
     */
    public void shutdown() {

        try {

            directoryService.shutdown();
            directoryBackendFactory.shutdown();

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    /**
     * Indicates whether the directory service is already running.
     *
     * @return the boolean
     */
    public boolean isStarted() {

        return directoryService.isStarted();
    }

    private void copyStream(String resourcePath, Path outputFile)
            throws IOException {

        if (outputFile.toFile().exists()) {

            return;
        }

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {

            try (OutputStream out = new FileOutputStream(outputFile.toFile())) {

                // transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;

                while ((len = in.read(buf)) > 0) {

                    out.write(buf, 0, len);
                }
            }
        }
    }

    private void createNewLoaders() {

        try {

            // extract the schema on disk (a brand new one) and load the registries
            SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor(serverConfig.getDsCacheDir().toFile());
            extractor.extractOrCopy(true);

            Path attributeTypesDir =
                    serverConfig.getDsCacheDir().resolve("schema/ou=schema/cn=other/ou=attributetypes");

            Files.createDirectories(attributeTypesDir);

            // memberOf Support
            Path memberOfLDIF = attributeTypesDir.resolve("m-oid=1.2.840.113556.1.2.102.ldif");
            copyStream("de/aservo/ldap/adapter/memberof.ldif", memberOfLDIF);

        } catch (IOException e) {

            throw new UncheckedIOException(e);
        }
    }

    private void initSchemaPartition(DirectoryService directoryService) {

        try {

            File schemaRepository = serverConfig.getDsCacheDir().resolve("schema").toFile();

            SchemaLoader loader = new LdifSchemaLoader(schemaRepository);
            SchemaManager schemaManager = new DefaultSchemaManager(loader);
            schemaManager.loadAllEnabled();
            directoryService.setSchemaManager(schemaManager);

            SchemaPartition schemaPartition = new SchemaPartition(schemaManager);
            directoryService.setSchemaPartition(schemaPartition);

            // initialize the LdifPartition
            LdifPartition ldifPartition =
                    new LdifPartition(directoryService.getSchemaManager(), directoryService.getDnFactory());

            ldifPartition.setPartitionPath(schemaRepository.toURI());

            schemaPartition.setWrappedPartition(ldifPartition);
            directoryService.setInstanceLayout(new InstanceLayout(serverConfig.getDsCacheDir().toFile()));

            // We have to load the schema now, otherwise we won't be able
            // to initialize the Partitions, as we won't be able to parse
            // and normalize their suffix DN
            schemaManager.loadAllEnabled();

            List<Throwable> errors = schemaManager.getErrors();

            if (!errors.isEmpty()) {

                throw new IOException(MessageFormat.format("Schema load failed: {0}", errors));
            }

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    private DirectoryService initDirectoryService() {

        try {

            // initialize the LDAP service
            DirectoryService service = new DefaultDirectoryService();

            // first load the schema
            initSchemaPartition(service);

            // then the system partition
            // this is a MANDATORY partition

            JdbmPartition partition =
                    new JdbmPartition(service.getSchemaManager(), service.getDnFactory());

            partition.setId("system");
            partition.setPartitionPath(serverConfig.getDsCacheDir().resolve("system").toFile().toURI());
            partition.setSuffixDn(new Dn(ServerDNConstants.SYSTEM_DN));

            service.setSystemPartition(partition);

            // disable the ChangeLog system
            service.getChangeLog().setEnabled(false);
            service.setDenormalizeOpAttrsEnabled(false);

            //disable Anonymous Access
            service.setAllowAnonymousAccess(false);

            List<Interceptor> interceptors = service.getInterceptors();

            for (Interceptor interceptor : interceptors) {

                if (interceptor instanceof AuthenticationInterceptor) {

                    logger.debug("Interceptor: {}", interceptor.getName());

                    AuthenticationInterceptor ai = (AuthenticationInterceptor) interceptor;
                    Set<Authenticator> auths = new HashSet<>();
                    auths.add(new CommonAuthenticator(directoryBackendFactory, service.getSchemaManager()));
                    ai.setAuthenticators(auths);
                }
            }

            // add Partition
            addPartition(service);

            return service;

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    private void addPartition(DirectoryService directoryService) {

        try {

            CommonPartition partition = new CommonPartition(serverConfig, directoryBackendFactory);

            partition.setSchemaManager(directoryService.getSchemaManager());
            partition.initialize();

            directoryService.addPartition(partition);

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }
}
