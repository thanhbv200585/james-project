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

import static org.apache.mailet.DsnParameters.HOLDFOR_PARAMETER;
import static org.apache.mailet.DsnParameters.HOLDUNITL_PARAMETER;

import org.apache.james.protocols.api.ProtocolSession;
import static org.apache.james.protocols.api.ProtocolSession.State.Transaction;


import org.apache.james.protocols.smtp.SMTPSession;
import org.apache.james.protocols.smtp.hook.HookResult;
import org.apache.james.protocols.smtp.hook.MailParametersHook;
import org.apache.mailet.FUTURERELEASEParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FutureReleaseMailParameterHook implements MailParametersHook {

    private static final Logger logger = LoggerFactory.getLogger(FutureReleaseMailParameterHook.class);

    public static final ProtocolSession.AttachmentKey<FUTURERELEASEParameters.Holdfor> FUTURERELEASE_HOLDFOR = ProtocolSession.AttachmentKey.of("FUTURERELEASE_HOLDFOR", FUTURERELEASEParameters.Holdfor.class);
    public static final ProtocolSession.AttachmentKey<FUTURERELEASEParameters.Holduntil> FUTURERELEASE_HOLDUNTIL = ProtocolSession.AttachmentKey.of("FUTURERELEASE_HOLDUNTIL", FUTURERELEASEParameters.Holduntil.class);
    @Override
    public HookResult doMailParameter(SMTPSession session, String paramName, String paramValue) {
        if(paramName.equals(HOLDFOR_PARAMETER)){
            logger.debug("HoldFor parameter is set to {}", paramValue);
            Integer value = Integer.parseInt(paramValue);
            FUTURERELEASEParameters.Holdfor holdfor = FUTURERELEASEParameters.Holdfor.of(value);
            session.setAttachment(FUTURERELEASE_HOLDFOR, holdfor, Transaction);
        }else if(paramName.equals(HOLDUNITL_PARAMETER)){
            logger.debug("HoldUntil parameter is set to {}", paramValue);
            FUTURERELEASEParameters.Holduntil holduntil = FUTURERELEASEParameters.Holduntil.of(paramValue);
            session.setAttachment(FUTURERELEASE_HOLDUNTIL, holduntil, Transaction);
        }
        return HookResult.DECLINED;
    }

    @Override
    public String[] getMailParamNames() {
        return new String[] {HOLDFOR_PARAMETER, HOLDUNITL_PARAMETER};
    }
}
