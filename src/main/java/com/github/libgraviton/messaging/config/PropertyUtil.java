package com.github.libgraviton.messaging.config;

import java.util.Properties;

/**
 * Some useful methods to get properties.
 */
public class PropertyUtil {

    /**
     * Gets a property as a boolean.
     *
     * @param properties The property set
     * @param propertyName The property's name
     * @param defaultValue A default value to be returned in case of the desired property not being set.
     *
     * @return The property's value or the default value
     */
    public static boolean getBoolean(Properties properties, String propertyName, boolean defaultValue) {
        return Boolean.valueOf(properties.getProperty(propertyName, Boolean.toString(defaultValue)));
    }

    /**
     * Gets a property as an int.
     *
     * @param properties The property set
     * @param propertyName The property's name
     * @param defaultValue A default value to be returned in case of the desired property not being set.
     *
     * @return The property's value or the default value
     */
    public static int getIntger(Properties properties, String propertyName, int defaultValue) {
        return Integer.valueOf(properties.getProperty(propertyName, Integer.toString(defaultValue)));
    }

    /**
     * Gets a property as a double.
     *
     * @param properties The property set
     * @param propertyName The property's name
     * @param defaultValue A default value to be returned in case of the desired property not being set.
     *
     * @return The property's value or the default value
     */
    public static double getDouble(Properties properties, String propertyName, double defaultValue) {
        return Double.valueOf(properties.getProperty(propertyName, Double.toString(defaultValue)));
    }

}
