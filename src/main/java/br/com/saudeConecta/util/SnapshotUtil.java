package br.com.saudeConecta.util;

import java.lang.reflect.Field; // ← deve ser este, não do Twilio
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SnapshotUtil {

    private SnapshotUtil() {}

    public static <T> T copiarSnapshot(T original) {
        try {
            @SuppressWarnings("unchecked")
            T clone = (T) original.getClass()
                    .getDeclaredConstructor()
                    .newInstance();

            for (java.lang.reflect.Field field : getAllFields(original.getClass())) {
                field.setAccessible(true);
                field.set(clone, field.get(original));
            }
            return clone;
        } catch (Exception e) {
            return original;
        }
    }

    public static List<java.lang.reflect.Field> getAllFields(Class<?> clazz) {
        List<java.lang.reflect.Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }
}