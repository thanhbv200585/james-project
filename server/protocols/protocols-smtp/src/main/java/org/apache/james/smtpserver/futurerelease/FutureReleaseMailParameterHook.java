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
import static org.apache.james.protocols.api.ProtocolSession.State.Transaction;
import static org.apache.mailet.FutureReleaseParameters.HOLDFOR_PARAMETER;
import static org.apache.mailet.FutureReleaseParameters.HOLDUNITL_PARAMETER;
import static org.apache.mailet.FutureReleaseParameters.holdforValue;


import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.apache.james.protocols.smtp.SMTPSession;
import org.apache.james.protocols.smtp.hook.HookResult;
import org.apache.james.protocols.smtp.hook.MailParametersHook;
import org.apache.mailet.FutureReleaseParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FutureReleaseMailParameterHook implements MailParametersHook {

    private static final Logger LOGGER = LoggerFactory.getLogger(FutureReleaseMailParameterHook.class);

    public static final ProtocolSession.AttachmentKey<FutureReleaseParameters.Holdfor> FUTURERELEASE_HOLDFOR = ProtocolSession.AttachmentKey.of("FUTURERELEASE_HOLDFOR", FutureReleaseParameters.Holdfor.class);
    public static final ProtocolSession.AttachmentKey<FutureReleaseParameters.Holduntil> FUTURERELEASE_HOLDUNTIL = ProtocolSession.AttachmentKey.of("FUTURERELEASE_HOLDUNTIL", FutureReleaseParameters.Holduntil.class);

    @Override
    public HookResult doMailParameter(SMTPSession session, String paramName, String paramValue) {
        Long interval = new Long(0);
        if (paramName.equals(HOLDFOR_PARAMETER)) {
            LOGGER.debug("HoldFor parameter is set to {}", paramValue);
            interval = Long.parseLong(paramValue);
        }else if (paramName.equals(HOLDUNITL_PARAMETER)) {
            LOGGER.debug("Convert holdunitl parameters into holdfor parameters");
            LocalDateTime now = LocalDateTime.now();
            if (paramValue.length() == 21) {
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
                LocalDateTime dateTime = LocalDateTime.parse(paramValue, dateTimeFormatter);
                interval =  Duration.between(now, dateTime).toSeconds();
            }else if (paramValue.length() == 25){
                LocalDateTime dateTime = ZonedDateTime.parse(paramValue).toLocalDateTime();
                interval = Duration.between(now, dateTime).toSeconds();
            }
        }
        if (interval.longValue() > holdforValue){
            LOGGER.debug("Invalid holdfor value {}", interval);
            return HookResult.DECLINED;
        }
        FutureReleaseParameters.Holdfor holdfor = FutureReleaseParameters.Holdfor.of(interval);
        session.setAttachment(FUTURERELEASE_HOLDFOR, holdfor, Transaction);
        return HookResult.DECLINED;
    }

    @Override
    public String[] getMailParamNames() {
        return new String[] {HOLDFOR_PARAMETER, HOLDUNITL_PARAMETER};
    }
}
