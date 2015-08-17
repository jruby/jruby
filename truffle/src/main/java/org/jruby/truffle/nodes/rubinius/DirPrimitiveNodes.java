/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
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
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.source.SourceSection;
import jnr.constants.platform.Errno;
import org.jruby.truffle.nodes.core.BasicObjectNodes;
import org.jruby.truffle.nodes.core.ClassNodes;
import org.jruby.truffle.nodes.core.ModuleNodes;
import org.jruby.truffle.om.dsl.api.Layout;
import org.jruby.truffle.om.dsl.api.Nullable;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;

import java.io.File;

public abstract class DirPrimitiveNodes {

    @Layout
    public interface DirLayout extends BasicObjectNodes.BasicObjectLayout {

        DynamicObjectFactory createDirShape(DynamicObject logicalClass, DynamicObject metaClass);

        DynamicObject createDir(DynamicObjectFactory factory, @Nullable Object contents, int position);

        Object getContents(DynamicObject object);
        void setContents(DynamicObject object, Object value);

        int getPosition(DynamicObject object);
        void setPosition(DynamicObject object, int value);

    }

    public static final DirLayout DIR_LAYOUT = DirLayoutImpl.INSTANCE;

    @RubiniusPrimitive(name = "dir_allocate")
    public static abstract class DirAllocatePrimitiveNode extends RubiniusPrimitiveNode {

        public DirAllocatePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject allocate(DynamicObject dirClass) {
            return DIR_LAYOUT.createDir(ClassNodes.CLASS_LAYOUT.getInstanceFactory(dirClass), null, 0);
        }

    }

    @RubiniusPrimitive(name = "dir_open")
    public static abstract class DirOpenPrimitiveNode extends RubiniusPrimitiveNode {

        public DirOpenPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyString(path)", "isNil(encoding)"})
        public DynamicObject open(DynamicObject dir, DynamicObject path, DynamicObject encoding) {
            // TODO CS 22-Apr-15 race conditions here

            final File file = new File(path.toString());

            if (!file.isDirectory()) {
                throw new RaiseException(getContext().getCoreLibrary().errnoError(Errno.ENOTDIR.intValue(), this));
            }

            final String[] contents = file.list();

            if (contents == null) {
                throw new UnsupportedOperationException();
            }

            DIR_LAYOUT.setContents(dir, contents);
            DIR_LAYOUT.setPosition(dir, -2); // -2 for . and then ..

            return nil();
        }

        @TruffleBoundary
        @Specialization(guards = {"isRubyString(path)", "isRubyEncoding(encoding)"})
        public DynamicObject openEncoding(DynamicObject dir, DynamicObject path, DynamicObject encoding) {
            // TODO BJF 30-APR-2015 HandleEncoding
            return open(dir, path, nil());
        }

    }

    @RubiniusPrimitive(name = "dir_read")
    public static abstract class DirReadPrimitiveNode extends RubiniusPrimitiveNode {

        public DirReadPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public Object read(DynamicObject dir) {
            final int position = DIR_LAYOUT.getPosition(dir);

            DIR_LAYOUT.setPosition(dir, position + 1);

            if (position == -2) {
                return createString(".");
            } else if (position == -1) {
                return createString("..");
            } else {
                final String[] contents = (String[]) DIR_LAYOUT.getContents(dir);

                if (position < contents.length) {
                    return createString(contents[position]);
                } else {
                    return nil();
                }
            }
        }

    }


    @RubiniusPrimitive(name = "dir_control")
    public static abstract class DirControlPrimitiveNode extends RubiniusPrimitiveNode {

        public DirControlPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public Object control(DynamicObject dir, int kind, int position) {
            switch (kind) {
                case 0:
                    DIR_LAYOUT.setPosition(dir, position);
                    return true;
                case 1:
                    DIR_LAYOUT.setPosition(dir, -2);
                    return true;
                case 2:
                    return DIR_LAYOUT.getPosition(dir);

            }
            return nil();
        }

    }

    @RubiniusPrimitive(name = "dir_close")
    public static abstract class DirClosePrimitiveNode extends RubiniusPrimitiveNode {

        public DirClosePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject open(DynamicObject dir) {
            return nil();
        }

    }

}
