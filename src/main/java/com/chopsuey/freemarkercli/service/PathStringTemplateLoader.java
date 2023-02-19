package com.chopsuey.freemarkercli.service;

import freemarker.cache.StringTemplateLoader;

import java.util.Optional;

public class PathStringTemplateLoader extends StringTemplateLoader {

    @Override
    public Object findTemplateSource(String name) {
        return Optional.ofNullable(super.findTemplateSource(name))
                .orElse(super.findTemplateSource("/" + name));
    }
}
