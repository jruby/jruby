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
 * Copyright (C) 2009-2012 Yoko Harada <yokolet@gmail.com>
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.embed.internal.BiVariableMap;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * An implementation of BiVariable for a Ruby global variable.
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class GlobalVariable extends AbstractVariable {

    private static final String VALID_NAME = "\\$(([a-zA-Z]|_|\\d)*|-[a-zA-Z]|[!-~&&[^#%()-\\{\\}\\[\\]\\|\\^]])";

    /**
     * Returns an instance of this class. This factory method is used when a global
     * variable is put in {@link BiVariableMap}.
     *
     * @param runtime Ruby runtime
     * @param name a variable name
     * @param javaObject Java object that should be assigned to.
     * @return the instance of GlobalVariable
     */
    public static BiVariable getInstance(RubyObject receiver, String name, Object... javaObject) {
        if (name.matches(VALID_NAME)) {
            GlobalVariable var = new GlobalVariable(receiver, name, javaObject);
            var.tryEagerInjection(null);
            return var;
        }
        return null;
    }

    protected GlobalVariable(RubyObject receiver, String name, Object... javaObjects) {
        super(receiver, name, false);
        updateByJavaObject(receiver.getRuntime(), javaObjects);
    }

    /**
     * A constructor used when global variables are retrieved from Ruby.
     *
     * @param name the global variable name
     * @param irubyObject Ruby global object
     */
    GlobalVariable(RubyObject receiver, String name, IRubyObject irubyObject) {
        super(receiver, name, true, irubyObject);
    }

    /**
     * Retrieves global variables from Ruby after the evaluation.
     *
     * @param runtime Ruby runtime
     * @param receiver receiver object returned when a script is evaluated.
     * @param vars map to save retrieved global variables.
     */
    public static void retrieve(IRubyObject receiver, BiVariableMap vars) {
        if ( vars.isLazy() ) return;

        GlobalVariables globalVars = receiver.getRuntime().getGlobalVariables();
        for ( final String name : globalVars.getNames() ) {
            if ( isPredefined(name) ) continue;
            IRubyObject value = globalVars.get(name);
            // reciever of gvar should to topSelf always
            updateGlobalVar(vars, getTopSelf(receiver), name, value);
        }
    }

    private static void updateGlobalVar(final BiVariableMap vars,
            final RubyObject receiver, final String name, final IRubyObject value) {
        BiVariable var = vars.getVariable(receiver, name);
        if (var != null) {
            var.setRubyObject(value);
        } else {
            var = new GlobalVariable(receiver, name, value);
            vars.update(name, var);
        }
    }

    /**
     * Retrieves a global variable by key from Ruby after the evaluation.
     *
     * @param runtime Ruby runtime
     * @param receiver receiver object returned when a script is evaluated.
     * @param vars map to save a retrieved global variable.
     * @param key name of the global variable
     */
    public static void retrieveByKey(Ruby runtime, BiVariableMap vars, String key) {
        GlobalVariables globalVars = runtime.getGlobalVariables();
        // if the specified key doesn't exist, this method is called before the
        // evaluation. Don't update value in this case.
        if ( ! globalVars.getNames().contains(key) ) return;

        // the specified key is found, so let's update
        IRubyObject value = globalVars.get(key);
        updateGlobalVar(vars, (RubyObject) runtime.getTopSelf(), key, value);
    }

    private static final String[] PREDEFINED_PATTERNS = {
        "\\$([\\u0021-\\u0040]|\\u005c|[\\u005e-\\u0060]|\\u007e)",
        "\\$-(\\d|[A-z])"
    };
    private static final Set<String> PREDEFINED_NAMES = new HashSet<String>();

    static {
        final String[] NAMES = {
            "$DEBUG", "$F", "$FILENAME", "$KCODE", "$LOAD_PATH", "$SAFE", "$VERBOSE", "$CLASSPATH", "$LOADED_FEATURES",
            "$PROGRAM_NAME", "$FIELD_SEPARATOR", "$ERROR_POSITION", "$DEFAULT_OUTPUT", "$PREMATCH", "$RS", "$MATCH",
            "$LAST_READ_LINE", "$FS", "$INPUT_RECORD_SEPARATOR", "$PID", "$NR", "$ERROR_INFO", "$PROCESS_ID",
            "$OUTPUT_RECORD_SEPARATOR", "$INPUT_LINE_NUMBER", "$LAST_PAREN_MATCH", "$LAST_MATCH_INFO", "$CHILD_STATUS",
            "$IGNORECASE", "$DEFAULT_INPUT", "$OFS", "$OUTPUT_FIELD_SEPARATOR", "$POSTMATCH", "$ORS",
            "$configure_args", "$deferr", "$defout", "$expect_verbose", "$stderr", "$stdin", "$stdout"
        };
        PREDEFINED_NAMES.addAll(Arrays.asList(NAMES));
    }

    protected static boolean isPredefined(final String name) {
        for ( String pattern : PREDEFINED_PATTERNS ) {
            if ( name.matches(pattern) ) return true;
        }
        return GlobalVariable.PREDEFINED_NAMES.contains(name);
    }

    /**
     * Returns enum type of this variable defined in {@link BiVariable}.
     *
     * @return this enum type, BiVariable.Type.GlobalVariable.
     */
    @Override
    public Type getType() {
        return Type.GlobalVariable;
    }

    /**
     * Returns true if the given name is a decent Ruby global variable. Unless
     * returns false.
     *
     * @param name is a name to be checked.
     * @return true if the given name is of a Ruby global variable.
     */
    public static boolean isValidName(Object name) {
        return isValidName(VALID_NAME, name);
    }

    /**
     * Sets a Java object and its Ruby type as a value of this object.
     * At the same time, sets Ruby object to Ruby runtime.
     *
     * @param runtime is used to convert a Java object to Ruby object.
     * @param javaObject is a variable value to be set.
     */
    @Override
    public void setJavaObject(Ruby runtime, Object javaObject) {
        updateByJavaObject(runtime, javaObject);
        tryEagerInjection(runtime, null);
    }

    /**
     * A global variable is injected when it is set. This method does nothing.
     * Instead injection is done by tryEagerInjection.
     */
    @Override
    public void inject() {
        // do nothing
    }

    @Deprecated
    public void tryEagerInjection(Ruby runtime, IRubyObject receiver) {
        tryEagerInjection(receiver);
    }

    /**
     * Injects a global variable value to a parsed Ruby script. This method is
     * invoked during EvalUnit#run() is executed.
     *
     * @param runtime is environment where a variable injection occurs
     * @param receiver is the instance that will have variable injection.
     */
    public void tryEagerInjection(final IRubyObject receiver) {
        // wreckages of global local vars might remain on runtime, which may cause
        // assertion error since those names doesn't start from "$"
        final String name = this.name.startsWith("$") ? this.name : ("$" + this.name);
        synchronized (getRuntime()) {
            getRuntime().getGlobalVariables().set(name.intern(), irubyObject);
        }
    }

    /**
     * Attempts to remove this variable from top self or receiver.
     *
     */
    @Override
    public void remove() {
        synchronized (getRuntime()) {
            getRuntime().getGlobalVariables().clear(name.intern());
        }
    }

    /**
     * Returns true if a given receiver is identical to the receiver this object has.
     *
     * @return true always
     */
    @Override
    public boolean isReceiverIdentical(RubyObject recv) {
        return true;
    }

}