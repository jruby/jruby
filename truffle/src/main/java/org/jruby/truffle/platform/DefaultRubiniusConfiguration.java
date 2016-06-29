/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * This file contains configuration.configuration values translated from Rubinius.
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
package org.jruby.truffle.platform;

import com.oracle.truffle.api.object.DynamicObject;
import jnr.constants.platform.Fcntl;
import jnr.constants.platform.OpenFlags;
import jnr.posix.FileStat;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.numeric.BignumOperations;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.string.StringOperations;

import java.math.BigInteger;
import java.util.Arrays;

public abstract class DefaultRubiniusConfiguration {

    public static final int SIZE_OF_INT = 4;
    public static final int SIZE_OF_LONG = 8;
    public static final int SIZE_OF_POINTER = 8;

    public static void load(RubiniusConfiguration configuration, RubyContext context) {
        configuration.config("rbx.platform.file.S_IRUSR", FileStat.S_IRUSR);
        configuration.config("rbx.platform.file.S_IWUSR", FileStat.S_IWUSR);
        configuration.config("rbx.platform.file.S_IXUSR", FileStat.S_IXUSR);
        configuration.config("rbx.platform.file.S_IRGRP", FileStat.S_IRGRP);
        configuration.config("rbx.platform.file.S_IWGRP", FileStat.S_IWGRP);
        configuration.config("rbx.platform.file.S_IXGRP", FileStat.S_IXGRP);
        configuration.config("rbx.platform.file.S_IROTH", FileStat.S_IROTH);
        configuration.config("rbx.platform.file.S_IWOTH", FileStat.S_IWOTH);
        configuration.config("rbx.platform.file.S_IXOTH", FileStat.S_IXOTH);
        configuration.config("rbx.platform.file.S_IFMT", FileStat.S_IFMT);
        configuration.config("rbx.platform.file.S_IFIFO", FileStat.S_IFIFO);
        configuration.config("rbx.platform.file.S_IFCHR", FileStat.S_IFCHR);
        configuration.config("rbx.platform.file.S_IFDIR", FileStat.S_IFDIR);
        configuration.config("rbx.platform.file.S_IFBLK", FileStat.S_IFBLK);
        configuration.config("rbx.platform.file.S_IFREG", FileStat.S_IFREG);
        configuration.config("rbx.platform.file.S_IFLNK", FileStat.S_IFLNK);
        configuration.config("rbx.platform.file.S_IFSOCK", FileStat.S_IFSOCK);
        configuration.config("rbx.platform.file.S_ISUID", FileStat.S_ISUID);
        configuration.config("rbx.platform.file.S_ISGID", FileStat.S_ISGID);
        configuration.config("rbx.platform.file.S_ISVTX", FileStat.S_ISVTX);

        for (Fcntl fcntl : Fcntl.values()) {
            if (fcntl.defined()) {
                configuration.config("rbx.platform.fcntl." + fcntl.name(), fcntl.intValue());
            }
        }

        configuration.config("rbx.platform.fcntl.FD_CLOEXEC", 1); // TODO BJF 15-May-2015 Get from JNR constants or stdlib FFI

        for (OpenFlags openFlag : OpenFlags.values()) {
            if (openFlag.defined()) {
                configuration.config("rbx.platform.file." + openFlag.name(), openFlag.intValue());
            }
        }

        configuration.config("rbx.platform.fcntl.O_ACCMODE", OpenFlags.O_RDONLY.intValue()
                | OpenFlags.O_WRONLY.intValue() | OpenFlags.O_RDWR.intValue());

        configuration.config("rbx.platform.typedef.time_t", context.getSymbolTable().getSymbol("long"));

        configuration.config("rbx.platform.timeval.sizeof", 2 * SIZE_OF_LONG);
        configuration.config("rbx.platform.timeval.tv_sec.offset", 0);
        configuration.config("rbx.platform.timeval.tv_sec.size", SIZE_OF_LONG);
        configuration.config("rbx.platform.timeval.tv_sec.type", context.getSymbolTable().getSymbol("time_t"));
        configuration.config("rbx.platform.timeval.tv_usec.offset", SIZE_OF_LONG);
        configuration.config("rbx.platform.timeval.tv_usec.size", SIZE_OF_LONG);
        configuration.config("rbx.platform.timeval.tv_usec.type", context.getSymbolTable().getSymbol("time_t"));

        /*
         * struct linger {
         *     int              l_onoff;
         *     int              l_linger;
         * };
         */

        int lingerOffset = 0;

        for (String field : Arrays.asList("l_onoff", "l_linger")) {
            configuration.config("rbx.platform.linger." + field + ".offset", lingerOffset);
            configuration.config("rbx.platform.linger." + field + ".size", SIZE_OF_INT);
            configuration.config("rbx.platform.linger." + field + ".type", context.getSymbolTable().getSymbol("int"));
            lingerOffset += SIZE_OF_INT;
        }

        configuration.config("rbx.platform.linger.sizeof", lingerOffset);

        /*
         * struct servent {
         *     char  *s_name;
         *     char **s_aliases;
         *     int    s_port;
         *     char  *s_proto;
         * };
         */

        configuration.config("rbx.platform.servent.sizeof", 3 * SIZE_OF_POINTER + SIZE_OF_INT);

        configuration.config("rbx.platform.servent.s_name.offset", 0);
        configuration.config("rbx.platform.servent.s_name.size", SIZE_OF_POINTER);
        configuration.config("rbx.platform.servent.s_name.type", context.getSymbolTable().getSymbol("pointer"));

        configuration.config("rbx.platform.servent.s_aliases.offset", SIZE_OF_POINTER);
        configuration.config("rbx.platform.servent.s_aliases.size", SIZE_OF_POINTER);
        configuration.config("rbx.platform.servent.s_aliases.type", context.getSymbolTable().getSymbol("pointer"));

        configuration.config("rbx.platform.servent.s_port.offset", 2 * SIZE_OF_POINTER);
        configuration.config("rbx.platform.servent.s_port.size", SIZE_OF_INT);
        configuration.config("rbx.platform.servent.s_port.type", context.getSymbolTable().getSymbol("int"));

        configuration.config("rbx.platform.servent.s_proto.offset", 2 * SIZE_OF_POINTER + SIZE_OF_INT);
        configuration.config("rbx.platform.servent.s_proto.size", SIZE_OF_POINTER);
        configuration.config("rbx.platform.servent.s_proto.type", context.getSymbolTable().getSymbol("pointer"));

        configuration.config("rbx.platform.io.SEEK_SET", 0);
        configuration.config("rbx.platform.io.SEEK_CUR", 1);
        configuration.config("rbx.platform.io.SEEK_END", 2);

        configuration.config("rbx.platform.socket.AI_PASSIVE", string(context, "1"));
        configuration.config("rbx.platform.socket.AF_UNSPEC", string(context, "0"));
        configuration.config("rbx.platform.socket.SOCK_STREAM", string(context, "1"));
        configuration.config("rbx.platform.socket.SOCK_STREAM", string(context, "1"));
    }

    protected static DynamicObject newBignum(RubyContext context, String value) {
        return BignumOperations.createBignum(context, new BigInteger(value));
    }

    protected static DynamicObject string(RubyContext context, String value) {
        return StringOperations.createString(context, StringOperations.encodeRope(value, UTF8Encoding.INSTANCE, CodeRange.CR_7BIT));
    }

}
