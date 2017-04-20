package com.aurimasniekis.phppsr4namespacedetector;

import com.intellij.CommonBundle;
import com.intellij.reference.SoftReference;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.lang.ref.Reference;
import java.util.ResourceBundle;

public class MessageBundle {
    private static Reference<ResourceBundle> ourBundle;
    @NonNls
    public static final String MESSAGE_BUNDLE = "main.resource.Messages";

    public static String message(@NotNull @PropertyKey(resourceBundle = "main.resource.Messages") String key, @NotNull Object... params) {
        return CommonBundle.message(getBundle(), key, params);
    }

    private static ResourceBundle getBundle() {
        ResourceBundle bundle = (ResourceBundle) SoftReference.dereference(ourBundle);
        if(bundle == null) {
            bundle = ResourceBundle.getBundle("main.resource.Messages");
            ourBundle = new java.lang.ref.SoftReference(bundle);
        }

        return bundle;
    }
}
