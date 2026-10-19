package io.mikip98.clothconfigextensions.client.registries;

import io.mikip98.clothconfigextensions.api.annotaions.ConfigExtension;

import java.lang.reflect.Field;

public abstract class Util {
    /**
     * Checks if any of the parent classes have the {@link ConfigExtension.Gui.GlobalTooltip} annotation
     * @return {@link ConfigExtension.Gui.GlobalTooltip} annotation instance if found, else null
     */
    public static ConfigExtension.Gui.GlobalTooltip getGlobalTooltip(Field field) {
        Class<?> currentClass = field.getDeclaringClass();
        while (currentClass != null) {
            if (currentClass.isAnnotationPresent(ConfigExtension.Gui.GlobalTooltip.class)) {
                return currentClass.getAnnotation(ConfigExtension.Gui.GlobalTooltip.class);
            }
            currentClass = currentClass.getEnclosingClass();
        }
        return null;
    }
    // TOOD: Simplify this to how this looks in AutoCommentedTomlSerializer
}
