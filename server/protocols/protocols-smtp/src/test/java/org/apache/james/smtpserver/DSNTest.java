/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.smtpserver;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.mailet.DsnParameters.Notify.DELAY;
import static org.apache.mailet.DsnParameters.Notify.FAILURE;
import static org.apache.mailet.DsnParameters.Notify.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.EnumSet;

import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.commons.net.smtp.SMTPClient;
import org.apache.james.UserEntityValidator;
import org.apache.james.core.Domain;
import org.apache.james.core.MailAddress;
import org.apache.james.core.Username;
import org.apache.james.dnsservice.api.DNSService;
import org.apache.james.dnsservice.api.InMemoryDNSService;
import org.apache.james.domainlist.api.DomainList;
import org.apache.james.domainlist.lib.DomainListConfiguration;
import org.apache.james.domainlist.memory.MemoryDomainList;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.mailbox.Authorizator;
import org.apache.james.mailrepository.api.MailRepositoryStore;
import org.apache.james.mailrepository.api.Protocol;
import org.apache.james.mailrepository.memory.MailRepositoryStoreConfiguration;
import org.apache.james.mailrepository.memory.MemoryMailRepository;
import org.apache.james.mailrepository.memory.MemoryMailRepositoryStore;
import org.apache.james.mailrepository.memory.MemoryMailRepositoryUrlStore;
import org.apache.james.mailrepository.memory.SimpleMailRepositoryLoader;
import org.apache.james.metrics.api.Metric;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.metrics.tests.RecordingMetricFactory;
import org.apache.james.protocols.api.utils.ProtocolServerUtils;
import org.apache.james.protocols.lib.mock.MockProtocolHandlerLoader;
import org.apache.james.queue.api.MailQueueFactory;
import org.apache.james.queue.api.RawMailQueueItemDecoratorFactory;
import org.apache.james.queue.memory.MemoryMailQueueFactory;
import org.apache.james.rrt.api.AliasReverseResolver;
import org.apache.james.rrt.api.CanSendFrom;
import org.apache.james.rrt.api.RecipientRewriteTable;
import org.apache.james.rrt.api.RecipientRewriteTableConfiguration;
import org.apache.james.rrt.lib.AliasReverseResolverImpl;
import org.apache.james.rrt.lib.CanSendFromImpl;
import org.apache.james.rrt.memory.MemoryRecipientRewriteTable;
import org.apache.james.server.core.configuration.Configuration;
import org.apache.james.server.core.configuration.FileConfigurationProvider;
import org.apache.james.server.core.filesystem.FileSystemImpl;
import org.apache.james.smtpserver.netty.SMTPServer;
import org.apache.james.smtpserver.netty.SmtpMetricsImpl;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.memory.MemoryUsersRepository;
import org.apache.mailet.DsnParameters;
import org.apache.mailet.Mail;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.inject.TypeLiteral;

class DSNTest {
    public static final String LOCAL_DOMAIN = "example.local";
    public static final Username BOB = Username.of("bob@localhost");
    public static final String PASSWORD = "bobpwd";

    protected MemoryDomainList domainList;
    protected MemoryUsersRepository usersRepository;
    protected SMTPServerTest.AlterableDNSServer dnsServer;
    protected MemoryMailRepositoryStore mailRepositoryStore;
    protected FileSystemImpl fileSystem;
    protected Configuration configuration;
    protected MockProtocolHandlerLoader chain;
    protected MemoryMailQueueFactory queueFactory;
    protected MemoryMailQueueFactory.MemoryCacheableMailQueue queue;

    private SMTPServer smtpServer;

    @BeforeEach
    void setUp() throws Exception {
        domainList = new MemoryDomainList(new InMemoryDNSService());
        domainList.configure(DomainListConfiguration.DEFAULT);

        domainList.addDomain(Domain.of(LOCAL_DOMAIN));
        domainList.addDomain(Domain.of("examplebis.local"));
        usersRepository = MemoryUsersRepository.withVirtualHosting(domainList);
        usersRepository.addUser(BOB, PASSWORD);

        createMailRepositoryStore();

        setUpFakeLoader();
        setUpSMTPServer();
    }

    protected void createMailRepositoryStore() throws Exception {
        configuration = Configuration.builder()
                .workingDirectory("../")
                .configurationFromClasspath()
                .build();
        fileSystem = new FileSystemImpl(configuration.directories());
        MemoryMailRepositoryUrlStore urlStore = new MemoryMailRepositoryUrlStore();

        MailRepositoryStoreConfiguration configuration = MailRepositoryStoreConfiguration.forItems(
            new MailRepositoryStoreConfiguration.Item(
                ImmutableList.of(new Protocol("memory")),
                MemoryMailRepository.class.getName(),
                new BaseHierarchicalConfiguration()));

        mailRepositoryStore = new MemoryMailRepositoryStore(urlStore, new SimpleMailRepositoryLoader(), configuration);
        mailRepositoryStore.init();
    }

    protected SMTPServer createSMTPServer(SmtpMetricsImpl smtpMetrics) {
        return new SMTPServer(smtpMetrics);
    }

    protected void setUpSMTPServer() {
        SmtpMetricsImpl smtpMetrics = mock(SmtpMetricsImpl.class);
        when(smtpMetrics.getCommandsMetric()).thenReturn(mock(Metric.class));
        when(smtpMetrics.getConnectionMetric()).thenReturn(mock(Metric.class));
        smtpServer = createSMTPServer(smtpMetrics);
        smtpServer.setDnsService(dnsServer);
        smtpServer.setFileSystem(fileSystem);
        smtpServer.setProtocolHandlerLoader(chain);
    }

    protected void setUpFakeLoader() {
        dnsServer = new SMTPServerTest.AlterableDNSServer();

        MemoryRecipientRewriteTable rewriteTable = new MemoryRecipientRewriteTable();
        rewriteTable.setConfiguration(RecipientRewriteTableConfiguration.DEFAULT_ENABLED);
        AliasReverseResolver aliasReverseResolver = new AliasReverseResolverImpl(rewriteTable);
        CanSendFrom canSendFrom = new CanSendFromImpl(rewriteTable, aliasReverseResolver);
        queueFactory = new MemoryMailQueueFactory(new RawMailQueueItemDecoratorFactory());
        queue = queueFactory.createQueue(MailQueueFactory.SPOOL);

        chain = MockProtocolHandlerLoader.builder()
            .put(binder -> binder.bind(DomainList.class).toInstance(domainList))
            .put(binder -> binder.bind(new TypeLiteral<MailQueueFactory<?>>() {}).toInstance(queueFactory))
            .put(binder -> binder.bind(RecipientRewriteTable.class).toInstance(rewriteTable))
            .put(binder -> binder.bind(CanSendFrom.class).toInstance(canSendFrom))
            .put(binder -> binder.bind(FileSystem.class).toInstance(fileSystem))
            .put(binder -> binder.bind(MailRepositoryStore.class).toInstance(mailRepositoryStore))
            .put(binder -> binder.bind(DNSService.class).toInstance(dnsServer))
            .put(binder -> binder.bind(UsersRepository.class).toInstance(usersRepository))
            .put(binder -> binder.bind(MetricFactory.class).to(RecordingMetricFactory.class))
            .put(binder -> binder.bind(UserEntityValidator.class).toInstance(UserEntityValidator.NOOP))
            .put(binder -> binder.bind(Authorizator.class).toInstance((userId, otherUserId) -> Authorizator.AuthorizationState.ALLOWED))
            .build();
    }

    @AfterEach
    void tearDown() {
        smtpServer.destroy();
    }
 // check whether ehlo contains DSN Extensions or not
    @Test
    void ehloShouldAdvertiseDsnExtension() throws Exception {
        smtpServer.configure(FileConfigurationProvider.getConfig(
            ClassLoader.getSystemResourceAsStream("smtpserver-dsn.xml")));
        smtpServer.init();

        SMTPClient smtpProtocol = new SMTPClient();
        InetSocketAddress bindedAddress = new ProtocolServerUtils(smtpServer).retrieveBindedAddress();
        smtpProtocol.connect(bindedAddress.getAddress().getHostAddress(), bindedAddress.getPort());
        authenticate(smtpProtocol);
        smtpProtocol.sendCommand("EHLO localhost");

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(smtpProtocol.getReplyCode()).isEqualTo(250);
            softly.assertThat(smtpProtocol.getReplyString()).contains("250 DSN");
        });
    }

    private void authenticate(SMTPClient smtpProtocol) throws IOException {
        smtpProtocol.sendCommand("AUTH PLAIN");
        smtpProtocol.sendCommand(Base64.getEncoder().encodeToString(("\0" + BOB.asString() + "\0" + PASSWORD + "\0").getBytes(UTF_8)));
        assertThat(smtpProtocol.getReplyCode())
            .as("authenticated")
            .isEqualTo(235);
    }

    @Test
    void dsnParametersShouldBeSetOnTheFinalEmail() throws Exception {
        smtpServer.configure(FileConfigurationProvider.getConfig(
            ClassLoader.getSystemResourceAsStream("smtpserver-dsn.xml")));
        smtpServer.init();

        SMTPClient smtpProtocol = new SMTPClient();
        InetSocketAddress bindedAddress = new ProtocolServerUtils(smtpServer).retrieveBindedAddress();
        smtpProtocol.connect(bindedAddress.getAddress().getHostAddress(), bindedAddress.getPort());
        authenticate(smtpProtocol);

        smtpProtocol.sendCommand("EHLO localhost");
        smtpProtocol.sendCommand("MAIL FROM: <bob@localhost> RET=HDRS ENVID=QQ314159");
        smtpProtocol.sendCommand("RCPT TO:<rcpt@localhost> NOTIFY=SUCCESS,FAILURE,DELAY ORCPT=rfc822;orcpt@localhost");
        smtpProtocol.sendShortMessageData("Subject: test mail\r\n\r\nTest body testSimpleMailSendWithDSN\r\n.\r\n");

        Mail lastMail = queue.getLastMail();
        assertThat(lastMail.dsnParameters())
            .contains(DsnParameters.builder()
                .envId(DsnParameters.EnvId.of("QQ314159"))
                .ret(DsnParameters.Ret.HDRS)
                .addRcptParameter(new MailAddress("rcpt@localhost"), DsnParameters.RecipientDsnParameters.of(
                    EnumSet.of(SUCCESS, FAILURE, DELAY),
                    new MailAddress("orcpt@localhost")
                )).build().get());
    }

    @Test
    void multipleRecipientsShouldBeSupported() throws Exception {
        smtpServer.configure(FileConfigurationProvider.getConfig(
            ClassLoader.getSystemResourceAsStream("smtpserver-dsn.xml")));
        smtpServer.init();

        SMTPClient smtpProtocol = new SMTPClient();
        InetSocketAddress bindedAddress = new ProtocolServerUtils(smtpServer).retrieveBindedAddress();
        smtpProtocol.connect(bindedAddress.getAddress().getHostAddress(), bindedAddress.getPort());
        authenticate(smtpProtocol);

        smtpProtocol.sendCommand("EHLO localhost");
        smtpProtocol.sendCommand("MAIL FROM: <bob@localhost> RET=HDRS ENVID=QQ314159");
        smtpProtocol.sendCommand("RCPT TO:<rcpt1@localhost> NOTIFY=SUCCESS,FAILURE,DELAY ORCPT=rfc822;orcpt1@localhost");
        smtpProtocol.sendCommand("RCPT TO:<rcpt2@localhost> NOTIFY=SUCCESS,FAILURE,DELAY ORCPT=rfc822;orcpt2@localhost");
        smtpProtocol.sendCommand("RCPT TO:<rcpt@localhost>");
        smtpProtocol.sendShortMessageData("Subject: test mail\r\n\r\nTest body testSimpleMailSendWithDSN\r\n.\r\n");

        Mail lastMail = queue.getLastMail();
        assertThat(lastMail.dsnParameters())
            .contains(DsnParameters.builder()
                .envId(DsnParameters.EnvId.of("QQ314159"))
                .ret(DsnParameters.Ret.HDRS)
                .addRcptParameter(new MailAddress("rcpt1@localhost"), DsnParameters.RecipientDsnParameters.of(
                    EnumSet.of(SUCCESS, FAILURE, DELAY),
                    new MailAddress("orcpt1@localhost")))
                .addRcptParameter(new MailAddress("rcpt2@localhost"), DsnParameters.RecipientDsnParameters.of(
                    EnumSet.of(SUCCESS, FAILURE, DELAY),
                    new MailAddress("orcpt2@localhost")
                )).build().get());
    }

    @Test
    void notifyCanBeOmitted() throws Exception {
        smtpServer.configure(FileConfigurationProvider.getConfig(
            ClassLoader.getSystemResourceAsStream("smtpserver-dsn.xml")));
        smtpServer.init();

        SMTPClient smtpProtocol = new SMTPClient();
        InetSocketAddress bindedAddress = new ProtocolServerUtils(smtpServer).retrieveBindedAddress();
        smtpProtocol.connect(bindedAddress.getAddress().getHostAddress(), bindedAddress.getPort());
        authenticate(smtpProtocol);

        smtpProtocol.sendCommand("EHLO localhost");
        smtpProtocol.sendCommand("MAIL FROM: <bob@localhost> RET=HDRS ENVID=QQ314159");
        smtpProtocol.sendCommand("RCPT TO:<rcpt@localhost> ORCPT=rfc822;orcpt@localhost");
        smtpProtocol.sendShortMessageData("Subject: test mail\r\n\r\nTest body testSimpleMailSendWithDSN\r\n.\r\n");

        Mail lastMail = queue.getLastMail();
        assertThat(lastMail.dsnParameters())
            .contains(DsnParameters.builder()
                .envId(DsnParameters.EnvId.of("QQ314159"))
                .ret(DsnParameters.Ret.HDRS)
                .addRcptParameter(new MailAddress("rcpt@localhost"), DsnParameters.RecipientDsnParameters.of(
                    new MailAddress("orcpt@localhost")))
                .build().get());
    }

    @Test
    void orcptCanBeOmitted() throws Exception {
        smtpServer.configure(FileConfigurationProvider.getConfig(
            ClassLoader.getSystemResourceAsStream("smtpserver-dsn.xml")));
        smtpServer.init();

        SMTPClient smtpProtocol = new SMTPClient();
        InetSocketAddress bindedAddress = new ProtocolServerUtils(smtpServer).retrieveBindedAddress();
        smtpProtocol.connect(bindedAddress.getAddress().getHostAddress(), bindedAddress.getPort());
        authenticate(smtpProtocol);

        smtpProtocol.sendCommand("EHLO localhost");
        smtpProtocol.sendCommand("MAIL FROM: <bob@localhost> RET=HDRS ENVID=QQ314159");
        smtpProtocol.sendCommand("RCPT TO:<rcpt@localhost> NOTIFY=SUCCESS,FAILURE,DELAY");
        smtpProtocol.sendShortMessageData("Subject: test mail\r\n\r\nTest body testSimpleMailSendWithDSN\r\n.\r\n");

        Mail lastMail = queue.getLastMail();
        assertThat(lastMail.dsnParameters())
            .contains(DsnParameters.builder()
                .envId(DsnParameters.EnvId.of("QQ314159"))
                .ret(DsnParameters.Ret.HDRS)
                .addRcptParameter(new MailAddress("rcpt@localhost"), DsnParameters.RecipientDsnParameters.of(
                    EnumSet.of(SUCCESS, FAILURE, DELAY)))
                .build().get());
    }

    @Test
    void retCanBeOmitted() throws Exception {
        smtpServer.configure(FileConfigurationProvider.getConfig(
            ClassLoader.getSystemResourceAsStream("smtpserver-dsn.xml")));
        smtpServer.init();

        SMTPClient smtpProtocol = new SMTPClient();
        InetSocketAddress bindedAddress = new ProtocolServerUtils(smtpServer).retrieveBindedAddress();
        smtpProtocol.connect(bindedAddress.getAddress().getHostAddress(), bindedAddress.getPort());
        authenticate(smtpProtocol);

        smtpProtocol.sendCommand("EHLO localhost");
        smtpProtocol.sendCommand("MAIL FROM: <bob@localhost> ENVID=QQ314159");
        smtpProtocol.sendCommand("RCPT TO:<rcpt@localhost> NOTIFY=SUCCESS,FAILURE,DELAY ORCPT=rfc822;orcpt@localhost");
        smtpProtocol.sendShortMessageData("Subject: test mail\r\n\r\nTest body testSimpleMailSendWithDSN\r\n.\r\n");

        Mail lastMail = queue.getLastMail();
        assertThat(lastMail.dsnParameters())
            .contains(DsnParameters.builder()
                .envId(DsnParameters.EnvId.of("QQ314159"))
                .addRcptParameter(new MailAddress("rcpt@localhost"), DsnParameters.RecipientDsnParameters.of(
                    EnumSet.of(SUCCESS, FAILURE, DELAY),
                    new MailAddress("orcpt@localhost")))
                .build().get());
    }

    @Test
    void envIdCanBeOmitted() throws Exception {
        smtpServer.configure(FileConfigurationProvider.getConfig(
            ClassLoader.getSystemResourceAsStream("smtpserver-dsn.xml")));
        smtpServer.init();

        SMTPClient smtpProtocol = new SMTPClient();
        InetSocketAddress bindedAddress = new ProtocolServerUtils(smtpServer).retrieveBindedAddress();
        smtpProtocol.connect(bindedAddress.getAddress().getHostAddress(), bindedAddress.getPort());
        authenticate(smtpProtocol);

        smtpProtocol.sendCommand("EHLO localhost");
        smtpProtocol.sendCommand("MAIL FROM: <bob@localhost> RET=HDRS");
        smtpProtocol.sendCommand("RCPT TO:<rcpt@localhost> NOTIFY=SUCCESS,FAILURE,DELAY ORCPT=rfc822;orcpt@localhost");
        smtpProtocol.sendShortMessageData("Subject: test mail\r\n\r\nTest body testSimpleMailSendWithDSN\r\n.\r\n");

        Mail lastMail = queue.getLastMail();
        assertThat(lastMail.dsnParameters())
            .contains(DsnParameters.builder()
                .ret(DsnParameters.Ret.HDRS)
                .addRcptParameter(new MailAddress("rcpt@localhost"), DsnParameters.RecipientDsnParameters.of(
                    EnumSet.of(SUCCESS, FAILURE, DELAY),
                    new MailAddress("orcpt@localhost")
                )).build().get());
    }
}
