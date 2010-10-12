/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.erlide.ui.eunit.internal.ui;

import org.eclipse.jface.action.Action;
import org.erlide.eunit.EUnitPlugin;

/**
 * Toggles console auto-scroll
 */
public class ScrollLockAction extends Action {

	private final TestRunnerViewPart fRunnerViewPart;

	public ScrollLockAction(final TestRunnerViewPart viewer) {
		super("Scroll Lock");
		fRunnerViewPart = viewer;
		setToolTipText("Scroll Lock");
		setDisabledImageDescriptor(EUnitPlugin
				.getImageDescriptor("dlcl16/lock.gif")); //$NON-NLS-1$
		setHoverImageDescriptor(EUnitPlugin
				.getImageDescriptor("elcl16/lock.gif")); //$NON-NLS-1$
		setImageDescriptor(EUnitPlugin.getImageDescriptor("elcl16/lock.gif")); //$NON-NLS-1$
		// TODO PlatformUI.getWorkbench().getHelpSystem().setHelp(
		// this,
		// IJUnitHelpContextIds.OUTPUT_SCROLL_LOCK_ACTION);
		setChecked(false);
	}

	/**
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public void run() {
		fRunnerViewPart.setAutoScroll(!isChecked());
	}
}
