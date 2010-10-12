/*******************************************************************************
 * Copyright (c) 2005 Vlad Dumitrescu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vlad Dumitrescu
 *******************************************************************************/
package org.erlide.ui.launch;

import java.util.List;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

import com.google.common.collect.Lists;

public class ErlangNodeTabGroup extends AbstractLaunchConfigurationTabGroup {

    public List<ILaunchConfigurationTab> createMyTabs(
            final ILaunchConfigurationDialog dialog, final String mode) {
        final List<ILaunchConfigurationTab> tabs = Lists.newArrayList();
        tabs.add(new ErlangMainTab());
        tabs.add(new RuntimeTab());
        if (mode.equals("debug")) {
            tabs.add(new DebugTab());
        }
        tabs.add(new CodepathTab());
        return tabs;
    }

    public void createTabs(final ILaunchConfigurationDialog dialog,
            final String mode) {
        final List<ILaunchConfigurationTab> tabs = Lists
                .newArrayList(createMyTabs(dialog, mode));
        tabs.add(new EnvironmentTab());
        tabs.add(new CommonTab());
        setTabs(tabs.toArray(new ILaunchConfigurationTab[0]));
    }
}
