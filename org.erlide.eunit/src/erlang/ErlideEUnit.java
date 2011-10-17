package erlang;

import java.util.List;

import org.erlide.core.common.Util;
import org.erlide.core.rpc.IRpcCallSite;
import org.erlide.core.rpc.RpcException;
import org.erlide.eunit.EUnitTestFunction;
import org.erlide.jinterface.ErlLogger;

import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangPid;
import com.ericsson.otp.erlang.OtpErlangTuple;
import com.google.common.collect.Lists;

public final class ErlideEUnit {

    public static List<EUnitTestFunction> findTests(final IRpcCallSite backend,
            final List<String> beams) {
        OtpErlangObject res = null;
        try {
            res = backend.call("erlide_eunit", "find_tests", "ls", beams);
        } catch (final RpcException e) {
            ErlLogger.warn(e);
        }
        if (Util.isOk(res)) {
            final OtpErlangTuple t = (OtpErlangTuple) res;
            final OtpErlangList l = (OtpErlangList) t.elementAt(1);
            final List<EUnitTestFunction> result = Lists
                    .newArrayListWithCapacity(l.arity());
            for (final OtpErlangObject i : l) {
                final OtpErlangTuple funT = (OtpErlangTuple) i;
                result.add(new EUnitTestFunction(funT));
            }
            return result;
        }
        return null;
    }

    public static boolean runTests(final IRpcCallSite backend,
            final OtpErlangList tests, final OtpErlangPid jpid) {
        ErlLogger.debug("erlide_eunit:run_tests %s  (jpid %s", tests, jpid);
        try {
            backend.cast("erlide_eunit", "run_tests", "xx", tests, jpid);
            return true;
        } catch (final RpcException e) {
            ErlLogger.warn(e);
        }
        return false;
    }
}
