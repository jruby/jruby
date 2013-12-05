/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2009 Charles O Nutter <headius@headius.com>
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
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.util.Collections;
import java.util.List;

import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.InstanceVariables;
import org.jruby.runtime.builtin.InternalVariables;
import org.jruby.runtime.builtin.RubyJavaObject;
import org.jruby.runtime.builtin.Variable;

import static org.jruby.runtime.invokedynamic.MethodNames.INSPECT;
import static org.jruby.runtime.Helpers.invokedynamic;
import org.jruby.util.TypeConverter;

public final class BasicObjectStub {
    public static IRubyObject callSuper(IRubyObject self, ThreadContext context, IRubyObject[] args, Block block) {
        return Helpers.invokeSuper(context, self, args, block);
    }

    public static IRubyObject callMethod(IRubyObject self, ThreadContext context, String name) {
        return Helpers.invoke(context, self, name);
    }

    public static IRubyObject callMethod(IRubyObject self, ThreadContext context, String name, IRubyObject arg) {
        return Helpers.invoke(context, self, name, arg);
    }

    public static IRubyObject callMethod(IRubyObject self, ThreadContext context, String name, IRubyObject[] args) {
        return Helpers.invoke(context, self, name, args);
    }

    public static IRubyObject callMethod(IRubyObject self, ThreadContext context, String name, IRubyObject[] args, Block block) {
        return Helpers.invoke(context, self, name, args, block);
    }

    public static IRubyObject callMethod(IRubyObject self, ThreadContext context, int methodIndex, String name) {
        return Helpers.invoke(context, self, name);
    }

    public static IRubyObject callMethod(IRubyObject self, ThreadContext context, int methodIndex, String name, IRubyObject arg) {
        return Helpers.invoke(context, self, name, arg);
    }

    public static boolean isNil(IRubyObject self) {
        return false;
    }

    public static boolean isTrue(IRubyObject self) {
        return true;
    }

    public static boolean isTaint(IRubyObject self) {
        return false;
    }

    public static void setTaint(IRubyObject self, boolean b) {
    }

    public static IRubyObject infectBy(IRubyObject self, IRubyObject obj) {
        return self;
    }

    public static boolean isFrozen(IRubyObject self) {
        return false;
    }

    public static void setFrozen(IRubyObject self, boolean b) {
    }

    public static boolean isUntrusted(IRubyObject self) {
        return false;
    }

    public static void setUntrusted(IRubyObject self, boolean b) {
    }

    public static boolean isImmediate(IRubyObject self) {
        return false;
    }

    public static RubyClass getMetaClass(IRubyObject self) {
        if (self instanceof RubyBasicObject) {
            return ((RubyBasicObject)self).getMetaClass();
        } else if (self instanceof RubyJavaObject) {
            return ((RubyJavaObject)self).getMetaClass();
        } else {
            throw new RuntimeException("unknown object type in BasicObjectStuff.getMetaClass: " + self.getClass());
        }
    }

    public static RubyClass getSingletonClass(IRubyObject self) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static RubyClass getType(IRubyObject self) {
        return getMetaClass(self).getRealClass();
    }

    public static boolean respondsTo(IRubyObject self, String name) {
        if(getMetaClass(self).searchMethod("respond_to?").equals(getRuntime(self).getRespondToMethod())) {
            return getMetaClass(self).isMethodBound(name, false);
        } else {
            return callMethod(self, getRuntime(self).getCurrentContext(), "respond_to?", getRuntime(self).newSymbol(name)).isTrue();
        }
    }

    public static Ruby getRuntime(IRubyObject self) {
        return getMetaClass(self).getClassRuntime();
    }

    public static Class getJavaClass(IRubyObject self) {
        return self.getClass();
    }

    public static String asJavaString(IRubyObject self) {
        IRubyObject asString = checkStringType(self);
        if(!asString.isNil()) return ((RubyString)asString).asJavaString();
        throw getRuntime(self).newTypeError(inspect(self).toString() + " is not a string");
    }

    public static RubyString asString(IRubyObject self) {
        IRubyObject str = Helpers.invoke(getRuntime(self).getCurrentContext(), self, "to_s");

        if (!(str instanceof RubyString)) return (RubyString)anyToString(self);
        if (isTaint(self)) str.setTaint(true);
        return (RubyString) str;
    }

    public static RubyArray convertToArray(IRubyObject self) {
        return (RubyArray) TypeConverter.convertToType(self, getRuntime(self).getArray(), "to_ary");
    }

    public static RubyHash convertToHash(IRubyObject self) {
        return (RubyHash)TypeConverter.convertToType(self, getRuntime(self).getHash(), "to_hash");
    }

    public static RubyFloat convertToFloat(IRubyObject self) {
        return (RubyFloat) TypeConverter.convertToType(self, getRuntime(self).getFloat(), "to_f");
    }

    public static RubyInteger convertToInteger(IRubyObject self) {
        return convertToInteger(self, "to_int");
    }

    public static RubyInteger convertToInteger(IRubyObject self, int convertMethodIndex, String convertMethod) {
        return convertToInteger(self, convertMethod);
    }

    public static RubyInteger convertToInteger(IRubyObject self, String convertMethod) {
        IRubyObject val = TypeConverter.convertToType(self, getRuntime(self).getInteger(), convertMethod, true);
        if (!(val instanceof RubyInteger)) throw getRuntime(self).newTypeError(getMetaClass(self).getName() + "#" + convertMethod + " should return Integer");
        return (RubyInteger)val;
    }

    public static RubyString convertToString(IRubyObject self) {
        return (RubyString) TypeConverter.convertToType(self, getRuntime(self).getString(), "to_str");
    }

    public static IRubyObject anyToString(IRubyObject self) {
        String cname = getMetaClass(self).getRealClass().getName();
        /* 6:tags 16:addr 1:eos */
        RubyString str = getRuntime(self).newString("#<" + cname + ":0x" + Integer.toHexString(System.identityHashCode(self)) + ">");
        str.setTaint(isTaint(self));
        return str;
    }

    public static IRubyObject checkStringType(IRubyObject self) {
        IRubyObject str = TypeConverter.convertToTypeWithCheck(self, getRuntime(self).getString(), "to_str");
        if(!str.isNil() && !(str instanceof RubyString)) {
            str = RubyString.newEmptyString(getRuntime(self));
        }
        return str;
    }

    public static IRubyObject checkArrayType(IRubyObject self) {
        return TypeConverter.convertToTypeWithCheck(self, getRuntime(self).getArray(), "to_ary");
    }

    public static Object toJava(IRubyObject self, Class cls) {
        if (cls.isAssignableFrom(self.getClass())) {
            return self;
        } else {
            throw getRuntime(self).newTypeError("could not convert " + self.getClass() + " to " + cls);
        }
    }

    public static IRubyObject dup(IRubyObject self) {
        // TODO: java.lang.Object.clone?
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static IRubyObject inspect(IRubyObject self) {
        Ruby runtime = getRuntime(self);
        if (hasVariables(self)) {
            StringBuilder part = new StringBuilder();
            String cname = getMetaClass(self).getRealClass().getName();
            part.append("#<").append(cname).append(":0x");
            part.append(Integer.toHexString(System.identityHashCode(self)));

            if (runtime.isInspecting(self)) {
                /* 6:tags 16:addr 1:eos */
                part.append(" ...>");
                return runtime.newString(part.toString());
            }
            try {
                runtime.registerInspecting(self);
                return runtime.newString(inspectObj(self, part).toString());
            } finally {
                runtime.unregisterInspecting(self);
            }
        }

        return Helpers.invoke(runtime.getCurrentContext(), self, "to_s");
    }


    /** inspect_obj
     *
     * The internal helper method that takes care of the part of the
     * inspection that inspects instance variables.
     */
    private static StringBuilder inspectObj(IRubyObject self, StringBuilder part) {
        ThreadContext context = getRuntime(self).getCurrentContext();
        String sep = "";

        for (Variable<IRubyObject> ivar : getInstanceVariables(self).getInstanceVariableList()) {
            part.append(sep).append(" ").append(ivar.getName()).append("=");
            part.append(invokedynamic(context, ivar.getValue(), INSPECT));
            sep = ",";
        }
        part.append(">");
        return part;
    }

    public static IRubyObject rbClone(IRubyObject self) {
        // TODO: java.lang.Object.clone?
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static boolean isModule(IRubyObject self) {
        return false;
    }

    public static boolean isClass(IRubyObject self) {
        return false;
    }

    public static void dataWrapStruct(IRubyObject self, Object obj) {
    }

    public static Object dataGetStruct(IRubyObject self) {
        return null;
    }

    public static Object dataGetStructChecked(IRubyObject self) {
        return null;
    }

    public static IRubyObject id(IRubyObject self) {
        return getRuntime(self).newFixnum(System.identityHashCode(self));
    }

    public static IRubyObject op_equal(IRubyObject self, ThreadContext context, IRubyObject other) {
        return getRuntime(self).newBoolean(self == other);
    }

    public static IRubyObject op_eqq(IRubyObject self, ThreadContext context, IRubyObject other) {
        return getRuntime(self).newBoolean(self == other);
    }

    public static boolean eql(IRubyObject self, IRubyObject other) {
        return self == other;
    }

    public static void addFinalizer(IRubyObject self, IRubyObject finalizer) {
    }

    public static void removeFinalizers(IRubyObject self) {
    }

    public static boolean hasVariables(IRubyObject self) {
        return false;
    }

    public static int getVariableCount(IRubyObject self) {
        return 0;
    }

    public static void syncVariables(IRubyObject self, List<Variable<Object>> variables) {
    }

    public static List<Variable<Object>> getVariableList(IRubyObject self) {
        return Collections.EMPTY_LIST;
    }

    @SuppressWarnings("deprecation")
    public static class DummyInstanceVariables implements InstanceVariables {
        private final IRubyObject nil;

        public DummyInstanceVariables(IRubyObject nil) {
            this.nil = nil;
        }

        public boolean hasInstanceVariable(String name) {
            return false;
        }

        public boolean fastHasInstanceVariable(String internedName) {
            return false;
        }

        public IRubyObject getInstanceVariable(String name) {
            return nil;
        }

        public IRubyObject fastGetInstanceVariable(String internedName) {
            return nil;
        }

        public IRubyObject setInstanceVariable(String name, IRubyObject value) {
            return value;
        }

        public IRubyObject fastSetInstanceVariable(String internedName, IRubyObject value) {
            return value;
        }

        public IRubyObject removeInstanceVariable(String name) {
            return nil;
        }

        public List<Variable<IRubyObject>> getInstanceVariableList() {
        return Collections.EMPTY_LIST;
        }

        public List<String> getInstanceVariableNameList() {
        return Collections.EMPTY_LIST;
        }

        public void copyInstanceVariablesInto(InstanceVariables other) {
        }

    }

    public static InstanceVariables getInstanceVariables(IRubyObject self) {
        // TODO: cache in runtime?
        return new DummyInstanceVariables(getRuntime(self).getNil());
    }

    @SuppressWarnings("deprecation")
    public static class DummyInternalVariables implements InternalVariables {
        public boolean hasInternalVariable(String name) {
            return false;
        }

        public boolean fastHasInternalVariable(String internedName) {
            return false;
        }

        public Object getInternalVariable(String name) {
            return null;
        }

        public Object fastGetInternalVariable(String internedName) {
            return null;
        }

        public void setInternalVariable(String name, Object value) {
        }

        public void fastSetInternalVariable(String internedName, Object value) {
        }

        public Object removeInternalVariable(String name) {
            return null;
        }
    }
    public static final InternalVariables DUMMY_INTERNAL_VARIABLES = new DummyInternalVariables();

    public static InternalVariables getInternalVariables(IRubyObject self) {
        return DUMMY_INTERNAL_VARIABLES;
    }

    public static List<String> getVariableNameList(IRubyObject self) {
        return Collections.EMPTY_LIST;
    }

    public static void copySpecialInstanceVariables(IRubyObject self, IRubyObject clone) {
    }

    public static Object getVariable(IRubyObject self, int index) {
        return null;
    }

    public static void setVariable(IRubyObject self, int index, Object value) {
    }
}