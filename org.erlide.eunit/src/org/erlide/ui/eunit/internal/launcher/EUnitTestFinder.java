/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/
package org.erlide.ui.eunit.internal.launcher;

import java.util.EnumSet;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.erlide.core.backend.BackendCore;
import org.erlide.core.model.erlang.IErlModule;
import org.erlide.core.model.root.ErlModelException;
import org.erlide.core.model.root.IErlElement;
import org.erlide.core.model.root.IErlElementVisitor;
import org.erlide.core.model.root.IErlProject;
import org.erlide.core.rpc.IRpcCallSite;
import org.erlide.eunit.EUnitTestFunction;

import com.google.common.collect.Lists;

import erlang.ErlideEUnit;

public class EUnitTestFinder implements IErlangTestFinder {

    public List<EUnitTestFunction> findTestsInContainer(
            final IErlProject project, final IErlElement parent,
            IProgressMonitor pm) throws CoreException {
        if (parent == null) { // XXX hmmm
            throw new IllegalArgumentException();
        }
        if (pm == null) {
            pm = new NullProgressMonitor();
        }
        final IProgressMonitor monitor = pm;
        if (parent instanceof IErlElement) {
            final IErlElement element = parent;
            final List<IErlModule> modules = Lists.newArrayList();
            element.accept(new IErlElementVisitor() {

                public boolean visit(final IErlElement element)
                        throws ErlModelException {
                    if (element instanceof IErlModule) {
                        final IErlModule module = (IErlModule) element;
                        modules.add(module);
                    }
                    return true;
                }
            }, EnumSet.of(IErlElement.AcceptFlags.LEAFS_ONLY),
                    IErlElement.Kind.MODULE);
            return findTestsInModules(project, modules, new SubProgressMonitor(
                    monitor, 1));
        }
        return null;
    }

    public List<EUnitTestFunction> findTestsInModules(
            final IErlProject project, final List<IErlModule> modules,
            final IProgressMonitor pm) throws CoreException {
        try {
            final IRpcCallSite backend = BackendCore.getBackendManager()
                    .getIdeBackend();
            final List<String> beams = Lists.newArrayListWithCapacity(modules
                    .size());
            final IFolder ebin = project.getWorkspaceProject().getFolder(
                    project.getOutputLocation());
            for (final IErlModule module : modules) {
                final String beamName = module.getModuleName() + ".beam";
                final IResource beam = ebin.findMember(beamName);
                if (beam != null) {
                    beams.add(beam.getLocation().toPortableString());
                }
            }
            return ErlideEUnit.findTests(backend, beams);
        } finally {
            pm.done();
        }
    }

}
