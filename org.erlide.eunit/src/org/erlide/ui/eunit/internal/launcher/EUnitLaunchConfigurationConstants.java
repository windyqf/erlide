/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.erlide.ui.eunit.internal.launcher;

import org.erlide.core.ErlangPlugin;

/**
 * Attribute keys used by the IJUnitLaunchConfiguration. Note that these
 * constants are not API and might change in the future.
 */
public class EUnitLaunchConfigurationConstants {

	public static final String MODE_RUN_QUIETLY_MODE = "runQuietly"; //$NON-NLS-1$
	public static final String ID_JUNIT_APPLICATION = "org.eclipse.jdt.junit.launchconfig"; //$NON-NLS-1$

	public static final String ATTR_NO_DISPLAY = ErlangPlugin.PLUGIN_ID
			+ ".NO_DISPLAY"; //$NON-NLS-1$

	public static final String ATTR_PORT = ErlangPlugin.PLUGIN_ID + ".PORT"; //$NON-NLS-1$

	public static final String ATTR_TEST_PROJECT = ErlangPlugin.PLUGIN_ID
			+ ".TESTPROJECT"; // $NON-NLS-1$

	public static final String ATTR_TEST_MODULE = ErlangPlugin.PLUGIN_ID
			+ ".TESTMODULE"; //$NON-NLS-1$

	public static final String ATTR_TEST_FUNCTION = ErlangPlugin.PLUGIN_ID
			+ ".TESTKIND"; // $NON-NLS-1$

	public static final String ATTR_KEEPRUNNING = ErlangPlugin.PLUGIN_ID
			+ ".KEEPRUNNING_ATTR"; //$NON-NLS-1$
	// /**
	// * The launch container, or "" iff running a single test type.
	// */
	// public static final String ATTR_TEST_CONTAINER = ErlangPlugin.PLUGIN_ID
	//            + ".CONTAINER"; //$NON-NLS-1$

	public static final String ATTR_FAILURES_NAMES = ErlangPlugin.PLUGIN_ID
			+ ".FAILURENAMES"; //$NON-NLS-1$

	public static final String ATTR_TEST_RUNNER_KIND = ErlangPlugin.PLUGIN_ID
			+ ".TEST_KIND"; //$NON-NLS-1$

}
