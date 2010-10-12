/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Brock Janiczak (brockj@tpg.com.au)
 *         - https://bugs.eclipse.org/bugs/show_bug.cgi?id=102236: [JUnit] display execution time next to each test
 *******************************************************************************/

package org.erlide.ui.eunit.internal.ui;

import java.text.MessageFormat;
import java.text.NumberFormat;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.erlide.ui.eunit.internal.model.TestCaseElement;
import org.erlide.ui.eunit.internal.model.TestElement.Status;
import org.erlide.ui.eunit.internal.model.TestSuiteElement;
import org.erlide.ui.eunit.model.ITestCaseElement;
import org.erlide.ui.eunit.model.ITestElement;
import org.erlide.ui.eunit.model.ITestSuiteElement;

public class TestSessionLabelProvider extends LabelProvider implements
		IStyledLabelProvider {

	private final TestRunnerViewPart fTestRunnerPart;
	private final int fLayoutMode;
	private final NumberFormat timeFormat;

	private boolean fShowTime;

	public TestSessionLabelProvider(final TestRunnerViewPart testRunnerPart,
			final int layoutMode) {
		fTestRunnerPart = testRunnerPart;
		fLayoutMode = layoutMode;
		fShowTime = true;

		timeFormat = NumberFormat.getNumberInstance();
		timeFormat.setGroupingUsed(true);
		timeFormat.setMinimumFractionDigits(3);
		timeFormat.setMaximumFractionDigits(3);
		timeFormat.setMinimumIntegerDigits(1);
	}

	public StyledString getStyledText(final Object element) {
		final String label = getSimpleLabel(element);
		if (label == null) {
			return new StyledString(element.toString());
		}
		StyledString text = new StyledString(label);

		final ITestElement testElement = (ITestElement) element;
		if (fLayoutMode == TestRunnerViewPart.LAYOUT_HIERARCHICAL) {
			// if (testElement.getParentContainer() instanceof ITestRunSession)
			// {
			// final String testKindDisplayName = fTestRunnerPart
			// .getTestKindDisplayName();
			// if (testKindDisplayName != null) {
			// final String decorated = MessageFormat
			// .format(JUnitMessages.TestSessionLabelProvider_testName_JUnitVersion,
			// new Object[] { label, testKindDisplayName });
			// text = StyledCellLabelProvider.styleDecoratedString(
			// decorated, StyledString.QUALIFIER_STYLER, text);
			// }
			// }

		} else {
			if (element instanceof ITestCaseElement) {
				final String moduleName = ((ITestCaseElement) element)
						.getTestModuleName();
				final String decorated = MessageFormat.format("{0} - {1}",
						label, moduleName);
				text = StyledCellLabelProvider.styleDecoratedString(decorated,
						StyledString.QUALIFIER_STYLER, text);
			}
		}
		return addElapsedTime(text, testElement.getElapsedTimeInSeconds());
	}

	private StyledString addElapsedTime(final StyledString styledString,
			final double time) {
		final String string = styledString.getString();
		final String decorated = addElapsedTime(string, time);
		return StyledCellLabelProvider.styleDecoratedString(decorated,
				StyledString.COUNTER_STYLER, styledString);
	}

	private String addElapsedTime(final String string, final double time) {
		if (!fShowTime || Double.isNaN(time)) {
			return string;
		}
		final String formattedTime = timeFormat.format(time);
		return MessageFormat.format("{0} ({1} s)", string, formattedTime);
	}

	private String getSimpleLabel(final Object element) {
		if (element instanceof ITestCaseElement) {
			return ((ITestCaseElement) element).getTestFunctionName();
		} else if (element instanceof ITestSuiteElement) {
			return ((ITestSuiteElement) element).getSuiteTypeName();
		}
		return null;
	}

	@Override
	public String getText(final Object element) {
		String label = getSimpleLabel(element);
		if (label == null) {
			return element.toString();
		}
		final ITestElement testElement = (ITestElement) element;
		if (fLayoutMode == TestRunnerViewPart.LAYOUT_HIERARCHICAL) {
			// if (testElement.getParentContainer() instanceof ITestRunSession)
			// {
			// final String testKindDisplayName = fTestRunnerPart
			// .getTestKindDisplayName();
			// if (testKindDisplayName != null) {
			// label = Messages
			// .format(JUnitMessages.TestSessionLabelProvider_testName_JUnitVersion,
			// new Object[] { label, testKindDisplayName });
			// }
			// }
		} else {
			if (element instanceof ITestCaseElement) {
				final String className = ((ITestCaseElement) element)
						.getTestModuleName();
				label = MessageFormat.format("{0} - {1}", label, className);
			}
		}
		return addElapsedTime(label, testElement.getElapsedTimeInSeconds());
	}

	@Override
	public Image getImage(final Object element) {
		if (element instanceof TestCaseElement) {
			final TestCaseElement testCaseElement = (TestCaseElement) element;
			if (testCaseElement.isIgnored()) {
				return fTestRunnerPart.fTestIgnoredIcon;
			}

			final Status status = testCaseElement.getStatus();
			if (status.isNotRun()) {
				return fTestRunnerPart.fTestIcon;
			} else if (status.isRunning()) {
				return fTestRunnerPart.fTestRunningIcon;
			} else if (status.isError()) {
				return fTestRunnerPart.fTestErrorIcon;
			} else if (status.isFailure()) {
				return fTestRunnerPart.fTestFailIcon;
			} else if (status.isOK()) {
				return fTestRunnerPart.fTestOkIcon;
			} else {
				throw new IllegalStateException(element.toString());
			}

		} else if (element instanceof TestSuiteElement) {
			final Status status = ((TestSuiteElement) element).getStatus();
			if (status.isNotRun()) {
				return fTestRunnerPart.fSuiteIcon;
			} else if (status.isRunning()) {
				return fTestRunnerPart.fSuiteRunningIcon;
			} else if (status.isError()) {
				return fTestRunnerPart.fSuiteErrorIcon;
			} else if (status.isFailure()) {
				return fTestRunnerPart.fSuiteFailIcon;
			} else if (status.isOK()) {
				return fTestRunnerPart.fSuiteOkIcon;
			} else {
				throw new IllegalStateException(element.toString());
			}

		} else {
			throw new IllegalArgumentException(String.valueOf(element));
		}
	}

	public void setShowTime(final boolean showTime) {
		fShowTime = showTime;
		fireLabelProviderChanged(new LabelProviderChangedEvent(this));
	}

}
