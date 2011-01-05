package org.erlide.ui.eunit.internal.launcher;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.debug.core.ILaunch;
import org.erlide.core.util.Tuple;
import org.erlide.eunit.EUnitPlugin;
import org.erlide.jinterface.backend.Backend;
import org.erlide.jinterface.backend.events.EventHandler;
import org.erlide.jinterface.backend.util.Util;
import org.erlide.jinterface.util.ErlLogger;
import org.erlide.ui.eunit.internal.model.ITestRunListener2;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangPid;
import com.ericsson.otp.erlang.OtpErlangTuple;

public class EUnitEventHandler extends EventHandler {

	private final OtpErlangPid eventPid;
	private final ILaunch launch;
	private final Backend backend;
	private final ListenerList /* <ITestRunListener2> */listeners = new ListenerList();

	public EUnitEventHandler(final OtpErlangPid eventPid, final ILaunch launch,
			final Backend backend) {
		ErlLogger.debug(
				"adding eventhandler to eventPid %s launch %s backend %s",
				eventPid, launch, backend);
		this.eventPid = eventPid;
		this.launch = launch;
		this.backend = backend;
		EUnitPlugin.getModel().addEventHandler(this);
	}

	private enum EUnitMsgWhat {
		test_begin, test_end, test_cancel, group_begin, group_end, group_cancel, terminated, run_started;

		static Set<String> allNames() {
			final EUnitMsgWhat[] values = values();
			final HashSet<String> result = new HashSet<String>(values.length);
			for (final EUnitMsgWhat value : values) {
				result.add(value.name());
			}
			return result;
		}
	}

	private interface AllListeners {
		void apply(ITestRunListener2 listener);
	}

	private void allListeners(final AllListeners application) {
		for (final Object listener : listeners.getListeners()) {
			application.apply((ITestRunListener2) listener);
		}
	}

	Set<String> allMsgWhats = EUnitMsgWhat.allNames();

	@Override
	protected void doHandleMsg(final OtpErlangObject msg) throws Exception {
		ErlLogger.debug("EUnitEventHandler %s", msg);
		if (msg instanceof OtpErlangTuple) {
			final Tuple<EUnitMsgWhat, OtpErlangTuple> eunitMsg = getEUnitMsg(msg);
			if (eunitMsg == null) {
				ErlLogger.error("EUnitEventHandler unknown msg '%s'", msg);
				return;
			}
			final EUnitMsgWhat what = eunitMsg.o1;
			final OtpErlangTuple argument = eunitMsg.o2;
			AllListeners al = null;
			switch (what) {
			case group_begin:
				al = new AllListeners() {

					public void apply(final ITestRunListener2 listener) {
						final String name = Util.stringValue(argument
								.elementAt(1));
						final String id = name; // FIXME
						listener.testTreeEntry(id);
						listener.testStarted(id, name);
					}
				};
				break;
			case group_end:
				al = new AllListeners() {

					public void apply(final ITestRunListener2 listener) {
						final String name = Util.stringValue(argument
								.elementAt(1));
						final String id = name; // FIXME
						listener.testEnded(id, name);
					}
				};
				break;
			case group_cancel:
				al = new AllListeners() {

					public void apply(final ITestRunListener2 listener) {
						final String name = Util.stringValue(argument
								.elementAt(1));
						final String id = name; // FIXME
						listener.testEnded(id, name);
					}
				};
				break;
			case test_begin:
				al = new AllListeners() {

					public void apply(final ITestRunListener2 listener) {
						final String testName = Util.stringValue(argument
								.elementAt(0));
						listener.testTreeEntry(testName);
						listener.testStarted(testName, testName);
					}
				};
				break;
			case test_end:
				al = new AllListeners() {
					public void apply(final ITestRunListener2 listener) {
						final String name = Util.stringValue(argument
								.elementAt(1));
						final String id = name; // FIXME
						final OtpErlangObject testResult = argument
								.elementAt(3);
						if (Util.isOk(testResult)) {
							listener.testEnded(id, name);
						} else {
							final OtpErlangTuple failureT = (OtpErlangTuple) testResult;
							final String expected = getValueExpected(
									testResult, "expected");
							final String value = getValueExpected(testResult,
									"value");
							listener.testFailed(
									ITestRunListener2.STATUS_FAILURE, id, name,
									failureT.elementAt(1).toString(), expected,
									value);
						}
					}
				};
				break;
			case test_cancel:
				al = new AllListeners() {
					public void apply(final ITestRunListener2 listener) {
						final String name = Util.stringValue(argument
								.elementAt(1));
						final String id = name; // FIXME
						listener.testEnded(id, name);
					}
				};
				break;
			case run_started:
				al = new AllListeners() {
					public void apply(final ITestRunListener2 listener) {
						listener.testRunStarted(0);
					}
				};
				break;
			case terminated:
				al = new AllListeners() {
					public void apply(final ITestRunListener2 listener) {
						listener.testRunEnded(0);
					}
				};
				break;
			}
			if (al != null) {
				allListeners(al);
			}
		}
	}

	protected static String getValueExpected(final OtpErlangObject testResult,
			final String what) {
		final OtpErlangTuple t = (OtpErlangTuple) testResult;
		final OtpErlangTuple t2 = (OtpErlangTuple) t.elementAt(1);
		final OtpErlangTuple t3 = (OtpErlangTuple) t2.elementAt(1);
		final OtpErlangList l = (OtpErlangList) t3.elementAt(1);
		for (final OtpErlangObject i : l) {
			if (i instanceof OtpErlangTuple) {
				final OtpErlangTuple et = (OtpErlangTuple) i;
				final OtpErlangObject o = et.elementAt(0);
				if (o instanceof OtpErlangAtom) {
					final OtpErlangAtom whatA = (OtpErlangAtom) o;
					if (whatA.atomValue().equals(what)) {
						return et.elementAt(1).toString();
					}
				}
			}
		}
		return what;
	}

	private Tuple<EUnitMsgWhat, OtpErlangTuple> getEUnitMsg(
			final OtpErlangObject msg) {
		if (!(msg instanceof OtpErlangTuple)) {
			return null;
		}
		final OtpErlangTuple t = (OtpErlangTuple) msg;
		if (t.arity() != 3) {
			return null;
		}
		final OtpErlangPid jpid = (OtpErlangPid) t.elementAt(1);
		if (!jpid.equals(eventPid)) {
			return null;
		}
		final OtpErlangAtom whatA = (OtpErlangAtom) t.elementAt(0);
		final String what = whatA.atomValue();
		if (!allMsgWhats.contains(what)) {
			return null;
		}
		final OtpErlangTuple argumentT = (OtpErlangTuple) t.elementAt(2);
		return new Tuple<EUnitMsgWhat, OtpErlangTuple>(
				EUnitMsgWhat.valueOf(what), argumentT);
	}

	public ILaunch getLaunch() {
		return launch;
	}

	public void addListener(final ITestRunListener2 listener) {
		listeners.add(listener);
	}

	public void removeListener(final ITestRunListener2 listener) {
		listeners.remove(listener);
	}

	public void shutdown() {
		listeners.clear();
		backend.getEventDaemon().removeHandler(this);
	}
}
