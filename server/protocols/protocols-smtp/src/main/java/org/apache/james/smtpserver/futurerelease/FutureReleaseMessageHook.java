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

package org.apache.james.smtpserver.futurerelease;

import org.apache.james.protocols.api.ProtocolSession;
import org.apache.james.protocols.smtp.SMTPSession;
import org.apache.james.protocols.smtp.hook.HookResult;
import org.apache.james.smtpserver.JamesMessageHook;
import org.apache.mailet.Attribute;
import org.apache.mailet.AttributeName;
import org.apache.mailet.AttributeValue;
import org.apache.mailet.DsnParameters;
import org.apache.mailet.Mail;
import static org.apache.james.smtpserver.futurerelease.FutureReleaseMailParameterHook.FUTURERELEASE_HOLDFOR;
import static org.apache.james.smtpserver.futurerelease.FutureReleaseMailParameterHook.FUTURERELEASE_HOLDUNTIL;

import java.time.ZonedDateTime;
import java.util.Optional;

public class FutureReleaseMessageHook implements JamesMessageHook{
    @Override
    public HookResult onMessage(SMTPSession session, Mail mail) {
        Optional<DsnParameters.Holdfor> holdfor = session.getAttachment(FUTURERELEASE_HOLDFOR, ProtocolSession.State.Transaction);
        Optional<DsnParameters.Holduntil> holduntil = session.getAttachment(FUTURERELEASE_HOLDUNTIL, ProtocolSession.State.Transaction);


        mail.setAttribute(new Attribute(AttributeName.of("futurerelease-arrival-date"), AttributeValue.of(ZonedDateTime.now())));
        return HookResult.DECLINED;
    }
}
