package com.github.libgraviton.messaging;

import java.util.Properties;

public class PrefixedProperties extends Properties {

    private String prefix;

    public PrefixedProperties(Properties properties, String prefix) {
        super(properties);
        this.prefix = prefix + '.';
    }

    public String getProperty(String name) {
        return super.getProperty(prefix + name);
    }

    public String getProperty(String name, String defaultValue) {
        return super.getProperty(prefix + name, defaultValue);
    }

}
