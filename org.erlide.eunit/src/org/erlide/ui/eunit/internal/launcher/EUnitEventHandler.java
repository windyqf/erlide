package org.erlide.ui.eunit.internal.launcher;

import org.erlide.jinterface.backend.events.EventHandler;
import org.erlide.jinterface.util.ErlLogger;

import com.ericsson.otp.erlang.OtpErlangObject;

public class EUnitEventHandler extends EventHandler {

	@Override
	protected void doHandleMsg(final OtpErlangObject msg) throws Exception {
		ErlLogger.debug("EUnitEventHandler %s", msg);
	}

}
