package org.apache.james.smtpserver.futurerelease;

import org.apache.james.protocols.smtp.SMTPSession;
import org.apache.james.protocols.smtp.hook.HeloHook;
import org.apache.james.protocols.smtp.hook.HookResult;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class FutureReleaseEHLOHook implements HeloHook {

    @Override
    public Set<String> implementedEsmtpFeatures() {
        return ImmutableSet.of("FUTURERELEASE");
    }

    @Override
    public HookResult doHelo(SMTPSession session, String helo) {return HookResult.DECLINED;}
}

