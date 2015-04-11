/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.subsystems;

import jnr.posix.FileStat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RubiniusConfiguration {

    private final Map<String, Object> configuration = new HashMap<>();
    
    public RubiniusConfiguration() {
        config("hash.hamt", false);

        config("rbx.platform.file.S_IRUSR", FileStat.S_IRUSR);
        config("rbx.platform.file.S_IWUSR", FileStat.S_IWUSR);
        config("rbx.platform.file.S_IXUSR", FileStat.S_IXUSR);
        config("rbx.platform.file.S_IRGRP", FileStat.S_IRGRP);
        config("rbx.platform.file.S_IWGRP", FileStat.S_IWGRP);
        config("rbx.platform.file.S_IXGRP", FileStat.S_IXGRP);
        config("rbx.platform.file.S_IROTH", FileStat.S_IROTH);
        config("rbx.platform.file.S_IWOTH", FileStat.S_IWOTH);
        config("rbx.platform.file.S_IXOTH", FileStat.S_IXOTH);
        config("rbx.platform.file.S_IFMT", FileStat.S_IFMT);
        config("rbx.platform.file.S_IFIFO", FileStat.S_IFIFO);
        config("rbx.platform.file.S_IFCHR", FileStat.S_IFCHR);
        config("rbx.platform.file.S_IFDIR", FileStat.S_IFDIR);
        config("rbx.platform.file.S_IFBLK", FileStat.S_IFBLK);
        config("rbx.platform.file.S_IFREG", FileStat.S_IFREG);
        config("rbx.platform.file.S_IFLNK", FileStat.S_IFLNK);
        config("rbx.platform.file.S_IFSOCK", FileStat.S_IFSOCK);
        config("rbx.platform.file.S_ISUID", FileStat.S_ISUID);
        config("rbx.platform.file.S_ISGID", FileStat.S_ISGID);
        config("rbx.platform.file.S_ISVTX", FileStat.S_ISVTX);
    }
    
    private void config(String key, Object value) {
        configuration.put(key, value);
    }

    public Object get(String key) {
        return configuration.get(key);
    }

    public Collection<String> getSection(String section) {
        final Collection<String> sectionKeys = new ArrayList<>();

        for (String key : configuration.keySet()) {
            if (key.startsWith(section)) {
                sectionKeys.add(key);
            }
        }

        return sectionKeys;
    }

}
