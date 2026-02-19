package com.kcb.masking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kcb.masking.annotation.Mask;
import com.kcb.masking.annotation.MaskStyle;
import com.kcb.masking.config.MaskingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Service that traverses an object graph and produces a masked copy
 * represented as a Map (suitable for logging via JSON serialization).
 *
 * <p>
 * <strong>Key behaviors:</strong>
 * <ul>
 * <li>Never modifies the original object</li>
 * <li>Supports nested objects via recursive traversal</li>
 * <li>Supports List/Set/Array fields</li>
 * <li>Null-safe throughout</li>
 * <li>Respects both config-driven field names and {@code @Mask}
 * annotations</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class ObjectMaskingService {

    private final MaskingService maskingService;
    private final MaskingProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * Produces a masked string representation of the given object.
     * The original object is never modified.
     *
     * @param obj the object to represent in masked form
     * @return JSON string with sensitive fields masked, or the object's toString()
     *         on error
     */
    public String toMaskedString(Object obj) {
        if (!properties.isEnabled() || obj == null) {
            return String.valueOf(obj);
        }
        try {
            Map<String, Object> maskedMap = buildMaskedMap(obj, new HashSet<>());
            return objectMapper.writeValueAsString(maskedMap);
        } catch (Exception e) {
            log.debug("Could not produce masked representation for {}: {}",
                    obj.getClass().getSimpleName(), e.getMessage());
            return obj.toString();
        }
    }

    /**
     * Recursively builds a masked representation of the given object as a Map.
     * Cycle detection is handled via an identity set of visited objects.
     *
     * @param obj     the object to process
     * @param visited set of already-visited objects (prevents infinite recursion)
     * @return a Map representation where sensitive values are masked
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> buildMaskedMap(Object obj, Set<Object> visited) {
        if (obj == null)
            return null;

        // Cycle guard using identity comparison
        if (visited.contains(System.identityHashCode(obj))) {
            return Map.of("$ref", obj.getClass().getSimpleName() + "@cyclic");
        }
        visited.add(System.identityHashCode(obj));

        Map<String, Object> result = new LinkedHashMap<>();
        Class<?> clazz = obj.getClass();

        // Traverse the entire class hierarchy
        List<Field> allFields = getAllFields(clazz);

        for (Field field : allFields) {
            field.setAccessible(true);
            try {
                String fieldName = field.getName();
                Object fieldValue = field.get(obj);

                // Skip synthetic / static fields
                if (field.isSynthetic() || java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                // Determine masking: annotation-driven takes precedence over config-driven
                Mask maskAnnotation = field.getAnnotation(Mask.class);
                boolean shouldMask = maskAnnotation != null || maskingService.isSensitiveField(fieldName);

                if (fieldValue == null) {
                    result.put(fieldName, null);
                } else if (shouldMask && fieldValue instanceof String strVal) {
                    // Apply masking
                    if (maskAnnotation != null) {
                        result.put(fieldName, maskingService.mask(
                                strVal,
                                maskAnnotation.style(),
                                maskAnnotation.maskCharacter()));
                    } else {
                        result.put(fieldName, maskingService.mask(strVal));
                    }
                } else if (fieldValue instanceof String) {
                    result.put(fieldName, fieldValue);
                } else if (fieldValue instanceof Number || fieldValue instanceof Boolean
                        || fieldValue instanceof Character || fieldValue.getClass().isEnum()) {
                    result.put(fieldName, fieldValue);
                } else if (fieldValue instanceof List<?> list) {
                    result.put(fieldName, processList(list, shouldMask, maskAnnotation, visited));
                } else if (fieldValue instanceof Set<?> set) {
                    result.put(fieldName, processList(new ArrayList<>(set), shouldMask, maskAnnotation, visited));
                } else if (fieldValue.getClass().isArray()) {
                    result.put(fieldName, processArray(fieldValue, shouldMask, maskAnnotation, visited));
                } else if (isComplexObject(fieldValue)) {
                    // Recurse for nested objects
                    result.put(fieldName, buildMaskedMap(fieldValue, visited));
                } else {
                    result.put(fieldName, fieldValue);
                }
            } catch (IllegalAccessException e) {
                log.debug("Could not access field {}: {}", field.getName(), e.getMessage());
            }
        }

        return result;
    }

    /**
     * Processes a list of values, applying masking where needed.
     */
    private List<Object> processList(List<?> list, boolean shouldMask,
            Mask maskAnnotation, Set<Object> visited) {
        List<Object> maskedList = new ArrayList<>();
        for (Object item : list) {
            if (item == null) {
                maskedList.add(null);
            } else if (shouldMask && item instanceof String strItem) {
                if (maskAnnotation != null) {
                    maskedList
                            .add(maskingService.mask(strItem, maskAnnotation.style(), maskAnnotation.maskCharacter()));
                } else {
                    maskedList.add(maskingService.mask(strItem));
                }
            } else if (item instanceof String) {
                maskedList.add(item);
            } else if (isComplexObject(item)) {
                maskedList.add(buildMaskedMap(item, visited));
            } else {
                maskedList.add(item);
            }
        }
        return maskedList;
    }

    /**
     * Processes an array, converting it to a list first.
     */
    private Object processArray(Object arrayObj, boolean shouldMask,
            Mask maskAnnotation, Set<Object> visited) {
        if (arrayObj instanceof Object[] objArray) {
            return processList(Arrays.asList(objArray), shouldMask, maskAnnotation, visited);
        }
        // Primitive arrays – return as-is (they can't contain sensitive strings)
        return arrayObj;
    }

    /**
     * Collects all declared fields from the class and all its superclasses.
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    /**
     * Determines if a value is a "complex" domain object that warrants recursive
     * inspection.
     * Returns false for JDK built-in types, collections already handled, etc.
     */
    private boolean isComplexObject(Object obj) {
        if (obj == null)
            return false;
        String packageName = obj.getClass().getPackageName();
        // Only recurse into application/library objects, not JDK internals
        return !packageName.startsWith("java.")
                && !packageName.startsWith("javax.")
                && !packageName.startsWith("sun.")
                && !packageName.startsWith("com.sun.")
                && !(obj instanceof Map)
                && !obj.getClass().isEnum();
    }
}
