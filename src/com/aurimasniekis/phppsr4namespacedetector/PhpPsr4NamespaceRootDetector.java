package com.aurimasniekis.phppsr4namespacedetector;

import com.intellij.json.psi.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.HashSet;
import com.intellij.webcore.resourceRoots.WebIdeProjectStructureWithSourceConfigurable;
import com.jetbrains.php.roots.PhpNamespaceRootInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaSourceRootProperties;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;

import java.io.File;
import java.util.*;

public class PhpPsr4NamespaceRootDetector {
    public static boolean detectNamespaceRoots(Project project) {
        Set<PhpNamespaceRootInfo> roots = new HashSet<PhpNamespaceRootInfo>();
        Runnable runnable = () -> {
            ApplicationManager.getApplication().runReadAction(() -> {
                processNamespaces(project, roots);
            });
        };

        boolean complete = ProgressManager.getInstance().runProcessWithProgressSynchronously(
            runnable,
            MessageBundle.message("actions.detect.namespace.roots.title"),
            true,
            project
        );

        if(!complete) {
            throw new ProcessCanceledException();
        } else if(roots.isEmpty()) {
            return false;
        } else {
            for (Module module : ModuleManager.getInstance(project).getModules()) {
                ContentEntry[] contentEntries = ModuleRootManager.getInstance(module).getContentEntries();
                if(contentEntries.length > 0) {
                    roots.stream().filter((info) -> {
                        return isInContent(contentEntries, info);
                    }).findAny().ifPresent((info) -> {
                        WebIdeProjectStructureWithSourceConfigurable configurable = new WebIdeProjectStructureWithSourceConfigurable(module);
                        ShowSettingsUtil.getInstance().editConfigurable(project, configurable, () -> {
                            addSourceRoots(configurable.getModifiableModel(), roots);
                            configurable.repaint();
                        });
                    });

                    if(roots.isEmpty()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }



    public static void addSourceRoots(@NotNull ModifiableRootModel model, @NotNull Set<PhpNamespaceRootInfo> rootInfos) {
        for(ContentEntry contentEntry : model.getContentEntries()) {
            VirtualFile file = contentEntry.getFile();

            assert file != null;

            List<PhpNamespaceRootInfo> addedRoots = new ArrayList<PhpNamespaceRootInfo>();
            SourceFolder[] sourceFolders = contentEntry.getSourceFolders();

            for (PhpNamespaceRootInfo rootInfo : rootInfos) {
                if (!existsSource(sourceFolders, rootInfo)) {
                    JavaSourceRootType rootType = JavaSourceRootType.SOURCE;

                    if (rootInfo.isTest()) {
                        rootType = JavaSourceRootType.TEST_SOURCE;
                    }

                    JavaSourceRootProperties properties = JpsJavaExtensionService
                            .getInstance()
                            .createSourceRootProperties(rootInfo.getPrefix());

                    contentEntry.addSourceFolder(rootInfo.getSourceUrl(), rootType, properties);
                    addedRoots.add(rootInfo);
                }
            }

            rootInfos.removeAll(addedRoots);
        }
    }

    private static boolean existsSource(SourceFolder[] sourceFolders, PhpNamespaceRootInfo rootInfo) {
        for (SourceFolder sourceFolder : sourceFolders) {
            if (sourceFolder.getPackagePrefix().equals(rootInfo.getPrefix())) {
                return true;
            }
        }

        return false;
    }

    public static void processNamespaces(Project project, Set<PhpNamespaceRootInfo> roots) {
        VirtualFile composerFile = project.getBaseDir().findChild("composer.json");

        JsonFile psiFile = (JsonFile) PsiManager.getInstance(project).findFile(composerFile);

        if (psiFile != null) {
            JsonProperty[] properties = PsiTreeUtil.getChildrenOfType(psiFile.getTopLevelValue(), JsonProperty.class);

            if (properties != null) {
                boolean found = false;
                for (JsonProperty property : properties) {
                    if (!property.getName().equals("autoload") && !property.getName().equals("autoload-dev")) {
                        continue;
                    }

                    if (property.getValue() != null && property.getValue() instanceof JsonObject) {
                        JsonProperty[] namespacesTypes = PsiTreeUtil.getChildrenOfType(
                            property.getValue(),
                            JsonProperty.class
                        );

                        if (namespacesTypes != null) {
                            for (JsonProperty namespaceType : namespacesTypes) {
                                if (!namespaceType.getName().equals("psr-4")) {
                                    continue;
                                }

                                JsonProperty[] namespaces = PsiTreeUtil.getChildrenOfType(
                                        namespaceType.getValue(),
                                        JsonProperty.class
                                );

                                if (namespaces != null) {
                                    for (JsonProperty namespace : namespaces) {
                                        if (namespace.getValue() != null &&
                                            namespace.getValue() instanceof JsonStringLiteral
                                        ) {
                                            String path = project.getBasePath()
                                                    + File.separator
                                                    + ((JsonStringLiteral) namespace.getValue()).getValue();

                                            if (!FileUtil.exists(path)) {
                                                continue;
                                            }

                                            PhpNamespaceRootInfo rootInfo = PhpNamespaceRootInfo.create(
                                                path,
                                                namespace.getName()
                                            );

                                            roots.add(rootInfo);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isInContent(@NotNull ContentEntry[] contentEntries, @NotNull PhpNamespaceRootInfo info) {
        for (ContentEntry entry : contentEntries) {
            VirtualFile file = entry.getFile();

            if (file != null &&
                FileUtil.isAncestor(file.getPath(), info.getSourcePath(), false) &&
                !existsSource(entry.getSourceFolders(), info)
            ) {
                return true;
            }
        }

        return false;
    }
}
