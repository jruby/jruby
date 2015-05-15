/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * This file contains configuration values translated from Rubinius.
 *
 * Copyright (c) 2007-2014, Evan Phoenix and contributors
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of Rubinius nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

    public static final int TYPE_CHAR = 0;
    public static final int TYPE_UCHAR = 1;
    public static final int TYPE_BOOL = 2;
    public static final int TYPE_SHORT = 3;
    public static final int TYPE_USHORT = 4;
    public static final int TYPE_INT = 5;
    public static final int TYPE_UINT = 6;
    public static final int TYPE_LONG = 7;
    public static final int TYPE_ULONG = 8;
    public static final int TYPE_LL = 9;
    public static final int TYPE_ULL = 10;
    public static final int TYPE_FLOAT = 11;
    public static final int TYPE_DOUBLE = 12;
    public static final int TYPE_PTR = 13;
    public static final int TYPE_VOID = 14;
    public static final int TYPE_STRING = 15;
    public static final int TYPE_STRPTR = 16;
    public static final int TYPE_CHARARR = 17;
    public static final int TYPE_ENUM = 18;
    public static final int TYPE_VARARGS = 19;

    private final RubyContext context;

    private final Map<String, Object> configuration = new HashMap<>();
    
    public RubiniusConfiguration(RubyContext context) {
        this.context = context;

        // Note that this is platform specific at the moment - generated on a Mac

        loadGeneratedConfiguration();

        // Our own constants - may want to remove some of these in time

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

        config("rbx.platform.fcntl.FD_CLOEXEC", 1); // TODO BJF 15-May-2015 Get from JNR constants or stdlib FFI

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
         *     char            *ai_canonname;  manpage on Darwin says these next two are swapped but header says otherwise
         *     struct sockaddr *ai_addr;
         *     struct addrinfo *ai_next;
         * };
         */

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

        for (String field : Arrays.asList("ai_canonname", "ai_addr", "ai_next")) {
            config("rbx.platform.addrinfo." + field + ".offset", addrInfoOffset);
            config("rbx.platform.addrinfo." + field + ".size", SIZE_OF_POINTER);
            config("rbx.platform.addrinfo." + field + ".type", "pointer");
            addrInfoOffset += SIZE_OF_POINTER;
        }

        config("rbx.platform.addrinfo.sizeof", addrInfoOffset);

        /*
         * struct linger {
         *     int              l_onoff;
         *     int              l_linger;
         * };
         */

        int lingerOffset = 0;

        for (String field : Arrays.asList("l_onoff", "l_linger")) {
            config("rbx.platform.linger." + field + ".offset", lingerOffset);
            config("rbx.platform.linger." + field + ".size", SIZE_OF_INT);
            config("rbx.platform.linger." + field + ".type", "int");
            lingerOffset += SIZE_OF_INT;
        }

        config("rbx.platform.linger.sizeof", lingerOffset);

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
        config("rbx.platform.socket.SOCK_STREAM", context.makeString("1"));
    }

    private void loadGeneratedConfiguration() {
        // Generated from tool/truffle/translate_rubinius_config.rb < ../rubinius/runtime/platform.conf
        config("rbx.platform.addrinfo.sizeof", 48);
        config("rbx.platform.addrinfo.ai_flags.offset", 0);
        config("rbx.platform.addrinfo.ai_flags.size", 4);
        config("rbx.platform.addrinfo.ai_flags.type", context.makeString("int"));
        config("rbx.platform.addrinfo.ai_family.offset", 4);
        config("rbx.platform.addrinfo.ai_family.size", 4);
        config("rbx.platform.addrinfo.ai_family.type", context.makeString("int"));
        config("rbx.platform.addrinfo.ai_socktype.offset", 8);
        config("rbx.platform.addrinfo.ai_socktype.size", 4);
        config("rbx.platform.addrinfo.ai_socktype.type", context.makeString("int"));
        config("rbx.platform.addrinfo.ai_protocol.offset", 12);
        config("rbx.platform.addrinfo.ai_protocol.size", 4);
        config("rbx.platform.addrinfo.ai_protocol.type", context.makeString("int"));
        config("rbx.platform.addrinfo.ai_addrlen.offset", 16);
        config("rbx.platform.addrinfo.ai_addrlen.size", 4);
        config("rbx.platform.addrinfo.ai_addrlen.type", context.makeString("int"));
        config("rbx.platform.addrinfo.ai_addr.offset", 32);
        config("rbx.platform.addrinfo.ai_addr.size", 8);
        config("rbx.platform.addrinfo.ai_addr.type", context.makeString("pointer"));
        config("rbx.platform.addrinfo.ai_canonname.offset", 24);
        config("rbx.platform.addrinfo.ai_canonname.size", 8);
        config("rbx.platform.addrinfo.ai_canonname.type", context.makeString("string"));
        config("rbx.platform.addrinfo.ai_next.offset", 40);
        config("rbx.platform.addrinfo.ai_next.size", 8);
        config("rbx.platform.addrinfo.ai_next.type", context.makeString("pointer"));
        config("rbx.platform.ifaddrs.sizeof", 56);
        config("rbx.platform.ifaddrs.ifa_next.offset", 0);
        config("rbx.platform.ifaddrs.ifa_next.size", 8);
        config("rbx.platform.ifaddrs.ifa_next.type", context.makeString("pointer"));
        config("rbx.platform.ifaddrs.ifa_name.offset", 8);
        config("rbx.platform.ifaddrs.ifa_name.size", 8);
        config("rbx.platform.ifaddrs.ifa_name.type", context.makeString("string"));
        config("rbx.platform.ifaddrs.ifa_flags.offset", 16);
        config("rbx.platform.ifaddrs.ifa_flags.size", 4);
        config("rbx.platform.ifaddrs.ifa_flags.type", context.makeString("int"));
        config("rbx.platform.ifaddrs.ifa_addr.offset", 24);
        config("rbx.platform.ifaddrs.ifa_addr.size", 8);
        config("rbx.platform.ifaddrs.ifa_addr.type", context.makeString("pointer"));
        config("rbx.platform.ifaddrs.ifa_netmask.offset", 32);
        config("rbx.platform.ifaddrs.ifa_netmask.size", 8);
        config("rbx.platform.ifaddrs.ifa_netmask.type", context.makeString("pointer"));
        config("rbx.platform.sockaddr.sizeof", 16);
        config("rbx.platform.sockaddr.sa_data.offset", 2);
        config("rbx.platform.sockaddr.sa_data.size", 14);
        config("rbx.platform.sockaddr.sa_data.type", context.makeString("char_array"));
        config("rbx.platform.sockaddr.sa_family.offset", 1);
        config("rbx.platform.sockaddr.sa_family.size", 1);
        config("rbx.platform.sockaddr.sa_family.type", context.makeString("sa_family_t"));
        config("rbx.platform.dirent.sizeof", 1048);
        config("rbx.platform.dirent.d_ino.offset", 0);
        config("rbx.platform.dirent.d_ino.size", 8);
        config("rbx.platform.dirent.d_ino.type", context.makeString("ino_t"));
        config("rbx.platform.dirent.d_reclen.offset", 16);
        config("rbx.platform.dirent.d_reclen.size", 2);
        config("rbx.platform.dirent.d_reclen.type", context.makeString("ushort"));
        config("rbx.platform.dirent.d_name.offset", 21);
        config("rbx.platform.dirent.d_name.size", 1024);
        config("rbx.platform.dirent.d_name.type", context.makeString("char_array"));
        config("rbx.platform.timeval.sizeof", 16);
        config("rbx.platform.timeval.tv_sec.offset", 0);
        config("rbx.platform.timeval.tv_sec.size", 8);
        config("rbx.platform.timeval.tv_sec.type", context.makeString("time_t"));
        config("rbx.platform.timeval.tv_usec.offset", 8);
        config("rbx.platform.timeval.tv_usec.size", 4);
        config("rbx.platform.timeval.tv_usec.type", context.makeString("suseconds_t"));
        config("rbx.platform.sockaddr_in.sizeof", 16);
        config("rbx.platform.sockaddr_in.sin_family.offset", 1);
        config("rbx.platform.sockaddr_in.sin_family.size", 1);
        config("rbx.platform.sockaddr_in.sin_family.type", context.makeString("sa_family_t"));
        config("rbx.platform.sockaddr_in.sin_port.offset", 2);
        config("rbx.platform.sockaddr_in.sin_port.size", 2);
        config("rbx.platform.sockaddr_in.sin_port.type", context.makeString("ushort"));
        config("rbx.platform.sockaddr_in.sin_addr.offset", 4);
        config("rbx.platform.sockaddr_in.sin_addr.size", 4);
        config("rbx.platform.sockaddr_in.sin_zero.offset", 8);
        config("rbx.platform.sockaddr_in.sin_zero.size", 8);
        config("rbx.platform.sockaddr_in.sin_zero.type", context.makeString("char_array"));
        config("rbx.platform.sockaddr_in6.sizeof", 28);
        config("rbx.platform.sockaddr_in6.sin6_family.offset", 1);
        config("rbx.platform.sockaddr_in6.sin6_family.size", 1);
        config("rbx.platform.sockaddr_in6.sin6_family.type", context.makeString("sa_family_t"));
        config("rbx.platform.sockaddr_in6.sin6_port.offset", 2);
        config("rbx.platform.sockaddr_in6.sin6_port.size", 2);
        config("rbx.platform.sockaddr_in6.sin6_port.type", context.makeString("ushort"));
        config("rbx.platform.sockaddr_in6.sin6_flowinfo.offset", 4);
        config("rbx.platform.sockaddr_in6.sin6_flowinfo.size", 4);
        config("rbx.platform.sockaddr_in6.sin6_addr.offset", 8);
        config("rbx.platform.sockaddr_in6.sin6_addr.size", 16);
        config("rbx.platform.sockaddr_in6.sin6_addr.type", context.makeString("char_array"));
        config("rbx.platform.sockaddr_in6.sin6_scope_id.offset", 24);
        config("rbx.platform.sockaddr_in6.sin6_scope_id.size", 4);
        config("rbx.platform.sockaddr_un.sizeof", 106);
        config("rbx.platform.sockaddr_un.sun_family.offset", 1);
        config("rbx.platform.sockaddr_un.sun_family.size", 1);
        config("rbx.platform.sockaddr_un.sun_family.type", context.makeString("sa_family_t"));
        config("rbx.platform.sockaddr_un.sun_path.offset", 2);
        config("rbx.platform.sockaddr_un.sun_path.size", 104);
        config("rbx.platform.sockaddr_un.sun_path.type", context.makeString("char_array"));
        config("rbx.platform.linger.sizeof", 8);
        config("rbx.platform.linger.l_onoff.offset", 0);
        config("rbx.platform.linger.l_onoff.size", 4);
        config("rbx.platform.linger.l_linger.offset", 4);
        config("rbx.platform.linger.l_linger.size", 4);
        config("rbx.platform.servent.sizeof", 32);
        config("rbx.platform.servent.s_name.offset", 0);
        config("rbx.platform.servent.s_name.size", 8);
        config("rbx.platform.servent.s_name.type", context.makeString("pointer"));
        config("rbx.platform.servent.s_aliases.offset", 8);
        config("rbx.platform.servent.s_aliases.size", 8);
        config("rbx.platform.servent.s_aliases.type", context.makeString("pointer"));
        config("rbx.platform.servent.s_port.offset", 16);
        config("rbx.platform.servent.s_port.size", 4);
        config("rbx.platform.servent.s_port.type", context.makeString("int"));
        config("rbx.platform.servent.s_proto.offset", 24);
        config("rbx.platform.servent.s_proto.size", 8);
        config("rbx.platform.servent.s_proto.type", context.makeString("pointer"));
        config("rbx.platform.stat.sizeof", 144);
        config("rbx.platform.stat.st_dev.offset", 0);
        config("rbx.platform.stat.st_dev.size", 4);
        config("rbx.platform.stat.st_dev.type", context.makeString("dev_t"));
        config("rbx.platform.stat.st_ino.offset", 8);
        config("rbx.platform.stat.st_ino.size", 8);
        config("rbx.platform.stat.st_ino.type", context.makeString("ino_t"));
        config("rbx.platform.stat.st_mode.offset", 4);
        config("rbx.platform.stat.st_mode.size", 2);
        config("rbx.platform.stat.st_mode.type", context.makeString("mode_t"));
        config("rbx.platform.stat.st_nlink.offset", 6);
        config("rbx.platform.stat.st_nlink.size", 2);
        config("rbx.platform.stat.st_nlink.type", context.makeString("nlink_t"));
        config("rbx.platform.stat.st_uid.offset", 16);
        config("rbx.platform.stat.st_uid.size", 4);
        config("rbx.platform.stat.st_uid.type", context.makeString("uid_t"));
        config("rbx.platform.stat.st_gid.offset", 20);
        config("rbx.platform.stat.st_gid.size", 4);
        config("rbx.platform.stat.st_gid.type", context.makeString("gid_t"));
        config("rbx.platform.stat.st_rdev.offset", 24);
        config("rbx.platform.stat.st_rdev.size", 4);
        config("rbx.platform.stat.st_rdev.type", context.makeString("dev_t"));
        config("rbx.platform.stat.st_size.offset", 96);
        config("rbx.platform.stat.st_size.size", 8);
        config("rbx.platform.stat.st_size.type", context.makeString("off_t"));
        config("rbx.platform.stat.st_blksize.offset", 112);
        config("rbx.platform.stat.st_blksize.size", 4);
        config("rbx.platform.stat.st_blocks.offset", 104);
        config("rbx.platform.stat.st_blocks.size", 8);
        config("rbx.platform.stat.st_atime.offset", 32);
        config("rbx.platform.stat.st_atime.size", 8);
        config("rbx.platform.stat.st_atime.type", context.makeString("time_t"));
        config("rbx.platform.stat.st_mtime.offset", 48);
        config("rbx.platform.stat.st_mtime.size", 8);
        config("rbx.platform.stat.st_mtime.type", context.makeString("time_t"));
        config("rbx.platform.stat.st_ctime.offset", 64);
        config("rbx.platform.stat.st_ctime.size", 8);
        config("rbx.platform.stat.st_ctime.type", context.makeString("time_t"));
        config("rbx.platform.rlimit.sizeof", 16);
        config("rbx.platform.rlimit.rlim_cur.offset", 0);
        config("rbx.platform.rlimit.rlim_cur.size", 8);
        config("rbx.platform.rlimit.rlim_cur.type", context.makeString("rlim_t"));
        config("rbx.platform.rlimit.rlim_max.offset", 8);
        config("rbx.platform.rlimit.rlim_max.size", 8);
        config("rbx.platform.rlimit.rlim_max.type", context.makeString("rlim_t"));
        config("rbx.platform.file.O_RDONLY", context.makeString("0"));
        config("rbx.platform.file.O_WRONLY", context.makeString("1"));
        config("rbx.platform.file.O_RDWR", context.makeString("2"));
        config("rbx.platform.file.O_CREAT", context.makeString("512"));
        config("rbx.platform.file.O_EXCL", context.makeString("2048"));
        config("rbx.platform.file.O_NOCTTY", context.makeString("131072"));
        config("rbx.platform.file.O_TRUNC", context.makeString("1024"));
        config("rbx.platform.file.O_APPEND", context.makeString("8"));
        config("rbx.platform.file.O_NONBLOCK", context.makeString("4"));
        config("rbx.platform.file.O_SYNC", context.makeString("128"));
        config("rbx.platform.file.S_IRUSR", context.makeString("256"));
        config("rbx.platform.file.S_IWUSR", context.makeString("128"));
        config("rbx.platform.file.S_IXUSR", context.makeString("64"));
        config("rbx.platform.file.S_IRGRP", context.makeString("32"));
        config("rbx.platform.file.S_IWGRP", context.makeString("16"));
        config("rbx.platform.file.S_IXGRP", context.makeString("8"));
        config("rbx.platform.file.S_IROTH", context.makeString("4"));
        config("rbx.platform.file.S_IWOTH", context.makeString("2"));
        config("rbx.platform.file.S_IXOTH", context.makeString("1"));
        config("rbx.platform.file.S_IFMT", context.makeString("61440"));
        config("rbx.platform.file.S_IFIFO", context.makeString("4096"));
        config("rbx.platform.file.S_IFCHR", context.makeString("8192"));
        config("rbx.platform.file.S_IFDIR", context.makeString("16384"));
        config("rbx.platform.file.S_IFBLK", context.makeString("24576"));
        config("rbx.platform.file.S_IFREG", context.makeString("32768"));
        config("rbx.platform.file.S_IFLNK", context.makeString("40960"));
        config("rbx.platform.file.S_IFSOCK", context.makeString("49152"));
        config("rbx.platform.file.S_IFWHT", context.makeString("57344"));
        config("rbx.platform.file.S_ISUID", context.makeString("2048"));
        config("rbx.platform.file.S_ISGID", context.makeString("1024"));
        config("rbx.platform.file.S_ISVTX", context.makeString("512"));
        config("rbx.platform.io.SEEK_SET", context.makeString("0"));
        config("rbx.platform.io.SEEK_CUR", context.makeString("1"));
        config("rbx.platform.io.SEEK_END", context.makeString("2"));
        config("rbx.platform.fcntl.F_GETFL", context.makeString("3"));
        config("rbx.platform.fcntl.F_SETFL", context.makeString("4"));
        config("rbx.platform.fcntl.O_ACCMODE", context.makeString("3"));
        config("rbx.platform.fcntl.F_GETFD", context.makeString("1"));
        config("rbx.platform.fcntl.F_SETFD", context.makeString("2"));
        config("rbx.platform.fcntl.FD_CLOEXEC", context.makeString("1"));
        config("rbx.platform.socket.AF_APPLETALK", context.makeString("16"));
        config("rbx.platform.socket.AF_CCITT", context.makeString("10"));
        config("rbx.platform.socket.AF_CHAOS", context.makeString("5"));
        config("rbx.platform.socket.AF_CNT", context.makeString("21"));
        config("rbx.platform.socket.AF_COIP", context.makeString("20"));
        config("rbx.platform.socket.AF_DATAKIT", context.makeString("9"));
        config("rbx.platform.socket.AF_DLI", context.makeString("13"));
        config("rbx.platform.socket.AF_E164", context.makeString("28"));
        config("rbx.platform.socket.AF_ECMA", context.makeString("8"));
        config("rbx.platform.socket.AF_HYLINK", context.makeString("15"));
        config("rbx.platform.socket.AF_IMPLINK", context.makeString("3"));
        config("rbx.platform.socket.AF_INET", context.makeString("2"));
        config("rbx.platform.socket.AF_INET6", context.makeString("30"));
        config("rbx.platform.socket.AF_IPX", context.makeString("23"));
        config("rbx.platform.socket.AF_ISDN", context.makeString("28"));
        config("rbx.platform.socket.AF_ISO", context.makeString("7"));
        config("rbx.platform.socket.AF_LAT", context.makeString("14"));
        config("rbx.platform.socket.AF_LINK", context.makeString("18"));
        config("rbx.platform.socket.AF_LOCAL", context.makeString("1"));
        config("rbx.platform.socket.AF_MAX", context.makeString("40"));
        config("rbx.platform.socket.AF_NATM", context.makeString("31"));
        config("rbx.platform.socket.AF_NDRV", context.makeString("27"));
        config("rbx.platform.socket.AF_NETBIOS", context.makeString("33"));
        config("rbx.platform.socket.AF_NS", context.makeString("6"));
        config("rbx.platform.socket.AF_OSI", context.makeString("7"));
        config("rbx.platform.socket.AF_PPP", context.makeString("34"));
        config("rbx.platform.socket.AF_PUP", context.makeString("4"));
        config("rbx.platform.socket.AF_ROUTE", context.makeString("17"));
        config("rbx.platform.socket.AF_SIP", context.makeString("24"));
        config("rbx.platform.socket.AF_SNA", context.makeString("11"));
        config("rbx.platform.socket.AF_SYSTEM", context.makeString("32"));
        config("rbx.platform.socket.AF_UNIX", context.makeString("1"));
        config("rbx.platform.socket.AF_UNSPEC", context.makeString("0"));
        config("rbx.platform.socket.AI_ADDRCONFIG", context.makeString("1024"));
        config("rbx.platform.socket.AI_ALL", context.makeString("256"));
        config("rbx.platform.socket.AI_CANONNAME", context.makeString("2"));
        config("rbx.platform.socket.AI_DEFAULT", context.makeString("1536"));
        config("rbx.platform.socket.AI_MASK", context.makeString("5127"));
        config("rbx.platform.socket.AI_NUMERICHOST", context.makeString("4"));
        config("rbx.platform.socket.AI_NUMERICSERV", context.makeString("4096"));
        config("rbx.platform.socket.AI_PASSIVE", context.makeString("1"));
        config("rbx.platform.socket.AI_V4MAPPED", context.makeString("2048"));
        config("rbx.platform.socket.AI_V4MAPPED_CFG", context.makeString("512"));
        config("rbx.platform.socket.EAI_ADDRFAMILY", context.makeString("1"));
        config("rbx.platform.socket.EAI_AGAIN", context.makeString("2"));
        config("rbx.platform.socket.EAI_BADFLAGS", context.makeString("3"));
        config("rbx.platform.socket.EAI_BADHINTS", context.makeString("12"));
        config("rbx.platform.socket.EAI_FAIL", context.makeString("4"));
        config("rbx.platform.socket.EAI_FAMILY", context.makeString("5"));
        config("rbx.platform.socket.EAI_MAX", context.makeString("15"));
        config("rbx.platform.socket.EAI_MEMORY", context.makeString("6"));
        config("rbx.platform.socket.EAI_NODATA", context.makeString("7"));
        config("rbx.platform.socket.EAI_NONAME", context.makeString("8"));
        config("rbx.platform.socket.EAI_OVERFLOW", context.makeString("14"));
        config("rbx.platform.socket.EAI_PROTOCOL", context.makeString("13"));
        config("rbx.platform.socket.EAI_SERVICE", context.makeString("9"));
        config("rbx.platform.socket.EAI_SOCKTYPE", context.makeString("10"));
        config("rbx.platform.socket.EAI_SYSTEM", context.makeString("11"));
        config("rbx.platform.socket.INADDR_ALLHOSTS_GROUP", context.makeString("3758096385"));
        config("rbx.platform.socket.INADDR_ANY", context.makeString("0"));
        config("rbx.platform.socket.INADDR_BROADCAST", context.makeString("4294967295"));
        config("rbx.platform.socket.INADDR_LOOPBACK", context.makeString("2130706433"));
        config("rbx.platform.socket.INADDR_MAX_LOCAL_GROUP", context.makeString("3758096639"));
        config("rbx.platform.socket.INADDR_NONE", context.makeString("4294967295"));
        config("rbx.platform.socket.INADDR_UNSPEC_GROUP", context.makeString("3758096384"));
        config("rbx.platform.socket.INET6_ADDRSTRLEN", context.makeString("46"));
        config("rbx.platform.socket.INET_ADDRSTRLEN", context.makeString("16"));
        config("rbx.platform.socket.IP_ADD_MEMBERSHIP", context.makeString("12"));
        config("rbx.platform.socket.IP_ADD_SOURCE_MEMBERSHIP", context.makeString("70"));
        config("rbx.platform.socket.IP_BLOCK_SOURCE", context.makeString("72"));
        config("rbx.platform.socket.IP_DEFAULT_MULTICAST_LOOP", context.makeString("1"));
        config("rbx.platform.socket.IP_DEFAULT_MULTICAST_TTL", context.makeString("1"));
        config("rbx.platform.socket.IP_DROP_MEMBERSHIP", context.makeString("13"));
        config("rbx.platform.socket.IP_DROP_SOURCE_MEMBERSHIP", context.makeString("71"));
        config("rbx.platform.socket.IP_HDRINCL", context.makeString("2"));
        config("rbx.platform.socket.IP_IPSEC_POLICY", context.makeString("21"));
        config("rbx.platform.socket.IP_MAX_MEMBERSHIPS", context.makeString("4095"));
        config("rbx.platform.socket.IP_MSFILTER", context.makeString("74"));
        config("rbx.platform.socket.IP_MULTICAST_IF", context.makeString("9"));
        config("rbx.platform.socket.IP_MULTICAST_LOOP", context.makeString("11"));
        config("rbx.platform.socket.IP_MULTICAST_TTL", context.makeString("10"));
        config("rbx.platform.socket.IP_OPTIONS", context.makeString("1"));
        config("rbx.platform.socket.IP_PKTINFO", context.makeString("26"));
        config("rbx.platform.socket.IP_PORTRANGE", context.makeString("19"));
        config("rbx.platform.socket.IP_RECVDSTADDR", context.makeString("7"));
        config("rbx.platform.socket.IP_RECVIF", context.makeString("20"));
        config("rbx.platform.socket.IP_RECVOPTS", context.makeString("5"));
        config("rbx.platform.socket.IP_RECVRETOPTS", context.makeString("6"));
        config("rbx.platform.socket.IP_RECVTTL", context.makeString("24"));
        config("rbx.platform.socket.IP_RETOPTS", context.makeString("8"));
        config("rbx.platform.socket.IP_TOS", context.makeString("3"));
        config("rbx.platform.socket.IP_TTL", context.makeString("4"));
        config("rbx.platform.socket.IP_UNBLOCK_SOURCE", context.makeString("73"));
        config("rbx.platform.socket.IPPORT_RESERVED", context.makeString("1024"));
        config("rbx.platform.socket.IPPORT_USERRESERVED", context.makeString("5000"));
        config("rbx.platform.socket.IPPROTO_AH", context.makeString("51"));
        config("rbx.platform.socket.IPPROTO_DSTOPTS", context.makeString("60"));
        config("rbx.platform.socket.IPPROTO_EGP", context.makeString("8"));
        config("rbx.platform.socket.IPPROTO_EON", context.makeString("80"));
        config("rbx.platform.socket.IPPROTO_ESP", context.makeString("50"));
        config("rbx.platform.socket.IPPROTO_FRAGMENT", context.makeString("44"));
        config("rbx.platform.socket.IPPROTO_GGP", context.makeString("3"));
        config("rbx.platform.socket.IPPROTO_HELLO", context.makeString("63"));
        config("rbx.platform.socket.IPPROTO_HOPOPTS", context.makeString("0"));
        config("rbx.platform.socket.IPPROTO_ICMP", context.makeString("1"));
        config("rbx.platform.socket.IPPROTO_ICMPV6", context.makeString("58"));
        config("rbx.platform.socket.IPPROTO_IDP", context.makeString("22"));
        config("rbx.platform.socket.IPPROTO_IGMP", context.makeString("2"));
        config("rbx.platform.socket.IPPROTO_IP", context.makeString("0"));
        config("rbx.platform.socket.IPPROTO_IPV6", context.makeString("41"));
        config("rbx.platform.socket.IPPROTO_MAX", context.makeString("256"));
        config("rbx.platform.socket.IPPROTO_ND", context.makeString("77"));
        config("rbx.platform.socket.IPPROTO_NONE", context.makeString("59"));
        config("rbx.platform.socket.IPPROTO_PUP", context.makeString("12"));
        config("rbx.platform.socket.IPPROTO_RAW", context.makeString("255"));
        config("rbx.platform.socket.IPPROTO_ROUTING", context.makeString("43"));
        config("rbx.platform.socket.IPPROTO_TCP", context.makeString("6"));
        config("rbx.platform.socket.IPPROTO_TP", context.makeString("29"));
        config("rbx.platform.socket.IPPROTO_UDP", context.makeString("17"));
        config("rbx.platform.socket.IPPROTO_XTP", context.makeString("36"));
        config("rbx.platform.socket.IPV6_CHECKSUM", context.makeString("26"));
        config("rbx.platform.socket.IPV6_JOIN_GROUP", context.makeString("12"));
        config("rbx.platform.socket.IPV6_LEAVE_GROUP", context.makeString("13"));
        config("rbx.platform.socket.IPV6_MULTICAST_HOPS", context.makeString("10"));
        config("rbx.platform.socket.IPV6_MULTICAST_IF", context.makeString("9"));
        config("rbx.platform.socket.IPV6_MULTICAST_LOOP", context.makeString("11"));
        config("rbx.platform.socket.IPV6_RECVTCLASS", context.makeString("35"));
        config("rbx.platform.socket.IPV6_RTHDR_TYPE_0", context.makeString("0"));
        config("rbx.platform.socket.IPV6_TCLASS", context.makeString("36"));
        config("rbx.platform.socket.IPV6_UNICAST_HOPS", context.makeString("4"));
        config("rbx.platform.socket.IPV6_V6ONLY", context.makeString("27"));
        config("rbx.platform.socket.MCAST_BLOCK_SOURCE", context.makeString("84"));
        config("rbx.platform.socket.MCAST_EXCLUDE", context.makeString("2"));
        config("rbx.platform.socket.MCAST_INCLUDE", context.makeString("1"));
        config("rbx.platform.socket.MCAST_JOIN_GROUP", context.makeString("80"));
        config("rbx.platform.socket.MCAST_JOIN_SOURCE_GROUP", context.makeString("82"));
        config("rbx.platform.socket.MCAST_LEAVE_GROUP", context.makeString("81"));
        config("rbx.platform.socket.MCAST_LEAVE_SOURCE_GROUP", context.makeString("83"));
        config("rbx.platform.socket.MCAST_UNBLOCK_SOURCE", context.makeString("85"));
        config("rbx.platform.socket.MSG_CTRUNC", context.makeString("32"));
        config("rbx.platform.socket.MSG_DONTROUTE", context.makeString("4"));
        config("rbx.platform.socket.MSG_DONTWAIT", context.makeString("128"));
        config("rbx.platform.socket.MSG_EOF", context.makeString("256"));
        config("rbx.platform.socket.MSG_EOR", context.makeString("8"));
        config("rbx.platform.socket.MSG_FLUSH", context.makeString("1024"));
        config("rbx.platform.socket.MSG_HAVEMORE", context.makeString("8192"));
        config("rbx.platform.socket.MSG_HOLD", context.makeString("2048"));
        config("rbx.platform.socket.MSG_OOB", context.makeString("1"));
        config("rbx.platform.socket.MSG_PEEK", context.makeString("2"));
        config("rbx.platform.socket.MSG_RCVMORE", context.makeString("16384"));
        config("rbx.platform.socket.MSG_SEND", context.makeString("4096"));
        config("rbx.platform.socket.MSG_TRUNC", context.makeString("16"));
        config("rbx.platform.socket.MSG_WAITALL", context.makeString("64"));
        config("rbx.platform.socket.NI_DGRAM", context.makeString("16"));
        config("rbx.platform.socket.NI_MAXHOST", context.makeString("1025"));
        config("rbx.platform.socket.NI_MAXSERV", context.makeString("32"));
        config("rbx.platform.socket.NI_NAMEREQD", context.makeString("4"));
        config("rbx.platform.socket.NI_NOFQDN", context.makeString("1"));
        config("rbx.platform.socket.NI_NUMERICHOST", context.makeString("2"));
        config("rbx.platform.socket.NI_NUMERICSERV", context.makeString("8"));
        config("rbx.platform.socket.PF_APPLETALK", context.makeString("16"));
        config("rbx.platform.socket.PF_CCITT", context.makeString("10"));
        config("rbx.platform.socket.PF_CHAOS", context.makeString("5"));
        config("rbx.platform.socket.PF_CNT", context.makeString("21"));
        config("rbx.platform.socket.PF_COIP", context.makeString("20"));
        config("rbx.platform.socket.PF_DATAKIT", context.makeString("9"));
        config("rbx.platform.socket.PF_DLI", context.makeString("13"));
        config("rbx.platform.socket.PF_ECMA", context.makeString("8"));
        config("rbx.platform.socket.PF_HYLINK", context.makeString("15"));
        config("rbx.platform.socket.PF_IMPLINK", context.makeString("3"));
        config("rbx.platform.socket.PF_INET", context.makeString("2"));
        config("rbx.platform.socket.PF_INET6", context.makeString("30"));
        config("rbx.platform.socket.PF_IPX", context.makeString("23"));
        config("rbx.platform.socket.PF_ISDN", context.makeString("28"));
        config("rbx.platform.socket.PF_ISO", context.makeString("7"));
        config("rbx.platform.socket.PF_KEY", context.makeString("29"));
        config("rbx.platform.socket.PF_LAT", context.makeString("14"));
        config("rbx.platform.socket.PF_LINK", context.makeString("18"));
        config("rbx.platform.socket.PF_LOCAL", context.makeString("1"));
        config("rbx.platform.socket.PF_MAX", context.makeString("40"));
        config("rbx.platform.socket.PF_NATM", context.makeString("31"));
        config("rbx.platform.socket.PF_NDRV", context.makeString("27"));
        config("rbx.platform.socket.PF_NETBIOS", context.makeString("33"));
        config("rbx.platform.socket.PF_NS", context.makeString("6"));
        config("rbx.platform.socket.PF_OSI", context.makeString("7"));
        config("rbx.platform.socket.PF_PIP", context.makeString("25"));
        config("rbx.platform.socket.PF_PPP", context.makeString("34"));
        config("rbx.platform.socket.PF_PUP", context.makeString("4"));
        config("rbx.platform.socket.PF_ROUTE", context.makeString("17"));
        config("rbx.platform.socket.PF_RTIP", context.makeString("22"));
        config("rbx.platform.socket.PF_SIP", context.makeString("24"));
        config("rbx.platform.socket.PF_SNA", context.makeString("11"));
        config("rbx.platform.socket.PF_SYSTEM", context.makeString("32"));
        config("rbx.platform.socket.PF_UNIX", context.makeString("1"));
        config("rbx.platform.socket.PF_UNSPEC", context.makeString("0"));
        config("rbx.platform.socket.PF_XTP", context.makeString("19"));
        config("rbx.platform.socket.SCM_CREDS", context.makeString("3"));
        config("rbx.platform.socket.SCM_RIGHTS", context.makeString("1"));
        config("rbx.platform.socket.SCM_TIMESTAMP", context.makeString("2"));
        config("rbx.platform.socket.SHUT_RD", context.makeString("0"));
        config("rbx.platform.socket.SHUT_RDWR", context.makeString("2"));
        config("rbx.platform.socket.SHUT_WR", context.makeString("1"));
        config("rbx.platform.socket.SO_ACCEPTCONN", context.makeString("2"));
        config("rbx.platform.socket.SO_BROADCAST", context.makeString("32"));
        config("rbx.platform.socket.SO_DEBUG", context.makeString("1"));
        config("rbx.platform.socket.SO_DONTROUTE", context.makeString("16"));
        config("rbx.platform.socket.SO_DONTTRUNC", context.makeString("8192"));
        config("rbx.platform.socket.SO_ERROR", context.makeString("4103"));
        config("rbx.platform.socket.SO_KEEPALIVE", context.makeString("8"));
        config("rbx.platform.socket.SO_LINGER", context.makeString("128"));
        config("rbx.platform.socket.SO_NKE", context.makeString("4129"));
        config("rbx.platform.socket.SO_NOSIGPIPE", context.makeString("4130"));
        config("rbx.platform.socket.SO_NREAD", context.makeString("4128"));
        config("rbx.platform.socket.SO_OOBINLINE", context.makeString("256"));
        config("rbx.platform.socket.SO_RCVBUF", context.makeString("4098"));
        config("rbx.platform.socket.SO_RCVLOWAT", context.makeString("4100"));
        config("rbx.platform.socket.SO_RCVTIMEO", context.makeString("4102"));
        config("rbx.platform.socket.SO_REUSEADDR", context.makeString("4"));
        config("rbx.platform.socket.SO_REUSEPORT", context.makeString("512"));
        config("rbx.platform.socket.SO_SNDBUF", context.makeString("4097"));
        config("rbx.platform.socket.SO_SNDLOWAT", context.makeString("4099"));
        config("rbx.platform.socket.SO_SNDTIMEO", context.makeString("4101"));
        config("rbx.platform.socket.SO_TIMESTAMP", context.makeString("1024"));
        config("rbx.platform.socket.SO_TYPE", context.makeString("4104"));
        config("rbx.platform.socket.SO_USELOOPBACK", context.makeString("64"));
        config("rbx.platform.socket.SO_WANTMORE", context.makeString("16384"));
        config("rbx.platform.socket.SO_WANTOOBFLAG", context.makeString("32768"));
        config("rbx.platform.socket.SOCK_DGRAM", context.makeString("2"));
        config("rbx.platform.socket.SOCK_RAW", context.makeString("3"));
        config("rbx.platform.socket.SOCK_RDM", context.makeString("4"));
        config("rbx.platform.socket.SOCK_SEQPACKET", context.makeString("5"));
        config("rbx.platform.socket.SOCK_STREAM", context.makeString("1"));
        config("rbx.platform.socket.SOL_SOCKET", context.makeString("65535"));
        config("rbx.platform.socket.SOMAXCONN", context.makeString("128"));
        config("rbx.platform.socket.TCP_KEEPCNT", context.makeString("258"));
        config("rbx.platform.socket.TCP_KEEPINTVL", context.makeString("257"));
        config("rbx.platform.socket.TCP_MAXSEG", context.makeString("2"));
        config("rbx.platform.socket.TCP_NODELAY", context.makeString("1"));
        config("rbx.platform.socket.TCP_NOOPT", context.makeString("8"));
        config("rbx.platform.socket.TCP_NOPUSH", context.makeString("4"));
        config("rbx.platform.process.EXIT_SUCCESS", context.makeString("0"));
        config("rbx.platform.process.EXIT_FAILURE", context.makeString("1"));
        config("rbx.platform.process.WNOHANG", context.makeString("1"));
        config("rbx.platform.process.WUNTRACED", context.makeString("2"));
        config("rbx.platform.process.PRIO_PROCESS", context.makeString("0"));
        config("rbx.platform.process.PRIO_PGRP", context.makeString("1"));
        config("rbx.platform.process.PRIO_USER", context.makeString("2"));
        config("rbx.platform.process.RLIMIT_CPU", context.makeString("0"));
        config("rbx.platform.process.RLIMIT_FSIZE", context.makeString("1"));
        config("rbx.platform.process.RLIMIT_DATA", context.makeString("2"));
        config("rbx.platform.process.RLIMIT_STACK", context.makeString("3"));
        config("rbx.platform.process.RLIMIT_CORE", context.makeString("4"));
        config("rbx.platform.process.RLIMIT_RSS", context.makeString("5"));
        config("rbx.platform.process.RLIMIT_NPROC", context.makeString("7"));
        config("rbx.platform.process.RLIMIT_NOFILE", context.makeString("8"));
        config("rbx.platform.process.RLIMIT_MEMLOCK", context.makeString("6"));
        config("rbx.platform.process.RLIMIT_AS", context.makeString("5"));
        config("rbx.platform.process.RLIM_INFINITY", context.makeString("9223372036854775807"));
        config("rbx.platform.process.RLIM_SAVED_MAX", context.makeString("9223372036854775807"));
        config("rbx.platform.process.RLIM_SAVED_CUR", context.makeString("9223372036854775807"));
        config("rbx.platform.signal.SIGHUP", context.makeString("1"));
        config("rbx.platform.signal.SIGINT", context.makeString("2"));
        config("rbx.platform.signal.SIGQUIT", context.makeString("3"));
        config("rbx.platform.signal.SIGILL", context.makeString("4"));
        config("rbx.platform.signal.SIGTRAP", context.makeString("5"));
        config("rbx.platform.signal.SIGIOT", context.makeString("6"));
        config("rbx.platform.signal.SIGABRT", context.makeString("6"));
        config("rbx.platform.signal.SIGEMT", context.makeString("7"));
        config("rbx.platform.signal.SIGFPE", context.makeString("8"));
        config("rbx.platform.signal.SIGKILL", context.makeString("9"));
        config("rbx.platform.signal.SIGBUS", context.makeString("10"));
        config("rbx.platform.signal.SIGSEGV", context.makeString("11"));
        config("rbx.platform.signal.SIGSYS", context.makeString("12"));
        config("rbx.platform.signal.SIGPIPE", context.makeString("13"));
        config("rbx.platform.signal.SIGALRM", context.makeString("14"));
        config("rbx.platform.signal.SIGTERM", context.makeString("15"));
        config("rbx.platform.signal.SIGURG", context.makeString("16"));
        config("rbx.platform.signal.SIGSTOP", context.makeString("17"));
        config("rbx.platform.signal.SIGTSTP", context.makeString("18"));
        config("rbx.platform.signal.SIGCONT", context.makeString("19"));
        config("rbx.platform.signal.SIGCHLD", context.makeString("20"));
        config("rbx.platform.signal.SIGTTIN", context.makeString("21"));
        config("rbx.platform.signal.SIGTTOU", context.makeString("22"));
        config("rbx.platform.signal.SIGIO", context.makeString("23"));
        config("rbx.platform.signal.SIGXCPU", context.makeString("24"));
        config("rbx.platform.signal.SIGXFSZ", context.makeString("25"));
        config("rbx.platform.signal.SIGVTALRM", context.makeString("26"));
        config("rbx.platform.signal.SIGPROF", context.makeString("27"));
        config("rbx.platform.signal.SIGWINCH", context.makeString("28"));
        config("rbx.platform.signal.SIGUSR1", context.makeString("30"));
        config("rbx.platform.signal.SIGUSR2", context.makeString("31"));
        config("rbx.platform.signal.SIGINFO", context.makeString("29"));
        config("rbx.platform.zlib.ZLIB_VERSION", context.makeString("1.2.5"));
        config("rbx.platform.dlopen.RTLD_LAZY", context.makeString("1"));
        config("rbx.platform.dlopen.RTLD_NOW", context.makeString("2"));
        config("rbx.platform.dlopen.RTLD_LOCAL", context.makeString("4"));
        config("rbx.platform.dlopen.RTLD_GLOBAL", context.makeString("8"));
        config("rbx.platform.typedef.int8_t", context.makeString("char"));
        config("rbx.platform.typedef.int16_t", context.makeString("short"));
        config("rbx.platform.typedef.int32_t", context.makeString("int"));
        config("rbx.platform.typedef.int64_t", context.makeString("long_long"));
        config("rbx.platform.typedef.uint8_t", context.makeString("uchar"));
        config("rbx.platform.typedef.uint16_t", context.makeString("ushort"));
        config("rbx.platform.typedef.uint32_t", context.makeString("uint"));
        config("rbx.platform.typedef.uint64_t", context.makeString("ulong_long"));
        config("rbx.platform.typedef.int_least8_t", context.makeString("char"));
        config("rbx.platform.typedef.int_least16_t", context.makeString("short"));
        config("rbx.platform.typedef.int_least32_t", context.makeString("int"));
        config("rbx.platform.typedef.int_least64_t", context.makeString("long_long"));
        config("rbx.platform.typedef.uint_least8_t", context.makeString("uchar"));
        config("rbx.platform.typedef.uint_least16_t", context.makeString("ushort"));
        config("rbx.platform.typedef.uint_least32_t", context.makeString("uint"));
        config("rbx.platform.typedef.uint_least64_t", context.makeString("ulong_long"));
        config("rbx.platform.typedef.int_fast8_t", context.makeString("char"));
        config("rbx.platform.typedef.int_fast16_t", context.makeString("short"));
        config("rbx.platform.typedef.int_fast32_t", context.makeString("int"));
        config("rbx.platform.typedef.int_fast64_t", context.makeString("long_long"));
        config("rbx.platform.typedef.uint_fast8_t", context.makeString("uchar"));
        config("rbx.platform.typedef.uint_fast16_t", context.makeString("ushort"));
        config("rbx.platform.typedef.uint_fast32_t", context.makeString("uint"));
        config("rbx.platform.typedef.uint_fast64_t", context.makeString("ulong_long"));
        config("rbx.platform.typedef.__int8_t", context.makeString("char"));
        config("rbx.platform.typedef.__uint8_t", context.makeString("uchar"));
        config("rbx.platform.typedef.__int16_t", context.makeString("short"));
        config("rbx.platform.typedef.__uint16_t", context.makeString("ushort"));
        config("rbx.platform.typedef.__int32_t", context.makeString("int"));
        config("rbx.platform.typedef.__uint32_t", context.makeString("uint"));
        config("rbx.platform.typedef.__int64_t", context.makeString("long_long"));
        config("rbx.platform.typedef.__uint64_t", context.makeString("ulong_long"));
        config("rbx.platform.typedef.__darwin_intptr_t", context.makeString("long"));
        config("rbx.platform.typedef.__darwin_natural_t", context.makeString("uint"));
        config("rbx.platform.typedef.__darwin_ct_rune_t", context.makeString("int"));
        config("rbx.platform.typedef.__darwin_ptrdiff_t", context.makeString("long"));
        config("rbx.platform.typedef.__darwin_size_t", context.makeString("ulong"));
        config("rbx.platform.typedef.__darwin_wchar_t", context.makeString("int"));
        config("rbx.platform.typedef.__darwin_rune_t", context.makeString("int"));
        config("rbx.platform.typedef.__darwin_wint_t", context.makeString("int"));
        config("rbx.platform.typedef.__darwin_clock_t", context.makeString("ulong"));
        config("rbx.platform.typedef.__darwin_socklen_t", context.makeString("uint"));
        config("rbx.platform.typedef.__darwin_ssize_t", context.makeString("long"));
        config("rbx.platform.typedef.__darwin_time_t", context.makeString("long"));
        config("rbx.platform.typedef.__darwin_blkcnt_t", context.makeString("long_long"));
        config("rbx.platform.typedef.__darwin_blksize_t", context.makeString("int"));
        config("rbx.platform.typedef.__darwin_dev_t", context.makeString("int"));
        config("rbx.platform.typedef.__darwin_fsblkcnt_t", context.makeString("uint"));
        config("rbx.platform.typedef.__darwin_fsfilcnt_t", context.makeString("uint"));
        config("rbx.platform.typedef.__darwin_gid_t", context.makeString("uint"));
        config("rbx.platform.typedef.__darwin_id_t", context.makeString("uint"));
        config("rbx.platform.typedef.__darwin_ino64_t", context.makeString("ulong_long"));
        config("rbx.platform.typedef.__darwin_ino_t", context.makeString("ulong_long"));
        config("rbx.platform.typedef.__darwin_mach_port_name_t", context.makeString("uint"));
        config("rbx.platform.typedef.__darwin_mach_port_t", context.makeString("uint"));
        config("rbx.platform.typedef.__darwin_mode_t", context.makeString("ushort"));
        config("rbx.platform.typedef.__darwin_off_t", context.makeString("long_long"));
        config("rbx.platform.typedef.__darwin_pid_t", context.makeString("int"));
        config("rbx.platform.typedef.__darwin_sigset_t", context.makeString("uint"));
        config("rbx.platform.typedef.__darwin_suseconds_t", context.makeString("int"));
        config("rbx.platform.typedef.__darwin_uid_t", context.makeString("uint"));
        config("rbx.platform.typedef.__darwin_useconds_t", context.makeString("uint"));
        config("rbx.platform.typedef.__darwin_pthread_key_t", context.makeString("ulong"));
        config("rbx.platform.typedef.intptr_t", context.makeString("long"));
        config("rbx.platform.typedef.uintptr_t", context.makeString("ulong"));
        config("rbx.platform.typedef.intmax_t", context.makeString("long"));
        config("rbx.platform.typedef.uintmax_t", context.makeString("ulong"));
        config("rbx.platform.typedef.u_int8_t", context.makeString("uchar"));
        config("rbx.platform.typedef.u_int16_t", context.makeString("ushort"));
        config("rbx.platform.typedef.u_int32_t", context.makeString("uint"));
        config("rbx.platform.typedef.u_int64_t", context.makeString("ulong_long"));
        config("rbx.platform.typedef.register_t", context.makeString("long_long"));
        config("rbx.platform.typedef.user_addr_t", context.makeString("ulong_long"));
        config("rbx.platform.typedef.user_size_t", context.makeString("ulong_long"));
        config("rbx.platform.typedef.user_ssize_t", context.makeString("long_long"));
        config("rbx.platform.typedef.user_long_t", context.makeString("long_long"));
        config("rbx.platform.typedef.user_ulong_t", context.makeString("ulong_long"));
        config("rbx.platform.typedef.user_time_t", context.makeString("long_long"));
        config("rbx.platform.typedef.user_off_t", context.makeString("long_long"));
        config("rbx.platform.typedef.syscall_arg_t", context.makeString("ulong_long"));
        config("rbx.platform.typedef.u_char", context.makeString("uchar"));
        config("rbx.platform.typedef.u_short", context.makeString("ushort"));
        config("rbx.platform.typedef.u_int", context.makeString("uint"));
        config("rbx.platform.typedef.u_long", context.makeString("ulong"));
        config("rbx.platform.typedef.ushort", context.makeString("ushort"));
        config("rbx.platform.typedef.uint", context.makeString("uint"));
        config("rbx.platform.typedef.u_quad_t", context.makeString("ulong_long"));
        config("rbx.platform.typedef.quad_t", context.makeString("long_long"));
        config("rbx.platform.typedef.qaddr_t", context.makeString("pointer"));
        config("rbx.platform.typedef.caddr_t", context.makeString("string"));
        config("rbx.platform.typedef.daddr_t", context.makeString("int"));
        config("rbx.platform.typedef.dev_t", context.makeString("int"));
        config("rbx.platform.typedef.fixpt_t", context.makeString("uint"));
        config("rbx.platform.typedef.blkcnt_t", context.makeString("long_long"));
        config("rbx.platform.typedef.blksize_t", context.makeString("int"));
        config("rbx.platform.typedef.gid_t", context.makeString("uint"));
        config("rbx.platform.typedef.in_addr_t", context.makeString("uint"));
        config("rbx.platform.typedef.in_port_t", context.makeString("ushort"));
        config("rbx.platform.typedef.ino_t", context.makeString("ulong_long"));
        config("rbx.platform.typedef.ino64_t", context.makeString("ulong_long"));
        config("rbx.platform.typedef.key_t", context.makeString("int"));
        config("rbx.platform.typedef.mode_t", context.makeString("ushort"));
        config("rbx.platform.typedef.nlink_t", context.makeString("ushort"));
        config("rbx.platform.typedef.id_t", context.makeString("uint"));
        config("rbx.platform.typedef.pid_t", context.makeString("int"));
        config("rbx.platform.typedef.off_t", context.makeString("long_long"));
        config("rbx.platform.typedef.segsz_t", context.makeString("int"));
        config("rbx.platform.typedef.swblk_t", context.makeString("int"));
        config("rbx.platform.typedef.uid_t", context.makeString("uint"));
        config("rbx.platform.typedef.clock_t", context.makeString("ulong"));
        config("rbx.platform.typedef.size_t", context.makeString("ulong"));
        config("rbx.platform.typedef.ssize_t", context.makeString("long"));
        config("rbx.platform.typedef.time_t", context.makeString("long"));
        config("rbx.platform.typedef.useconds_t", context.makeString("uint"));
        config("rbx.platform.typedef.suseconds_t", context.makeString("int"));
        config("rbx.platform.typedef.rsize_t", context.makeString("ulong"));
        config("rbx.platform.typedef.errno_t", context.makeString("int"));
        config("rbx.platform.typedef.fd_mask", context.makeString("int"));
        config("rbx.platform.typedef.pthread_key_t", context.makeString("ulong"));
        config("rbx.platform.typedef.fsblkcnt_t", context.makeString("uint"));
        config("rbx.platform.typedef.fsfilcnt_t", context.makeString("uint"));
        config("rbx.platform.typedef.sa_family_t", context.makeString("uchar"));
        config("rbx.platform.typedef.socklen_t", context.makeString("uint"));
        config("rbx.platform.typedef.rlim_t", context.makeString("ulong_long"));
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
