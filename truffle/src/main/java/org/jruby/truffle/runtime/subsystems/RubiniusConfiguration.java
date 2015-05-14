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

import java.util.*;

public class RubiniusConfiguration {

    public static final int SIZE_OF_SHORT = 2;
    public static final int SIZE_OF_INT = 4;
    public static final int SIZE_OF_LONG = 8;
    public static final int SIZE_OF_POINTER = 8;

    public static final int SIZE_OF_ADDRINFO = 4 * SIZE_OF_INT + 4 * SIZE_OF_POINTER;

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
        config("rbx.platform.timeval.tv_sec.offset", 0);
        config("rbx.platform.timeval.tv_sec.size", SIZE_OF_LONG);
        config("rbx.platform.timeval.tv_sec.type", "time_t");
        config("rbx.platform.timeval.tv_usec.offset", SIZE_OF_LONG);
        config("rbx.platform.timeval.tv_usec.size", SIZE_OF_LONG);
        config("rbx.platform.timeval.tv_usec.type", "time_t");

        /*
         * struct addrinfo {
         *     int              ai_flags;
         *     int              ai_family;
         *     int              ai_socktype;
         *     int              ai_protocol;
         *     socklen_t        ai_addrlen;
         *     struct sockaddr *ai_addr;
         *     char            *ai_canonname;
         *     struct addrinfo *ai_next;
         * };
         */

        config("rbx.platform.addrinfo.sizeof", 4 * SIZE_OF_INT + 4 * SIZE_OF_LONG);

        int addrInfoOffset = 0;

        for (String field : Arrays.asList("ai_flags", "ai_family", "ai_socktype", "ai_protocol")) {
            config("rbx.platform.addrinfo." + field + ".offset", addrInfoOffset);
            config("rbx.platform.addrinfo." + field + ".size", SIZE_OF_INT);
            config("rbx.platform.addrinfo." + field + ".type", "int");
            addrInfoOffset += SIZE_OF_INT;
        }

        config("rbx.platform.addrinfo.ai_addrlen.offset", addrInfoOffset);
        config("rbx.platform.addrinfo.ai_addrlen.size", SIZE_OF_LONG);
        config("rbx.platform.addrinfo.ai_addrlen.type", "long");
        addrInfoOffset += SIZE_OF_LONG;

        for (String field : Arrays.asList("ai_addr", "ai_canonname", "ai_next")) {
            config("rbx.platform.addrinfo." + field + ".offset", addrInfoOffset);
            config("rbx.platform.addrinfo." + field + ".size", SIZE_OF_POINTER);
            config("rbx.platform.addrinfo." + field + ".type", "pointer");
            addrInfoOffset += SIZE_OF_LONG;
        }

        /*
         * struct linger {
         *     int              l_onoff;
         *     int              l_linger;
         * };
         */

        config("rbx.platform.linger.sizeof", 2 * SIZE_OF_INT);

        int lingerOffset = 0;

        for (String field : Arrays.asList("l_onoff", "l_linger")) {
            config("rbx.platform.linger." + field + ".offset", lingerOffset);
            config("rbx.platform.linger." + field + ".size", SIZE_OF_INT);
            config("rbx.platform.linger." + field + ".type", "int");
            lingerOffset += SIZE_OF_INT;
        }

        /*
         * struct sockaddr_in {
         *     short            sin_family;
         *     unsigned short   sin_port;
         *     struct in_addr   sin_addr;
         *     char             sin_zero[8];
         * };
         *
         * struct in_addr {
         *     unsigned long s_addr;
         * };
         */

        config("rbx.platform.sockaddr_in.sizeof", 2 * SIZE_OF_SHORT + SIZE_OF_LONG + 8);

        config("rbx.platform.sockaddr_in.sin_family.offset", 0);
        config("rbx.platform.sockaddr_in.sin_family.size", SIZE_OF_SHORT);
        config("rbx.platform.sockaddr_in.sin_family.type", "short");

        config("rbx.platform.sockaddr_in.sin_port.offset", SIZE_OF_SHORT);
        config("rbx.platform.sockaddr_in.sin_port.size", SIZE_OF_SHORT);
        config("rbx.platform.sockaddr_in.sin_port.type", "ushort");

        config("rbx.platform.sockaddr_in.sin_addr.offset", 2 * SIZE_OF_SHORT);
        config("rbx.platform.sockaddr_in.sin_addr.size", SIZE_OF_LONG);
        config("rbx.platform.sockaddr_in.sin_addr.type", "ulong");

        config("rbx.platform.sockaddr_in.sin_zero.offset", 2 * SIZE_OF_SHORT + SIZE_OF_LONG);
        config("rbx.platform.sockaddr_in.sin_zero.size", 8);
        config("rbx.platform.sockaddr_in.sin_zero.type", "char");

        /*
         * struct servent {
         *     char  *s_name;
         *     char **s_aliases;
         *     int    s_port;
         *     char  *s_proto;
         * };
         */

        config("rbx.platform.servent.sizeof", 3 * SIZE_OF_POINTER + SIZE_OF_INT);

        config("rbx.platform.servent.s_name.offset", 0);
        config("rbx.platform.servent.s_name.size", SIZE_OF_POINTER);
        config("rbx.platform.servent.s_name.type", "pointer");

        config("rbx.platform.servent.s_aliases.offset", SIZE_OF_POINTER);
        config("rbx.platform.servent.s_aliases.size", SIZE_OF_POINTER);
        config("rbx.platform.servent.s_aliases.type", "pointer");

        config("rbx.platform.servent.s_port.offset", 2 * SIZE_OF_POINTER);
        config("rbx.platform.servent.s_port.size", SIZE_OF_INT);
        config("rbx.platform.servent.s_port.type", "int");

        config("rbx.platform.servent.s_proto.offset", 2 * SIZE_OF_POINTER + SIZE_OF_INT);
        config("rbx.platform.servent.s_proto.size", SIZE_OF_POINTER);
        config("rbx.platform.servent.s_proto.type", "pointer");

        config("rbx.platform.io.SEEK_SET", 0);
        config("rbx.platform.io.SEEK_CUR", 1);
        config("rbx.platform.io.SEEK_END", 2);

        config("rbx.platform.socket.AI_PASSIVE", context.makeString("1"));
        config("rbx.platform.socket.AF_UNSPEC", context.makeString("0"));
        config("rbx.platform.socket.SOCK_STREAM", context.makeString("1"));
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
