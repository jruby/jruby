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

import jnr.constants.platform.Fcntl;
import jnr.constants.platform.OpenFlags;
import jnr.posix.FileStat;
import org.jruby.truffle.runtime.RubyContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RubiniusConfiguration {

    private static final int SIZE_OF_LONG = 8;

    private final RubyContext context;

    private final Map<String, Object> configuration = new HashMap<>();
    
    public RubiniusConfiguration(RubyContext context) {
        this.context = context;

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

        for (Fcntl fcntl : Fcntl.values()) {
            if (fcntl.name().startsWith("F_")) {
                config("rbx.platform.fcntl." + fcntl.name(), fcntl.intValue());
            }
        }

        for (OpenFlags openFlag : OpenFlags.values()) {
            if (openFlag.name().startsWith("O_")) {
                config("rbx.platform.file." + openFlag.name(), openFlag.intValue());
            }
        }

        config("rbx.platform.fcntl.O_ACCMODE", OpenFlags.O_RDONLY.intValue()
                | OpenFlags.O_WRONLY.intValue() | OpenFlags.O_RDWR.intValue());

        config("rbx.platform.typedef.time_t", "long");

        config("rbx.platform.timeval.sizeof", 2 * SIZE_OF_LONG);
        config("rbx.platform.timeval.tv_sec.offset", 0 * SIZE_OF_LONG);
        config("rbx.platform.timeval.tv_sec.size", SIZE_OF_LONG);
        config("rbx.platform.timeval.tv_sec.type", "time_t");
        config("rbx.platform.timeval.tv_usec.offset", 1 * SIZE_OF_LONG);
        config("rbx.platform.timeval.tv_usec.size", SIZE_OF_LONG);
        config("rbx.platform.timeval.tv_usec.type", "time_t");

        config("rbx.platform.io.SEEK_SET", 0);
        config("rbx.platform.io.SEEK_CUR", 1);
        config("rbx.platform.io.SEEK_END", 2);
    }

    private void config(String key, String value) {
        config(key, context.getSymbolTable().getSymbol(value));
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
