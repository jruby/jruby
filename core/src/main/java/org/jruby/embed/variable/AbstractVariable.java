/**
 * **** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2009-2011 Yoko Harada <yokolet@gmail.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed.variable;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
abstract class AbstractVariable implements BiVariable {
    /*
     * receiver a receiver object to inject this var in. When the variable/constant
     * is originated from Java, receiver may be null. During the injection, the
     * receiver must be set.
     */
    protected final IRubyObject receiver;
    protected final String name;
    protected Object javaObject = null;
    protected Class javaType = null;
    protected IRubyObject rubyObject = null;
    protected boolean fromRuby;

    /**
     * Constructor used when this variable is originaed from Java.
     *
     * @param runtime
     * @param name
     * @param fromRuby
     * @param values
     */
    protected AbstractVariable(IRubyObject receiver, String name, boolean fromRuby) {
        this.receiver = receiver;
        this.name = name;
        this.fromRuby = fromRuby;
    }

    /**
     * Constructor when the variable is originated from Ruby.
     *
     * @param receiver a receiver object that this variable/constant is originally in. When
     *        the variable/constant is originated from Ruby, receiver may not be null.
     * @param name
     * @param fromRuby
     * @param rubyObject
     */
    protected AbstractVariable(IRubyObject receiver, String name, boolean fromRuby, IRubyObject rubyObject) {
        this.receiver = receiver;
        this.name = name;
        this.fromRuby = fromRuby;
        this.rubyObject = rubyObject;
    }

    final Ruby getRuntime() { return receiver.getRuntime(); }

    final ThreadContext getCurrentContext() { return getRuntime().getCurrentContext(); }

    final RubyObject getTopSelf() { return getTopSelf(receiver); }

    static RubyObject getTopSelf(final IRubyObject receiver) {
        return (RubyObject) receiver.getRuntime().getTopSelf();
    }

    protected void updateByJavaObject(final Ruby runtime, Object... values) {
        assert values != null;
        javaObject = values[0];
        if (javaObject == null) {
            javaType = null;
        } else if (values.length > 1) {
            javaType = (Class) values[1];
        } else {
            javaType = javaObject.getClass();
        }
        rubyObject = JavaEmbedUtils.javaToRuby(runtime, javaObject);
        fromRuby = false;
    }

    protected void updateRubyObject(final IRubyObject rubyObject) {
        if ( rubyObject == null ) return;
        this.rubyObject = rubyObject;
        this.javaType = null;
        // NOTE: quite weird - but won't pass tests otherwise !?!
        //this.javaObject = null;
        // delays updating javaObject for performance.
    }

    public IRubyObject getReceiver() {
        return receiver;
    }

    /**
     * Returns true if a given receiver is identical to the receiver this object has.
     *
     * @return true if identical otherwise false
     */
    public boolean isReceiverIdentical(final RubyObject receiver) {
        return getReceiver().equals(receiver);
    }

    public String getName() {
        return name;
    }

    public Object getJavaObject() {
        if (rubyObject == null) return javaObject;

        if (javaType != null) { // Java originated variables
            javaObject = javaType.cast( rubyObject.toJava(javaType) );
        }
        else { // Ruby originated variables
            javaObject = rubyObject.toJava(Object.class);
            if (javaObject != null) {
                javaType = javaObject.getClass();
            }
        }
        return javaObject;
    }

    public void setJavaObject(final Ruby runtime, Object javaObject) {
        updateByJavaObject(runtime, javaObject);
    }

    public IRubyObject getRubyObject() {
        return rubyObject;
    }

    public void setRubyObject(final IRubyObject rubyObject) {
        updateRubyObject(rubyObject);
    }

    protected static RubyModule getRubyClass(final Ruby runtime) {
        ThreadContext context = runtime.getCurrentContext();
        StaticScope scope = context.getCurrentStaticScope();
        RubyModule rubyClass = scope.getModule();
        return rubyClass;
    }

    protected static boolean isValidName(String pattern, Object name) {
        if ( ! ( name instanceof String ) ) return false;
        return ( (String) name).matches(pattern);
    }
}
