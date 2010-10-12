/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation
 *          (report 36180: Callers/Callees view)
 *******************************************************************************/
package org.erlide.ui.eunit.internal.ui.viewsupport;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.erlide.ui.dialogfields.DialogField;
import org.erlide.ui.dialogfields.IDialogFieldListener;
import org.erlide.ui.dialogfields.IListAdapter;
import org.erlide.ui.dialogfields.LayoutUtil;
import org.erlide.ui.dialogfields.ListDialogField;
import org.erlide.ui.dialogfields.Separator;
import org.erlide.ui.dialogfields.StringDialogField;
import org.erlide.ui.util.StatusInfo;

/*package*/class HistoryListAction extends Action {

	private class HistoryListDialog extends StatusDialog {
		private static final int MAX_MAX_ENTRIES = 100;
		private ListDialogField<Object> fHistoryList;
		private StringDialogField fMaxEntriesField;
		private int fMaxEntries;

		private Object fResult;

		private HistoryListDialog() {
			super(fHistory.getShell());
			setTitle(fHistory.getHistoryListDialogTitle());

			createHistoryList();
			createMaxEntriesField();
			setHelpAvailable(false);
		}

		/*
		 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
		 * 
		 * @since 3.4
		 */
		@Override
		protected boolean isResizable() {
			return true;
		}

		private void createHistoryList() {
			final IListAdapter<Object> adapter = new IListAdapter<Object>() {
				public void customButtonPressed(
						final ListDialogField<Object> field, final int index) {
					doCustomButtonPressed(index);
				}

				public void selectionChanged(final ListDialogField<Object> field) {
					doSelectionChanged();
				}

				public void doubleClicked(final ListDialogField<Object> field) {
					doDoubleClicked();
				}
			};
			final String[] buttonLabels = new String[] { "&Remove",
					"Remove &All" };
			final LabelProvider labelProvider = new TestRunLabelProvider();
			fHistoryList = new ListDialogField(adapter, buttonLabels,
					labelProvider);
			fHistoryList.setLabelText(fHistory.getHistoryListDialogMessage());

			final List historyEntries = fHistory.getHistoryEntries();
			fHistoryList.setElements(historyEntries);

			final Object currentEntry = fHistory.getCurrentEntry();
			ISelection sel;
			if (currentEntry != null) {
				sel = new StructuredSelection(currentEntry);
			} else {
				sel = new StructuredSelection();
			}
			fHistoryList.selectElements(sel);
		}

		private void createMaxEntriesField() {
			fMaxEntriesField = new StringDialogField();
			fMaxEntriesField.setLabelText(fHistory.getMaxEntriesMessage());
			fMaxEntriesField.setDialogFieldListener(new IDialogFieldListener() {
				public void dialogFieldChanged(final DialogField field) {
					final String maxString = fMaxEntriesField.getText();
					boolean valid;
					try {
						fMaxEntries = Integer.parseInt(maxString);
						valid = fMaxEntries > 0
								&& fMaxEntries < MAX_MAX_ENTRIES;
					} catch (final NumberFormatException e) {
						valid = false;
					}
					if (valid) {
						updateStatus(new StatusInfo());
					} else {
						updateStatus(new StatusInfo(
								IStatus.ERROR,
								MessageFormat
										.format("Please enter a positive integer smaller than {0}.",
												Integer.toString(MAX_MAX_ENTRIES))));
					}
				}
			});
			fMaxEntriesField
					.setText(Integer.toString(fHistory.getMaxEntries()));
		}

		/*
		 * @see Dialog#createDialogArea(Composite)
		 */
		@Override
		protected Control createDialogArea(final Composite parent) {
			initializeDialogUnits(parent);

			final Composite composite = (Composite) super
					.createDialogArea(parent);

			final Composite inner = new Composite(composite, SWT.NONE);
			inner.setLayoutData(new GridData(GridData.FILL_BOTH));
			inner.setFont(composite.getFont());

			LayoutUtil.doDefaultLayout(inner, new DialogField[] { fHistoryList,
					new Separator() }, true);
			LayoutUtil.setHeightHint(fHistoryList.getListControl(null),
					convertHeightInCharsToPixels(12));
			LayoutUtil.setHorizontalGrabbing(fHistoryList.getListControl(null));

			final Composite additionalControls = new Composite(inner, SWT.NONE);
			additionalControls.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
					true, false));
			LayoutUtil.doDefaultLayout(additionalControls,
					new DialogField[] { fMaxEntriesField }, false);
			LayoutUtil.setHorizontalGrabbing(fMaxEntriesField
					.getTextControl(null));

			applyDialogFont(composite);
			return composite;
		}

		private void doCustomButtonPressed(final int index) {
			switch (index) {
			case 0: // remove
				fHistoryList.removeElements(fHistoryList.getSelectedElements());
				fHistoryList.selectFirstElement();
				break;

			case 1: // remove all
				fHistoryList.removeAllElements();
				break;
			default:
				break;
			}
		}

		private void doDoubleClicked() {
			okPressed();
		}

		private void doSelectionChanged() {
			final List selected = fHistoryList.getSelectedElements();
			if (selected.size() >= 1) {
				fResult = selected.get(0);
			} else {
				fResult = null;
			}
			fHistoryList.enableButton(0, selected.size() != 0);
		}

		public Object getResult() {
			return fResult;
		}

		public List getRemaining() {
			return fHistoryList.getElements();
		}

		public int getMaxEntries() {
			return fMaxEntries;
		}

	}

	private final class TestRunLabelProvider extends LabelProvider {
		private final HashMap fImages = new HashMap();

		@Override
		public String getText(final Object element) {
			return fHistory.getText(element);
		}

		@Override
		public Image getImage(final Object element) {
			final ImageDescriptor imageDescriptor = fHistory
					.getImageDescriptor(element);
			return getCachedImage(imageDescriptor);
		}

		private Image getCachedImage(final ImageDescriptor imageDescriptor) {
			final Object cached = fImages.get(imageDescriptor);
			if (cached != null) {
				return (Image) cached;
			}
			final Image image = imageDescriptor.createImage(fHistory.getShell()
					.getDisplay());
			fImages.put(imageDescriptor, image);
			return image;
		}

		@Override
		public void dispose() {
			for (final Iterator iter = fImages.values().iterator(); iter
					.hasNext();) {
				final Image image = (Image) iter.next();
				image.dispose();
			}
			fImages.clear();
		}
	}

	private final ViewHistory fHistory;

	public HistoryListAction(final ViewHistory history) {
		super(null, IAction.AS_RADIO_BUTTON);
		fHistory = history;
		fHistory.configureHistoryListAction(this);
	}

	/*
	 * @see IAction#run()
	 */
	@Override
	public void run() {
		final HistoryListDialog dialog = new HistoryListDialog();
		if (dialog.open() == Window.OK) {
			fHistory.setHistoryEntries(dialog.getRemaining(),
					dialog.getResult());
			fHistory.setMaxEntries(dialog.getMaxEntries());
		}
	}

}
