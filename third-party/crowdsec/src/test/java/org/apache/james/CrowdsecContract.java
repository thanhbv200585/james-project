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

package org.apache.james;


import static org.apache.james.jmap.JMAPTestingConstants.DOMAIN;

import org.apache.james.utils.DataProbeImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public interface CrowdsecContract {

    String BOBS_DOMAIN = "spamer.com";
    String BOB = "bob@" + BOBS_DOMAIN;
    String BOB_PASSWORD = "bobPassword";
    String RECIPIENTS_DOMAIN = "angels.org";
    String ALICE = "alice@" + RECIPIENTS_DOMAIN;
    String ALICE_PASSWORD = "alicePassword";
    String PAUL = "paul@" + RECIPIENTS_DOMAIN;
    String PAUL_PASSWORD = "paulPassword";

    @BeforeEach
    default void setup(GuiceJamesServer server) throws Exception {
        server.getProbe(DataProbeImpl.class).fluent()
            .addDomain(DOMAIN)
            .addUser(BOB, BOB_PASSWORD);
    }

    @Test
    default void spamShouldBeDeliveredInSpamMailboxWhenSameMessageHasAlreadyBeenMovedToSpam(GuiceJamesServer jamesServer) throws Exception {
        System.out.println("test");
    }
}
