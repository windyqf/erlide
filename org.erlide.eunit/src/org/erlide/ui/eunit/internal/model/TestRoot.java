/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.erlide.ui.eunit.internal.model;

import org.erlide.ui.eunit.model.ITestRunSession;

public class TestRoot extends TestSuiteElement {

    private final ITestRunSession fSession;

    public TestRoot(final ITestRunSession session) {
        super(null, "-1", session.getTestRunName(), 1); //$NON-NLS-1$
        fSession = session;
    }

    @Override
    public TestRoot getRoot() {
        return this;
    }

    @Override
    public ITestRunSession getTestRunSession() {
        return fSession;
    }
}
