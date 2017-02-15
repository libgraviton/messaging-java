package com.github.libgraviton.messaging.config;

import java.util.Properties;

/**
 * Created by tgdpaad2 on 15/02/17.
 */
public class PropertyUtil {

    public static boolean getBoolean(Properties properties, String propertyName, boolean defaultValue) {
        return Boolean.valueOf(properties.getProperty(propertyName, Boolean.toString(defaultValue)));
    }

    public static int getIntger(Properties properties, String propertyName, int defaultValue) {
        return Integer.valueOf(properties.getProperty(propertyName, Integer.toString(defaultValue)));
    }

    public static double getDouble(Properties properties, String propertyName, double defaultValue) {
        return Double.valueOf(properties.getProperty(propertyName, Double.toString(defaultValue)));
    }

}
