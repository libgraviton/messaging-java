package com.github.libgraviton.messaging.config;

import java.util.Properties;


/**
 * This class allows you to access a set of properties in a given config. For example if you define config "config",
 * the call of getProperty("some.property") will return the value of config.some.property.
 */
public class ContextProperties extends Properties {

    private String context;

    /**
     * Constructor.
     *
     * @param properties The properties instance holding all properties.
     * @param context The config to be used.
     */
    public ContextProperties(Properties properties, String context) {
        super(properties);
        this.context = context;
    }

    @Override
    public String getProperty(String name) {
        return super.getProperty(context + name);
    }

}
