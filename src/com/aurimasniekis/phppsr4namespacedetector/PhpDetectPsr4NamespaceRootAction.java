package com.aurimasniekis.phppsr4namespacedetector;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

public class PhpDetectPsr4NamespaceRootAction extends AnAction {
    public PhpDetectPsr4NamespaceRootAction() {
        super("Detect PSR-4");
    }

    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if(project != null) {
            String messageId = "actions.detect.namespace.roots.no.new.roots.detected";

            if (PhpPsr4NamespaceRootDetector.detectNamespaceRoots(project)) {
                messageId = "actions.detect.namespace.roots.finished";
            }

            PhpPsr4NamespaceNotifier.showNotificationByMessageId(project, messageId);
        }
    }
}
