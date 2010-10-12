/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids: sdavids@gmx.de bug 37333 Failure Trace cannot
 * 			navigate to non-public class in CU throwing Exception
 *******************************************************************************/
package org.erlide.ui.eunit.internal.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.ITextEditor;
import org.erlide.core.erlang.IErlElement;
import org.erlide.core.erlang.IErlProject;

/**
 * Open a test in the Java editor and reveal a given line
 */
public class OpenEditorAtLineAction extends OpenEditorAction {

	private final int fLineNumber;

	public OpenEditorAtLineAction(final TestRunnerViewPart testRunner,
			final String moduleName, final int line) {
		super(testRunner, moduleName);
		// TODO PlatformUI.getWorkbench().getHelpSystem()
		// .setHelp(this, IJUnitHelpContextIds.OPENEDITORATLINE_ACTION);
		fLineNumber = line;
	}

	@Override
	protected void reveal(final ITextEditor textEditor) {
		if (fLineNumber >= 0) {
			try {
				final IDocument document = textEditor.getDocumentProvider()
						.getDocument(textEditor.getEditorInput());
				textEditor.selectAndReveal(
						document.getLineOffset(fLineNumber - 1),
						document.getLineLength(fLineNumber - 1));
			} catch (final BadLocationException x) {
				// marker refers to invalid text position -> do nothing
			}
		}
	}

	protected IErlElement findElement(final IErlProject project,
			final String moduleName) throws CoreException {
		return findModule(project, moduleName);
	}

}
