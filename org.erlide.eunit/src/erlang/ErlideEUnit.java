package erlang;

import java.util.List;

import org.erlide.eunit.TestFunction;
import org.erlide.jinterface.backend.Backend;
import org.erlide.jinterface.backend.BackendException;
import org.erlide.jinterface.backend.util.Util;
import org.erlide.jinterface.util.ErlLogger;

import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangPid;
import com.ericsson.otp.erlang.OtpErlangTuple;
import com.google.common.collect.Lists;

public final class ErlideEUnit {

	public static List<TestFunction> findTests(final Backend backend,
			final List<String> beams) {
		OtpErlangObject res = null;
		try {
			res = backend.call("erlide_eunit", "find_tests", "ls", beams);
		} catch (final BackendException e) {
			ErlLogger.warn(e);
		}
		if (Util.isOk(res)) {
			final OtpErlangTuple t = (OtpErlangTuple) res;
			final OtpErlangList l = (OtpErlangList) t.elementAt(1);
			final List<TestFunction> result = Lists.newArrayListWithCapacity(l
					.arity());
			for (final OtpErlangObject i : l) {
				final OtpErlangTuple funT = (OtpErlangTuple) i;
				result.add(new TestFunction(funT));
			}
			return result;
		}
		return null;
	}

	public static boolean runTests(final Backend backend,
			final OtpErlangList tests, final OtpErlangPid jpid) {
		try {
			ErlLogger.debug("erlide_eunit:run_tests %s  (jpid %s", tests, jpid);
			backend.cast("erlide_eunit", "run_tests", "xx", tests, jpid);
			return true;
		} catch (final BackendException e) {
			ErlLogger.warn(e);
		}
		return false;
	}
}
