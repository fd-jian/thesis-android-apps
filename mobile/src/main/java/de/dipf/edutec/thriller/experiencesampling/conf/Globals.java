package de.dipf.edutec.thriller.experiencesampling.conf;

import android.hardware.Sensor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Globals {
    public static boolean useOffline = false;
    public static boolean loginScreenActive = false;

    /**
     * Holds all simple sensor names mapped by the {@link Sensor}'s "TYPE_*" property. The simple name is used for the
     * mqtt topics.
     */
    public static final Map<Integer, String> sensorTopicNames = Arrays.stream(Sensor.class.getDeclaredFields())
            .filter(Globals::isPublicStaticAndNotAnnotatedUnsupported)
            .filter(field -> field.getName().matches("^TYPE_.*"))
            .collect(Collectors.toMap(
                    Globals::getFieldIntValue,
                    field -> Arrays.stream(Sensor.class.getDeclaredFields())
                            .filter(Globals::isPublicStaticAndNotAnnotatedUnsupported)
                            .filter(f -> f.getName().matches(String.format("^STRING_%s", field.getName())))
                            .map(f1 -> getSensorSimpleName(getSensorSimpleName(f1))).toArray(String[]::new)
            )).entrySet().stream()
            .filter(integerEntry -> integerEntry.getValue().length != 0)
            .collect(Collectors.toMap(Map.Entry::getKey, integerEntry -> integerEntry.getValue()[0]));

    private static String getSensorSimpleName(Field f) {
        try {
            String sensorTypeString = (String) f.get(null);
            if (sensorTypeString == null) {
                throw new RuntimeException(String.format("Field value of %s must not be null. This indicates a " +
                        "fundamental inconsistency.", f.getName()));
            }
            return getSensorSimpleName(sensorTypeString);
        } catch (IllegalAccessException e) {
            throw new RuntimeException();
        }
    }

    private static String getSensorSimpleName(String sensorType) {
        return Optional.of(sensorType.split("\\."))
                .map(strings -> strings[strings.length - 1])
                .orElse(null);
    }

    private static int getFieldIntValue(Field field) {
        try {
            return field.getInt(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException();
        }
    }

    private static boolean isPublicStaticAndNotAnnotatedUnsupported(java.lang.reflect.Field field) {
        return Modifier.isStatic(field.getModifiers()) &&
                Modifier.isPublic(field.getModifiers());
    }

}
