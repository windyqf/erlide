package org.erlide.core.erlang.util;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangLong;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangRangeException;
import com.ericsson.otp.erlang.OtpErlangTuple;

public class ErlangFunctionCall {
    private final String module;
    private final String function;
    private final int arity;
    private final String parameters;

    public ErlangFunctionCall(final String module, final String function,
            final int arity) {
        this.module = module;
        this.function = function;
        this.arity = arity;
        parameters = null;
    }

    public ErlangFunctionCall(final String module, final String function,
            final String parameters) {
        this.module = module;
        this.function = function;
        arity = -1;
        this.parameters = parameters;
    }

    public ErlangFunctionCall(final OtpErlangTuple t) {
        final OtpErlangAtom m = (OtpErlangAtom) t.elementAt(0);
        final OtpErlangAtom f = (OtpErlangAtom) t.elementAt(1);
        module = m.atomValue();
        function = f.atomValue();
        final OtpErlangObject element2 = t.elementAt(2);
        if (element2 instanceof OtpErlangLong) {
            final OtpErlangLong arityL = (OtpErlangLong) element2;
            int a;
            try {
                a = arityL.intValue();
            } catch (final OtpErlangRangeException e) {
                a = -1;
            }
            arity = a;
            parameters = null;
        } else {
            final OtpErlangList pars = (OtpErlangList) element2;
            final StringBuilder sb = new StringBuilder("(");
            for (final OtpErlangObject i : pars) {
                sb.append(i.toString()).append(", ");
            }
            if (pars.arity() > 0) {
                sb.delete(sb.length() - 2, Integer.MAX_VALUE);
            }
            sb.append(')');
            parameters = sb.toString();
            arity = pars.arity();
        }
    }

    public String getModule() {
        return module;
    }

    public String getName() {
        return function;
    }

    public int getArity() {
        return arity;
    }

    public String getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(module).append(':').append(function);
        if (parameters != null) {
            sb.append(parameters);
        } else {
            sb.append(arity);
        }
        return sb.toString();
    }
}
