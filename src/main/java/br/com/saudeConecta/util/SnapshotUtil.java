package br.com.saudeConecta.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SnapshotUtil {

    private static final Logger log = LoggerFactory.getLogger(SnapshotUtil.class);

    private SnapshotUtil() {}

    /**
     * Cria uma cópia rasa (shallow copy) do objeto para uso em comparação de histórico.
     * Resolve proxies Hibernate usando a classe real da entidade.
     *
     * @param original objeto a ser copiado
     * @return novo objeto com os mesmos valores de campo
     * @throws RuntimeException se a cópia falhar
     */
    public static <T> T copiarSnapshot(T original) {
        if (original == null) {
            throw new IllegalArgumentException("Objeto original não pode ser nulo para snapshot");
        }

        // Resolve classe real (ignora proxies Hibernate que contêm '$' no nome)
        Class<?> clazz = original.getClass();
        while (clazz.getName().contains("$") && clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
            clazz = clazz.getSuperclass();
        }

        try {
            @SuppressWarnings("unchecked")
            T clone = (T) clazz.getDeclaredConstructor().newInstance();

            for (Field field : getAllFields(clazz)) {
                // Ignora campos estáticos (ex: serialVersionUID)
                if (Modifier.isStatic(field.getModifiers())) continue;

                field.setAccessible(true);
                try {
                    field.set(clone, field.get(original));
                } catch (Exception e) {
                    log.warn("Não foi possível copiar campo '{}': {}", field.getName(), e.getMessage());
                }
            }

            log.debug("Snapshot criado com sucesso para {}", clazz.getSimpleName());
            return clone;
        } catch (Exception e) {
            log.error("Falha ao criar snapshot de {}: {}", clazz.getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("Falha ao criar snapshot de " + clazz.getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    public static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }
}