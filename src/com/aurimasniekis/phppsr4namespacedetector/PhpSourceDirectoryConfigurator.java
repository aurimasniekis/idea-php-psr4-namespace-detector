package com.aurimasniekis.phppsr4namespacedetector;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.DirectoryProjectConfigurator;
import com.jetbrains.php.roots.PhpNamespaceRootInfo;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.util.Ref;

import javax.swing.event.HyperlinkEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by aniekis on 4/20/17.
 */
public class PhpSourceDirectoryConfigurator implements DirectoryProjectConfigurator {
    public void configureProject(Project project, @NotNull VirtualFile baseDir, Ref<Module> moduleRef) {
        StartupManager.getInstance(project).runWhenProjectIsInitialized(() -> {
            DumbService.getInstance(project).smartInvokeLater(() -> {
                Set<PhpNamespaceRootInfo> namespaces = new HashSet<PhpNamespaceRootInfo>();
                PhpPsr4NamespaceRootDetector.processNamespaces(project, namespaces);

                if(namespaces.size() > 0) {
                    NotificationListener listener = new NotificationListener() {
                        public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                            if("detect".equals(event.getDescription())) {
                                DumbService.getInstance(project).smartInvokeLater(() -> {
                                    boolean detected = PhpPsr4NamespaceRootDetector.detectNamespaceRoots(project);
                                    if(!detected) {
                                        PhpPsr4NamespaceNotifier.showNotificationByMessageId(
                                            project,
                                            "actions.detect.namespace.roots.no.roots.detected",
                                            (NotificationListener)null
                                        );
                                    }

                                    notification.expire();
                                });
                            } else {
                                ShowSettingsUtil.getInstance().showSettingsDialog(project, "Directories");
                            }

                        }
                    };
                    PhpPsr4NamespaceNotifier.showNotificationByMessageId(
                        project,
                        "actions.detect.namespace.roots.notification.message",
                        listener
                    );
                }

            });
        });
    }
}
