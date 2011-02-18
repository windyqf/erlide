package org.erlide.ui.editors.erl.actions;

import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;
import org.erlide.backend.Backend;
import org.erlide.backend.BackendException;
import org.erlide.backend.util.Util;
import org.erlide.core.erlang.ErlangCore;
import org.erlide.ui.editors.erl.autoedit.AutoIndentStrategy;
import org.erlide.ui.editors.erl.autoedit.SmartTypingPreferencePage;
import org.erlide.ui.prefs.plugin.IndentationPreferencePage;

import com.ericsson.otp.erlang.OtpErlangObject;

import erlang.ErlideIndent;

/**
 * Our sample action implements workbench action delegate. The action proxy will
 * be created by the workbench and shown in the UI. When the user tries to use
 * the action, this delegate will be created and execution will be delegated to
 * it.
 * 
 * @see IWorkbenchWindowActionDelegate
 */

public class IndentAction extends ErlangTextEditorAction {

    public IndentAction(final ResourceBundle bundle, final String prefix,
            final ITextEditor editor) {
        super(bundle, prefix, editor);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.erlide.ui.actions.ErlangTextEditorAction#callErlang(org.eclipse.jface
     * .text.ITextSelection, java.lang.String)
     */
    @Override
    protected OtpErlangObject callErlang(final int offset, final int length,
            final String text) throws BackendException {
        final OtpErlangObject r1 = doIndentLines(offset, length, text, false,
                "");
        return r1;
    }

    private static OtpErlangObject doIndentLines(final int offset,
            final int length, final String text, final boolean template,
            final String prefix) throws BackendException {
        final int tabw = AutoIndentStrategy.getTabWidthFromPreferences();
        final Map<String, String> prefs = new TreeMap<String, String>();
        IndentationPreferencePage.addKeysAndPrefs(prefs);
        SmartTypingPreferencePage.addAutoNLKeysAndPrefs(prefs);
        final Backend b = ErlangCore.getBackendManager().getIdeBackend();
        final boolean useTabs = AutoIndentStrategy.getUseTabsFromPreferences();
        if (template) {
            final OtpErlangObject r1 = ErlideIndent.templateIndentLines(b,
                    prefix, text, tabw, useTabs, prefs);
            return r1;
        } else {
            final OtpErlangObject r1 = ErlideIndent.indentLines(b, offset,
                    length, text, tabw, useTabs, prefs);
            return r1;
        }
    }

    public static String indentLines(final int offset, final int length,
            final String text, final boolean template, final String prefix)
            throws BackendException {
        return Util.stringValue(doIndentLines(offset, length, text, template,
                prefix));
    }
}
