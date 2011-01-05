package org.erlide.ui.eunit.internal.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.erlide.ui.eunit.internal.launcher.EUnitLaunchConfigurationConstants;
import org.erlide.ui.launch.ErlangMainTab;

public class EUnitMainTab extends ErlangMainTab {

	private Text testCaseText;
	private Button browseButton;
	protected String testCase;

	@Override
	protected void createStartGroup(final Composite comp) {
		final Group startGroup = new Group(comp, SWT.NONE);
		startGroup.setText("Test");
		final GridData gd_startGroup = new GridData(SWT.FILL, SWT.CENTER,
				false, false);
		startGroup.setLayoutData(gd_startGroup);
		final GridLayout gridLayout_1 = new GridLayout();
		gridLayout_1.numColumns = 3;
		startGroup.setLayout(gridLayout_1);

		testCaseText = textWithLabel(startGroup, "Test module or function",
				114, new ModifyListener() {

					public void modifyText(final ModifyEvent e) {
						testCase = testCaseText.getText();
						updateLaunchConfigurationDialog();
					}
				});
		browseButton = createPushButton(startGroup, "Browse...", null);
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				;
			}
		});
	}

	@Override
	protected void initializeStart(final ILaunchConfiguration config) {
		String testProject;
		try {
			testProject = config.getAttribute(
					EUnitLaunchConfigurationConstants.ATTR_TEST_PROJECT, "");
		} catch (final CoreException e) {
			testProject = "";
		}
		String testModule;
		try {
			testModule = config.getAttribute(
					EUnitLaunchConfigurationConstants.ATTR_TEST_MODULE, "");
		} catch (final CoreException e) {
			testModule = "";
		}
		String testFunction;
		try {
			testFunction = config.getAttribute(
					EUnitLaunchConfigurationConstants.ATTR_TEST_FUNCTION, "");
		} catch (final CoreException e) {
			testFunction = "";
		}
		final StringBuilder sb = new StringBuilder();
		sb.append(testProject);
		if (testModule.length() > 0) {
			sb.append(":").append(testModule);
		}
		if (testFunction.length() > 0) {
			sb.append(":").append(testFunction);
		}
		final String testString = sb.toString();
		testCaseText.setText(testString);
		testCase = testString;
	}

	@Override
	protected void applyStart(final ILaunchConfigurationWorkingCopy config) {
		final String[] strings = testCase.split(":"); // Project:Module:Function
		final String project = strings[0];
		config.setAttribute(
				EUnitLaunchConfigurationConstants.ATTR_TEST_PROJECT, project);
		final String module = strings.length > 1 ? strings[1] : "";
		config.setAttribute(EUnitLaunchConfigurationConstants.ATTR_TEST_MODULE,
				module);
		final String function = strings.length > 2 ? strings[2] : "";
		config.setAttribute(
				EUnitLaunchConfigurationConstants.ATTR_TEST_FUNCTION, function);
	}

	@Override
	public boolean isValid(final ILaunchConfiguration launchConfig) {
		if (!super.isValid(launchConfig)) {
			return false;
		}
		// FIXME use the EUnitTestFinder to find actual tests
		return testCase.length() > 0;
	}

	@Override
	public void setDefaults(final ILaunchConfigurationWorkingCopy config) {
		super.setDefaults(config);
		config.setAttribute(
				EUnitLaunchConfigurationConstants.ATTR_TEST_PROJECT, "");
		config.setAttribute(EUnitLaunchConfigurationConstants.ATTR_TEST_MODULE,
				"");
		config.setAttribute(
				EUnitLaunchConfigurationConstants.ATTR_TEST_FUNCTION, "");
	}
}
