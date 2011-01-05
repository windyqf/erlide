package org.erlide.eunit;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangTuple;

public class EUnitTestFunction {

	private final Kind kind;
	private final String module;
	private final String name;

	public enum Kind {
		TEST, GENERATED, UNKNOWN
	};

	private static final OtpErlangAtom TEST_ATOM = new OtpErlangAtom("test");
	private static final OtpErlangAtom GENERATED_ATOM = new OtpErlangAtom(
			"generated");

	public EUnitTestFunction(final OtpErlangTuple t) {
		// -record(test, {m, f}).
		// -record(generated, {m, f, n}).
		final OtpErlangAtom kindA = (OtpErlangAtom) t.elementAt(0);
		final String kind = kindA.atomValue();
		final OtpErlangAtom moduleA = (OtpErlangAtom) t.elementAt(1);
		module = moduleA.atomValue();
		final OtpErlangAtom nameA = (OtpErlangAtom) t.elementAt(2);
		name = nameA.atomValue();
		if ("test".equals(kind)) {
			this.kind = Kind.TEST;
		} else if ("generated".equals(kind)) {
			this.kind = Kind.GENERATED;
		} else {
			this.kind = Kind.UNKNOWN;
		}
	}

	public String getName() {
		return name;
	}

	public String getModule() {
		return module;
	}

	public OtpErlangObject getTuple() {
		final OtpErlangAtom moduleA = new OtpErlangAtom(module);
		final OtpErlangAtom nameA = new OtpErlangAtom(name);
		final OtpErlangAtom a;
		switch (kind) {
		case TEST:
			a = TEST_ATOM;
			break;
		case GENERATED:
			a = GENERATED_ATOM;
			break;
		default:
			return null;
		}
		return new OtpErlangTuple(new OtpErlangObject[] { a, moduleA, nameA });
	}

	public Kind getKind() {
		return kind;
	}
}
