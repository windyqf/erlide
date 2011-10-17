/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/

package org.erlide.ui.eunit.internal.launcher;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.erlide.core.model.root.IErlElement;
import org.erlide.core.model.root.IErlProject;
import org.erlide.eunit.EUnitTestFunction;

/**
 * Interface to be implemented by for extension point
 * org.eclipse.jdt.junit.internal_testKinds.
 */
public interface IErlangTestFinder {
    // ITestFinder NULL= new ITestFinder() {
    // public void findTestsInContainer(IErlElement element, Set result,
    // IProgressMonitor pm) {
    // // do nothing
    // }
    //
    // public boolean isTest(IType type) {
    // return false;
    // }
    // };

    public List<EUnitTestFunction> findTestsInContainer(
            final IErlProject project, IErlElement parent, IProgressMonitor pm)
            throws CoreException;

    // public boolean isTest(IErlElement element) throws CoreException;
}
