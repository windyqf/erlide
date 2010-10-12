package org.erlide.eunit;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.erlide.ui.eunit.internal.model.EUnitModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class EUnitPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.erlide.eunit"; //$NON-NLS-1$

	private static final String HISTORY_DIR_NAME = "history";

	private static final String ICONS_PATH = "/icons/full/obj16/";

	// The shared instance
	private static EUnitPlugin plugin;

	private static EUnitModel fEUnitModel = null;

	private final ListenerList fNewTestRunListeners;

	/**
	 * The constructor
	 */
	public EUnitPlugin() {
		fNewTestRunListeners = new ListenerList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static EUnitPlugin getDefault() {
		return plugin;
	}

	public static File getHistoryDirectory() {
		final File historyDir = getDefault().getStateLocation()
				.append(HISTORY_DIR_NAME).toFile();
		if (!historyDir.isDirectory()) {
			historyDir.mkdir();
		}
		return historyDir;
	}

	protected void createImageDescriptor(final String id, final URL baseURL) {
		URL url = null;
		try {
			url = new URL(baseURL, ICONS_PATH + id);
		} catch (final MalformedURLException e) {
			// ignore exception
		}

		getImageRegistry().put(id, ImageDescriptor.createFromURL(url));
	}

	public IDialogSettings getDialogSettingsSection(final String name) {
		final IDialogSettings dialogSettings = getDialogSettings();
		IDialogSettings section = dialogSettings.getSection(name);
		if (section == null) {
			section = dialogSettings.addNewSection(name);
		}
		return section;
	}

	public static ImageDescriptor getImageDescriptor(final String relativePath) {
		final IPath path = new Path(ICONS_PATH).append(relativePath);
		return createImageDescriptor(getDefault().getBundle(), path, true);
	}

	private static ImageDescriptor createImageDescriptor(final Bundle bundle,
			final IPath path, final boolean useMissingImageDescriptor) {
		final URL url = FileLocator.find(bundle, path, null);
		if (url != null) {
			return ImageDescriptor.createFromURL(url);
		}
		if (useMissingImageDescriptor) {
			return ImageDescriptor.getMissingImageDescriptor();
		}
		return null;
	}

	public static Image createImage(final String path) {
		return getImageDescriptor(path).createImage();
	}

	public static void setLocalImageDescriptors(final IAction action,
			final String iconName) {
		setImageDescriptors(action, "lcl16", iconName); //$NON-NLS-1$
	}

	private static void setImageDescriptors(final IAction action,
			final String type, final String relPath) {
		final ImageDescriptor id = createImageDescriptor(
				"d" + type, relPath, false); //$NON-NLS-1$
		if (id != null) {
			action.setDisabledImageDescriptor(id);
		}

		final ImageDescriptor descriptor = createImageDescriptor(
				"e" + type, relPath, true); //$NON-NLS-1$
		action.setHoverImageDescriptor(descriptor);
		action.setImageDescriptor(descriptor);
	}

	/*
	 * Creates an image descriptor for the given prefix and name in the JDT UI
	 * bundle. The path can contain variables like $NL$. If no image could be
	 * found, <code>useMissingImageDescriptor</code> decides if either the
	 * 'missing image descriptor' is returned or <code>null</code>. or
	 * <code>null</code>.
	 */
	private static ImageDescriptor createImageDescriptor(
			final String pathPrefix, final String imageName,
			final boolean useMissingImageDescriptor) {
		final IPath path = new Path(ICONS_PATH).append(pathPrefix).append(
				imageName);
		return createImageDescriptor(EUnitPlugin.getDefault().getBundle(),
				path, useMissingImageDescriptor);
	}

	private EUnitModel getEUnitModel() {
		if (fEUnitModel == null) {
			fEUnitModel = new EUnitModel();
		}
		return fEUnitModel;
	}

	public static EUnitModel getModel() {
		return getDefault().getEUnitModel();
	}

	public ListenerList getTestRunListeners() {
		return fNewTestRunListeners;
	}

	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		final IWorkbench workBench = getDefault().getWorkbench();
		if (workBench == null) {
			return null;
		}
		return workBench.getActiveWorkbenchWindow();
	}

	public static Shell getActiveWorkbenchShell() {
		final IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window != null) {
			return window.getShell();
		}
		return null;
	}

	public static IWorkbenchPage getActivePage() {
		final IWorkbenchWindow activeWorkbenchWindow = getActiveWorkbenchWindow();
		if (activeWorkbenchWindow == null) {
			return null;
		}
		return activeWorkbenchWindow.getActivePage();
	}

}
