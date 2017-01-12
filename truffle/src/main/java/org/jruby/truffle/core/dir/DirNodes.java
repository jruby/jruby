/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * Some of the code in this class is transliterated from C++ code in Rubinius.
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
package org.jruby.truffle.core.dir;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import jnr.constants.platform.Errno;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.builtins.Primitive;
import org.jruby.truffle.builtins.PrimitiveArrayArgumentsNode;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.platform.UnsafeGroup;

import java.io.File;

public abstract class DirNodes {

    @Primitive(name = "dir_allocate")
    public static abstract class DirAllocatePrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Child private AllocateObjectNode allocateNode = AllocateObjectNode.create();

        @Specialization
        public DynamicObject allocate(DynamicObject dirClass) {
            return allocateNode.allocate(dirClass, null, 0);
        }

    }

    @Primitive(name = "dir_open", unsafe = UnsafeGroup.IO)
    public static abstract class DirOpenPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = {"isRubyString(path)", "isNil(encoding)"})
        public DynamicObject open(DynamicObject dir, DynamicObject path, DynamicObject encoding) {
            // TODO CS 22-Apr-15 race conditions here

            final File file = new File(StringOperations.decodeUTF8(path));

            if (!file.isDirectory()) {
                throw new RaiseException(coreExceptions().errnoError(Errno.ENOTDIR.intValue(), this));
            }

            final String[] contents = file.list();

            if (contents == null) {
                throw new UnsupportedOperationException();
            }

            Layouts.DIR.setContents(dir, contents);
            Layouts.DIR.setPosition(dir, -2); // -2 for . and then ..

            return nil();
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyString(path)", "isRubyEncoding(encoding)"})
        public DynamicObject openEncoding(DynamicObject dir, DynamicObject path, DynamicObject encoding) {
            // TODO BJF 30-APR-2015 HandleEncoding
            return open(dir, path, nil());
        }

    }

    @Primitive(name = "dir_read", unsafe = UnsafeGroup.IO)
    public static abstract class DirReadPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public Object read(DynamicObject dir) {
            final int position = Layouts.DIR.getPosition(dir);

            Layouts.DIR.setPosition(dir, position + 1);

            if (position == -2) {
                return create7BitString(".", UTF8Encoding.INSTANCE);
            } else if (position == -1) {
                return create7BitString("..", UTF8Encoding.INSTANCE);
            } else {
                final String[] contents = (String[]) Layouts.DIR.getContents(dir);

                if (position < contents.length) {
                    return createString(StringOperations.encodeRope(contents[position], UTF8Encoding.INSTANCE));
                } else {
                    return nil();
                }
            }
        }

    }


    @Primitive(name = "dir_control", unsafe = UnsafeGroup.IO)
    public static abstract class DirControlPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public Object control(DynamicObject dir, int kind, int position) {
            switch (kind) {
                case 0:
                    Layouts.DIR.setPosition(dir, position);
                    return true;
                case 1:
                    Layouts.DIR.setPosition(dir, -2);
                    return true;
                case 2:
                    return Layouts.DIR.getPosition(dir);

            }
            return nil();
        }

    }

    @Primitive(name = "dir_close", unsafe = UnsafeGroup.IO)
    public static abstract class DirClosePrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject open(DynamicObject dir) {
            return nil();
        }

    }

}
