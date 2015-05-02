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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.source.SourceSection;
import jnr.constants.platform.Errno;
import org.jruby.truffle.nodes.objectstorage.ReadHeadObjectFieldNode;
import org.jruby.truffle.nodes.objectstorage.WriteHeadObjectFieldNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyEncoding;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.core.RubyString;

import java.io.File;

public abstract class DirPrimitiveNodes {

    // TODO CS 14-April-15 use a shape, properties and allocator

    private static final HiddenKey contentsKey = new HiddenKey("contents");
    private static final HiddenKey positionKey = new HiddenKey("position");

    @RubiniusPrimitive(name = "dir_allocate")
    public static abstract class DirAllocatePrimitiveNode extends RubiniusPrimitiveNode {

        public DirAllocatePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject allocate(RubyClass dirClass) {
            return new RubyBasicObject(dirClass);
        }

    }

    @RubiniusPrimitive(name = "dir_open")
    public static abstract class DirOpenPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private WriteHeadObjectFieldNode writeContentsNode;
        @Child private WriteHeadObjectFieldNode writePositionNode;

        public DirOpenPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            writeContentsNode = new WriteHeadObjectFieldNode(contentsKey);
            writePositionNode = new WriteHeadObjectFieldNode(positionKey);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyNilClass open(RubyBasicObject dir, RubyString path, RubyNilClass encoding) {
            // TODO CS 22-Apr-15 race conditions here

            final File file = new File(path.toString());

            if (!file.isDirectory()) {
                throw new RaiseException(getContext().getCoreLibrary().errnoError(Errno.ENOTDIR.intValue(), this));
            }

            final String[] contents = file.list();

            if (contents == null) {
                throw new UnsupportedOperationException();
            }

            writeContentsNode.execute(dir, contents);
            writePositionNode.execute(dir, -2); // -2 for . and then ..

            return nil();
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyNilClass open(RubyBasicObject dir, RubyString path, RubyEncoding encoding) {
            // TODO BJF 30-APR-2015 HandleEncoding
            return open(dir, path, nil());
        }

    }

    @RubiniusPrimitive(name = "dir_read")
    public static abstract class DirReadPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private ReadHeadObjectFieldNode readContentsNode;
        @Child private ReadHeadObjectFieldNode readPositionNode;
        @Child private WriteHeadObjectFieldNode writePositionNode;

        public DirReadPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            readContentsNode = new ReadHeadObjectFieldNode(contentsKey);
            readPositionNode = new ReadHeadObjectFieldNode(positionKey);
            writePositionNode = new WriteHeadObjectFieldNode(positionKey);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public Object read(RubyBasicObject dir) {
            final int position;

            try {
                position = readPositionNode.executeInteger(dir);
            } catch (UnexpectedResultException e) {
                throw new IllegalStateException();
            }

            writePositionNode.execute(dir, position + 1);

            if (position == -2) {
                return getContext().makeString(".");
            } else if (position == -1) {
                return getContext().makeString("..");
            } else {
                final String[] contents = (String[]) readContentsNode.execute(dir);

                if (position < contents.length) {
                    return getContext().makeString(contents[position]);
                } else {
                    return nil();
                }
            }
        }

    }


    @RubiniusPrimitive(name = "dir_control")
    public static abstract class DirControlPrimitiveNode extends RubiniusPrimitiveNode {

        @Child private ReadHeadObjectFieldNode readPositionNode;
        @Child private WriteHeadObjectFieldNode writePositionNode;

        public DirControlPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            readPositionNode = new ReadHeadObjectFieldNode(positionKey);
            writePositionNode = new WriteHeadObjectFieldNode(positionKey);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public Object control(RubyBasicObject dir, int kind, int position) {
            switch (kind) {
                case 0:
                    writePositionNode.execute(dir, position);
                    return true;
                case 1:
                    writePositionNode.execute(dir, -2);
                    return true;
                case 2:
                    try {
                        return readPositionNode.executeInteger(dir);
                    } catch (UnexpectedResultException e) {
                        throw new IllegalStateException();
                    }

            }
            return nil();
        }

    }



    @RubiniusPrimitive(name = "dir_close")
    public static abstract class DirClosePrimitiveNode extends RubiniusPrimitiveNode {

        public DirClosePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyNilClass open(RubyBasicObject dir) {
            return nil();
        }

    }

}
