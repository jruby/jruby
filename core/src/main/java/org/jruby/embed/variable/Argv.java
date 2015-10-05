/**
 * **** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2011 Yoko Harada <yokolet@gmail.com>
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.RubyNil;
import org.jruby.RubyObject;
import org.jruby.embed.internal.BiVariableMap;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author yoko
 */
public class Argv extends AbstractVariable {

    private static final String VALID_NAME = "ARGV";

    /**
     * Returns an instance of this class. This factory method is used when an ARGV
     * is put in {@link BiVariableMap}.
     *
     * @param runtime
     * @param name a variable name
     * @param javaObject Java object that should be assigned to.
     * @return the instance of Constant
     */
    public static BiVariable getInstance(RubyObject receiver, String name, Object... javaObject) {
        if ( name.equals(VALID_NAME) ) {
            return new Argv(receiver, name, javaObject);
        }
        return null;
    }

    private Argv(RubyObject receiver, String name, Object... javaObjects) {
        super(receiver, name, false);
        assert javaObjects != null;
        javaObject = javaObjects[0];
        if (javaObject == null) {
            javaType = null;
        } else if (javaObjects.length > 1) {
            javaType = (Class) javaObjects[1];
        } else {
            javaType = javaObject.getClass();
        }
    }

    /**
     * A constructor used when ARGV is retrieved from Ruby.
     *
     * @param receiver a receiver object that this variable/constant is originally in. When
     *        the variable/constant is originated from Ruby, receiver may not be null.
     * @param name the constant name
     * @param irubyObject Ruby constant object
     */
    Argv(RubyObject receiver, String name, IRubyObject irubyObject) {
        super(receiver, name, true, irubyObject);
    }

    /**
     * Returns enum type of this variable defined in {@link BiVariable}.
     *
     * @return this enum type, BiVariable.Type.InstanceVariable.
     */
    @Override
    public Type getType() {
        return Type.Argv;
    }

    /**
     * Returns true if the given name is ARGV. Unless returns false.
     *
     * @param name is a name to be checked.
     * @return true if the given name is ARGV.
     */
    public static boolean isValidName(Object name) {
        return isValidName(VALID_NAME, name);
    }

    /**
     * Injects ARGV values to a parsed Ruby script. This method is
     * invoked during EvalUnit#run() is executed.
     *
     * @param runtime is environment where a variable injection occurs
     * @param receiver is the instance that will have variable injection.
     */
    @Override
    public void inject() {
        final Ruby runtime = getRuntime();

        final RubyArray argv = RubyArray.newArray(runtime);
        if ( javaObject instanceof Collection ) {
            argv.addAll( (Collection) javaObject );
        }
        else if ( javaObject instanceof String[] ) {
            for ( String str : (String[]) javaObject ) argv.add(str);
        }
        this.irubyObject = argv; fromRuby = true;

        RubyModule rubyModule = getRubyClass(runtime);
        // SSS FIXME: With rubyclass stack gone, this needs a replacement
        if (rubyModule == null) rubyModule = null; // receiver.getRuntime().getCurrentContext().getRubyClass();

        if (rubyModule == null) return;

        rubyModule.storeConstant(name, argv);
        runtime.getConstantInvalidator(name).invalidate();
    }

    /**
     * Removes this object from {@link BiVariableMap}. Also, initialize
     * this variable in top self.
     */
    @Override
    public void remove() {
        this.javaObject = new ArrayList();
        inject();
    }

   /**
     * Retrieves ARGV from Ruby after the evaluation or method invocation.
     *
     * @param runtime Ruby runtime
     * @param receiver receiver object returned when a script is evaluated.
     * @param vars map to save retrieved constants.
     */
    public static void retrieve(final RubyObject receiver, final BiVariableMap vars) {
        if ( vars.isLazy() ) return;
        updateARGV(receiver, vars);
    }

    private static void updateARGV(final IRubyObject receiver, final BiVariableMap vars) {
        final String name = "ARGV";
        final RubyObject topSelf = getTopSelf(receiver);
        final IRubyObject argv = topSelf.getMetaClass().getConstant(name);
        if ( argv == null || (argv instanceof RubyNil) ) return;
        // ARGV constant should be only one
        if ( vars.containsKey(name) ) {
            BiVariable var = vars.getVariable(topSelf, name);
            var.setRubyObject(argv);
        }
        else {
            vars.update(name, new Argv(topSelf, name, argv));
        }
    }

    /**
     * Retrieves ARGV by key from Ruby runtime after the evaluation.
     * This method is used when eager retrieval is off.
     *
     * @param receiver receiver object returned when a script is evaluated.
     * @param vars map to save retrieved instance variables.
     * @param key instace varible name
     */
    public static void retrieveByKey(RubyObject receiver, BiVariableMap vars, String key) {
        assert key.equals("ARGV");
        updateARGV(receiver, vars);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object getJavaObject() {
        if ( irubyObject == null || ! fromRuby ) return javaObject;

        final RubyArray ary = (RubyArray) irubyObject;
        if (javaType == null) { // firstly retrieved from Ruby
            return javaObject = new ArrayList<String>(ary);
        }
        else if (javaType == String[].class) {
            String[] strArr = new String[ ary.size() ];
            for ( int i=0; i<ary.size(); i++ ) {
                strArr[i] = (String) ary.get(i);
            }
            return javaObject = strArr;
        }
        else if (javaObject instanceof List) {
            try {
                ((List) javaObject).clear();
                ((List) javaObject).addAll(ary);
            }
            catch (UnsupportedOperationException e) {
                // no op. no way to update.
            }
            return javaObject;
        }
        return null;
    }

}
