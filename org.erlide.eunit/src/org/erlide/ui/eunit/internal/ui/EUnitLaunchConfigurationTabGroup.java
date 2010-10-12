package org.erlide.ui.eunit.internal.ui;

import java.util.List;

import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.erlide.ui.launch.CodepathTab;
import org.erlide.ui.launch.DebugTab;
import org.erlide.ui.launch.ErlangNodeTabGroup;
import org.erlide.ui.launch.RuntimeTab;

import com.google.common.collect.Lists;

public class EUnitLaunchConfigurationTabGroup extends ErlangNodeTabGroup {

	@Override
	public List<ILaunchConfigurationTab> createMyTabs(
			final ILaunchConfigurationDialog dialog, final String mode) {
		final List<ILaunchConfigurationTab> tabs = Lists.newArrayList();
		tabs.add(new EUnitMainTab());
		tabs.add(new RuntimeTab());
		if (mode.equals("debug")) {
			tabs.add(new DebugTab());
		}
		tabs.add(new CodepathTab());
		return tabs;
	}

}
