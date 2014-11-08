/*
 * Adapted from https://github.com/rubinius/rubinius-core-api.
 *
 * Copyright (c) 2011, Evan Phoenix
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
 * * Neither the name of the Evan Phoenix nor the names of its contributors
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
package org.jruby.truffle.runtime.rubinius;

import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.control.RaiseException;

public class RubiniusLibrary {

    private final RubyModule rubiniusModule;
    private final RubyClass tupleClass;
    private final RubyModule typeModule;
    private final RubyModule environmentAccessModule;
    private final RubyClass channelClass;
    private final RubyClass byteArrayCLass;
    private final RubyClass vmExceptionClass;
    private final RubyClass objectBoundsExceededErrorClass;
    private final RubyClass assertionErrorClass;

	public RubiniusLibrary(CoreLibrary coreLib) {
		rubiniusModule = new RubyModule(coreLib.getModuleClass(), null, "Rubinius");
		tupleClass = new RubyClass(rubiniusModule, coreLib.getObjectClass(), "Tuple");
        typeModule = new RubyModule(coreLib.getModuleClass(), rubiniusModule, "Type");
        environmentAccessModule = new RubyModule(coreLib.getModuleClass(), rubiniusModule, "EnvironmentAccess");
        channelClass = new RubyClass(rubiniusModule, coreLib.getObjectClass(), "Channel");
        byteArrayCLass = new RubyClass(rubiniusModule, coreLib.getObjectClass(), "ByteArray");

        //TODO: how is Rubinius.LookupTable specified?
        // rubinius.setConstant("LookupTable", currentNode.getHash());

        vmExceptionClass = new RubyClass(rubiniusModule, coreLib.getExceptionClass(), "VMException");
        objectBoundsExceededErrorClass = new RubyClass(rubiniusModule, vmExceptionClass, "ObjectBoundsExceededError");
        assertionErrorClass = new RubyClass(rubiniusModule, vmExceptionClass, "AssertionError");

        coreLib.getObjectClass().setConstant(null, rubiniusModule.getName(), rubiniusModule);

        final String[] files = new String[]{
                "jruby/truffle/core/rubinius/api/bootstrap/channel.rb",
                "jruby/truffle/core/rubinius/api/common/bytearray.rb",
                "jruby/truffle/core/rubinius/api/common/channel.rb",
                "jruby/truffle/core/rubinius/api/common/thread.rb",
                "jruby/truffle/core/rubinius/api/common/tuple.rb",
                "jruby/truffle/core/rubinius/api/common/type.rb",
                "jruby/truffle/core/rubinius/kernel/common/struct.rb"
                //"jruby/truffle/core/rubinius/kernel/common/time.rb"
        };

        for (String file : files) {
            coreLib.loadRubyCore(file);
        }
	}

    // helper function, should maybe be moved elsewhere
    public static RubyException throwObjectBoundsExceededError(RubyNode currentNode, String message) {
        throw new RaiseException(currentNode.getContext().getCoreLibrary().getRubiniusLibrary().objectBoundsExceededError(currentNode, message));
    }

    // helper function, should maybe be moved elsewhere
    public static void throwArgumentError(RubyNode currentNode, String message) {
        throw new RaiseException(currentNode.getContext().getCoreLibrary().argumentError(message, currentNode));
    }

	public RubyException objectBoundsExceededError(RubyNode currentNode, String message) {
		return new RubyException(objectBoundsExceededErrorClass, currentNode.getContext().makeString(message), RubyCallStack.getBacktrace(currentNode));
	}

    public RubyModule getRubiniusModule() {
        return rubiniusModule;
    }

    public RubyClass getTupleClass() {
        return tupleClass;
    }

    public RubyModule getTypeModule() {
        return typeModule;
    }

    public RubyModule getEnvironmentAccessModule() {
        return environmentAccessModule;
    }

    public RubyClass getChannelClass() {
        return channelClass;
    }

    public RubyClass getByteArrayCLass() {
        return byteArrayCLass;
    }

    public RubyClass getVmExceptionClass() {
        return vmExceptionClass;
    }

    public RubyClass getObjectBoundsExceededErrorClass() {
        return objectBoundsExceededErrorClass;
    }

    public RubyClass getAssertionErrorClass() {
        return assertionErrorClass;
    }
}
