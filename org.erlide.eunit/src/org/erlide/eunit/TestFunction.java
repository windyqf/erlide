package org.erlide.eunit;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangFun;
import com.ericsson.otp.erlang.OtpErlangLong;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangRangeException;
import com.ericsson.otp.erlang.OtpErlangTuple;

public class TestFunction {

	OtpErlangTuple tuple;
	private final String module;
	private final String name;
	private final OtpErlangFun fun;
	private final int line;

	public TestFunction(final OtpErlangTuple t) {
		tuple = t;
		final OtpErlangAtom moduleA = (OtpErlangAtom) t.elementAt(0);
		module = moduleA.atomValue();
		final OtpErlangObject o = t.elementAt(1);
		if (o instanceof OtpErlangFun) {
			name = null;
			fun = (OtpErlangFun) o;
			final OtpErlangLong l = (OtpErlangLong) t.elementAt(2);
			int i;
			try {
				i = l.intValue();
			} catch (final OtpErlangRangeException e) {
				i = -1;
			}
			line = i;
		} else {
			final OtpErlangAtom nameA = (OtpErlangAtom) t.elementAt(1);
			name = nameA.atomValue();
			fun = null;
			line = -1;
		}
	}

	public int getLine() {
		return line;
	}

	public OtpErlangFun getFun() {
		return fun;
	}

	public String getName() {
		return name;
	}

	public String getModule() {
		return module;
	}

	public OtpErlangObject getTuple() {
		if (line == -1) {
			return tuple;
		} else {
			return new OtpErlangTuple(new OtpErlangObject[] {
					new OtpErlangLong(line), fun });
		}
	}

}
