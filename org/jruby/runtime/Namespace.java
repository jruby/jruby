/*
 * Namespace.java
 * Created on 14.02.2002, 19:24:22
 *
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        Jan Arne Petersen (jpetersen@uni-bonn.de)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "JRuby" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact jpetersen@uni-bonn.de.
 *
 * 5. Products derived from this software may not be called
 *    "JRuby", nor may "JRuby" appear in their name, without prior
 *    written permission of Jan Arne Petersen.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JAN ARNE PETERSEN OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * ====================================================================
 *
 */
package org.jruby.runtime;

import org.jruby.*;
import org.jruby.runtime.builtin.IRubyObject;

/** Represents an element in the nested module/class namespace hierarchy.
 *
 * Example:
 *
 * <pre>
 * module JRuby
 *    class ExampleClass
 *       #1
 *    end
 * end
 * </pre>
 *
 * At point #1 there is the Namespace structure:
 *
 * <pre>
 * Namespace -> module = ExampleClass
 *           -> parent = Namespace -> mdoule = JRuby
 *                                 -> parent = Namespace -> module = Object
 *                                                       -> parent = null
 * </pre>
 *
 * Replaces CRefNode.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class Namespace {
    private Namespace parent;

    private RubyModule namespaceModule;

    public Namespace(RubyModule namespaceModule) {
        this(namespaceModule, null);
    }

    public Namespace(RubyModule namespaceModule, Namespace parent) {
        this.namespaceModule = namespaceModule;
        this.parent = parent;
    }

    public Namespace cloneNamespace() {
        return new Namespace(namespaceModule, parent != null ? parent.cloneNamespace() : null);
    }

    public Namespace getParent() {
        return parent;
    }

    public void setParent(Namespace newParent) {
        parent = newParent;
    }

    /**
     * Gets the namespaceModule.
     * @return Returns a RubyModule
     */
    public RubyModule getNamespaceModule() {
        return namespaceModule;
    }

    /**
     * Sets the namespaceModule.
     * @param namespaceModule The namespaceModule to set
     */
    public void setNamespaceModule(RubyModule namespaceModule) {
        this.namespaceModule = namespaceModule;
    }

    public IRubyObject getConstant(IRubyObject self, String name) {
        for (Namespace ns = this; ns != null && ns.getParent() != null; ns = ns.getParent()) {
            if (ns.getNamespaceModule() == null) {
                return self.getInternalClass().getConstant(name);
            } else if (ns.getNamespaceModule().hasInstanceVariable(name)) {
                return (IRubyObject) ns.getNamespaceModule().getInstanceVariable(name);
            }
        }
        return getNamespaceModule().getConstant(name);
    }
}
