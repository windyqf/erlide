package org.erlide.core.model.erlang.internal;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.erlide.core.ErlangCore;
import org.erlide.core.model.erlang.ErlModelException;
import org.erlide.core.model.erlang.IErlExternal;
import org.erlide.core.model.erlang.IErlModelManager;
import org.erlide.core.model.erlang.IErlModule;
import org.erlide.core.model.erlang.IErlProject;
import org.erlide.core.model.erlang.IParent;
import org.erlide.core.model.erlang.util.CoreUtil;
import org.erlide.core.rpc.RpcCallSite;
import org.erlide.core.services.search.ErlideOpen;
import org.erlide.core.services.search.ErlideOpen.ExternalTreeEntry;
import org.erlide.jinterface.ErlLogger;

import com.ericsson.otp.erlang.OtpErlangList;
import com.google.common.collect.Maps;

public class ErlExternalReferenceEntryList extends Openable implements
        IErlExternal {

    private final String externalIncludes, externalModules;
    private final String externalName;
    private final List<String> projectIncludes;

    public ErlExternalReferenceEntryList(final IParent parent,
            final String name, final String externalName,
            final String externalIncludes, final List<String> projectIncludes,
            final String externalModules) {
        super(parent, name);
        this.externalName = externalName;
        this.externalIncludes = externalIncludes;
        this.projectIncludes = projectIncludes;
        this.externalModules = externalModules;
    }

    public Kind getKind() {
        return Kind.EXTERNAL;
    }

    @Override
    protected boolean buildStructure(final IProgressMonitor pm)
            throws ErlModelException {
        // TODO some code duplication within this function
        ErlLogger.debug("ErlExternalReferenceEntryList.buildStructure %s",
                externalName);
        final ErlModelCache cache = ErlModel.getErlModelCache();
        List<ExternalTreeEntry> externalModuleTree = cache
                .getExternalTree(externalModules);
        List<ExternalTreeEntry> externalIncludeTree = cache
                .getExternalTree(externalIncludes);
        if (externalModuleTree == null || externalIncludeTree == null) {
            final RpcCallSite backend = CoreUtil
                    .getBuildOrIdeBackend(getProject().getWorkspaceProject());
            final OtpErlangList pathVars = ErlangCore.getModel().getPathVars();
            if (externalModuleTree == null && externalModules.length() > 0) {
                if (pm != null) {
                    pm.worked(1);
                }
                externalModuleTree = ErlideOpen.getExternalModuleTree(backend,
                        externalModules, pathVars);
            }
            if (externalIncludeTree == null && externalIncludes.length() > 0) {
                if (pm != null) {
                    pm.worked(1);
                }
                externalIncludeTree = ErlideOpen.getExternalModuleTree(backend,
                        externalIncludes, pathVars);
            }
        }
        final IErlModelManager modelManager = ErlangCore.getModelManager();
        setChildren(null);
        final IErlProject project = (IErlProject) getAncestorOfKind(Kind.PROJECT);
        if (externalModuleTree != null && !externalModuleTree.isEmpty()) {
            addExternalEntries(pm, externalModuleTree, modelManager, "modules",
                    externalModules, null, false);
            cache.putExternalTree(externalModules, project, externalModuleTree);
        }
        if (externalIncludeTree != null && !externalIncludeTree.isEmpty()
                || !projectIncludes.isEmpty()) {
            addExternalEntries(pm, externalIncludeTree, modelManager,
                    "includes", externalIncludes, projectIncludes, true);
            if (externalIncludeTree != null) {
                cache.putExternalTree(externalIncludes, project,
                        externalIncludeTree);
            }
        }
        return true;
    }

    private void addExternalEntries(final IProgressMonitor pm,
            final List<ExternalTreeEntry> externalTree,
            final IErlModelManager modelManager, final String rootName,
            final String rootEntry, final List<String> otherItems,
            final boolean includeDir) throws ErlModelException {
        final Map<String, IErlExternal> pathToEntryMap = Maps.newHashMap();
        pathToEntryMap.put("root", this);
        IErlExternal parent = null;
        if (externalTree != null && !externalTree.isEmpty()) {
            for (final ExternalTreeEntry entry : externalTree) {
                final String path = entry.getPath();
                // final String name = entry.getName();
                parent = pathToEntryMap.get(entry.getParentPath());
                if (entry.isModule()) {
                    final IErlModule module = modelManager.getModuleFromFile(
                            parent, getNameFromPath(path), null, path, path);
                    parent.addChild(module);
                } else {
                    final String name = getNameFromExternalPath(path);
                    final ErlExternalReferenceEntry externalReferenceEntry = new ErlExternalReferenceEntry(
                            parent, name, path, true, includeDir);
                    pathToEntryMap.put(path, externalReferenceEntry);
                    externalReferenceEntry.open(pm);
                    parent.addChild(externalReferenceEntry);
                }
            }
        }
        if (otherItems != null) {
            if (parent == null) {
                parent = new ErlExternalReferenceEntry(this, rootName,
                        rootEntry, true, includeDir);
                addChild(parent);
            }
            for (final String path : otherItems) {
                final IErlModule module = modelManager.getModuleFromFile(
                        parent, getNameFromPath(path), null, path, path);
                parent.addChild(module);
            }
        }
    }

    private String getNameFromPath(final String path) {
        final IPath p = new Path(path);
        final String name = p.lastSegment();
        return name;
    }

    private static String getNameFromExternalPath(String path) {
        int i = path.indexOf(".settings");
        if (i > 2) {
            path = path.substring(0, i - 1);
        }
        i = path.lastIndexOf('/');
        path = path.substring(i + 1);
        if (path.endsWith(".erlidex")) {
            path = path.substring(0, path.length() - 8);
        }
        return path;
    }

    @Override
    protected void closing(final Object info) throws ErlModelException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isOpen() {
        return super.isOpen();
    }

    @Override
    public String getFilePath() {
        return null;
    }

    @Override
    public String getLabelString() {
        return getName();
    }

    public String getExternalName() {
        return externalName;
    }

    public boolean hasModuleWithPath(final String path) {
        return false;
    }

    public RpcCallSite getBackend() {
        return null;
    }

    public boolean isOTP() {
        return false;
    }

    @Override
    public IResource getResource() {
        return null;
    }

    public boolean hasIncludes() {
        return true;
    }

}
