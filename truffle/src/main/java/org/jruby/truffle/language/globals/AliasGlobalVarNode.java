/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.language.globals;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.language.RubyNode;

public class AliasGlobalVarNode extends RubyNode {

    private final String oldName;
    private final String newName;

    public AliasGlobalVarNode(String oldName, String newName) {
        this.oldName = oldName;
        this.newName = newName;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        GlobalVariables globalVariables = getContext().getCoreLibrary().getGlobalVariables();
        GlobalVariableStorage storage = globalVariables.getStorage(oldName);
        globalVariables.alias(newName, storage);
        return nil();
    }

}
