package org.jruby.ir.operands;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyInteger;
import org.jruby.RubyString;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.InstanceVariables;
import org.jruby.runtime.builtin.InternalVariables;
import org.jruby.runtime.builtin.Variable;

import java.util.List;

/**
 * For argument processing.  If an opt arg does not exist we will return
 * this so instrs can reason about non-existent arguments.
 *
 * Since this value can be temporarily stored in a binding, we need it to be an IRubyObject as well.
 * But since it can never really participate in any operation, all calls throw a runtime exception.
 */
public class UndefinedValue extends Operand implements IRubyObject {
    public static final UndefinedValue UNDEFINED = new UndefinedValue();

    private UndefinedValue() {}

    @Override
    public void addUsedVariables(List<org.jruby.ir.operands.Variable> l) {
        /* Nothing to do */
    }

    @Override
    public Operand cloneForInlining(InlinerInfo ii) {
        return this;
    }

    @Override
    public boolean canCopyPropagate() {
        return true;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        return this;
    }

    private RuntimeException undefinedOperation() {
        return new RuntimeException("IR compiler/interpreter bug: org.jruby.ir.operands.UndefinedValue should not be used as a valid value during execution.");
    }

    @Deprecated
    public IRubyObject callSuper(ThreadContext context, IRubyObject[] args, Block block) { throw undefinedOperation(); }

    public IRubyObject callMethod(ThreadContext context, String name) { throw undefinedOperation(); }
    public IRubyObject callMethod(ThreadContext context, String name, IRubyObject arg) { throw undefinedOperation(); }
    public IRubyObject callMethod(ThreadContext context, String name, IRubyObject[] args) { throw undefinedOperation(); }
    public IRubyObject callMethod(ThreadContext context, String name, IRubyObject[] args, Block block) { throw undefinedOperation(); }

    @Deprecated
    public IRubyObject callMethod(ThreadContext context, int methodIndex, String name) { throw undefinedOperation(); }
    @Deprecated
    public IRubyObject callMethod(ThreadContext context, int methodIndex, String name, IRubyObject arg) { throw undefinedOperation(); }

    public IRubyObject checkCallMethod(ThreadContext context, String name) { throw undefinedOperation(); }

    public boolean isNil() { throw undefinedOperation(); }

    /**
     *
     * @return
     */
    public boolean isTrue() { throw undefinedOperation(); }

    /**
     * RubyMethod isTaint.
     * @return boolean
     */
    public boolean isTaint() { throw undefinedOperation(); }

    /**
     * RubyMethod setTaint.
     * @param b
     */
    public void setTaint(boolean b) { throw undefinedOperation(); }

    /**
     * Infect this object using the taint of another object
     * @param obj
     * @return
     */
    public IRubyObject infectBy(IRubyObject obj) { throw undefinedOperation(); }

    /**
     * RubyMethod isFrozen.
     * @return boolean
     */
    public boolean isFrozen() { throw undefinedOperation(); }

    /**
     * RubyMethod setFrozen.
     * @param b
     */
    public void setFrozen(boolean b) { throw undefinedOperation(); }

    /**
     * RubyMethod isUntrusted.
     * @return boolean
     */
    public boolean isUntrusted() { throw undefinedOperation(); }

    /**
     * RubyMethod setUntrusted.
     * @param b
     */
    public void setUntrusted(boolean b) { throw undefinedOperation(); }

    /**
     *
     * @return
     */
    public boolean isImmediate() { throw undefinedOperation(); }

    /**
     * RubyMethod getRubyClass.
     * @return
     */
    public RubyClass getMetaClass() { throw undefinedOperation(); }

    /**
     * RubyMethod getSingletonClass.
     * @return RubyClass
     */
    public RubyClass getSingletonClass() { throw undefinedOperation(); }

    /**
     * RubyMethod getType.
     * @return RubyClass
     */
    public RubyClass getType() { throw undefinedOperation(); }

    /**
     * RubyMethod respondsTo.
     * @param string
     * @return boolean
     */
    public boolean respondsTo(String string) { throw undefinedOperation(); }

    /**
     * RubyMethod respondsTo.
     * @param string
     * @return boolean
     */
    public boolean respondsToMissing(String string) { throw undefinedOperation(); }

    /**
     * RubyMethod respondsTo.
     * @param string
     * @return boolean
     */
    public boolean respondsToMissing(String string, boolean priv) { throw undefinedOperation(); }

    /**
     * RubyMethod getRuntime.
     * @return
     */
    public Ruby getRuntime() { throw undefinedOperation(); }

    /**
     * RubyMethod getJavaClass.
     * @return Class
     */
    public Class getJavaClass() { throw undefinedOperation(); }

    /**
     * Convert the object into a symbol name if possible.
     *
     * @return String the symbol name
     */
    public String asJavaString() { throw undefinedOperation(); }

    /** rb_obj_as_string
     * @return
     */
    public RubyString asString() { throw undefinedOperation(); }

    /**
     * Methods which perform to_xxx if the object has such a method
     * @return
     */
    public RubyArray convertToArray() { throw undefinedOperation(); }
    /**
     *
     * @return
     */
    public RubyHash convertToHash() { throw undefinedOperation(); }
    /**
    *
    * @return
    */
    public RubyFloat convertToFloat() { throw undefinedOperation(); }
    /**
     *
     * @return
     */
    public RubyInteger convertToInteger() { throw undefinedOperation(); }
    /**
     *
     * @return
     */
    @Deprecated
    public RubyInteger convertToInteger(int convertMethodIndex, String convertMethod) { throw undefinedOperation(); }
    /**
     *
     * @return
     */
    public RubyInteger convertToInteger(String convertMethod) { throw undefinedOperation(); }
    /**
     *
     * @return
     */
    public RubyString convertToString() { throw undefinedOperation(); }

    /**
     *
     * @return
     */
    public IRubyObject anyToString() { throw undefinedOperation(); }

    /**
     *
     * @return
     */
    public IRubyObject checkStringType() { throw undefinedOperation(); }

    /**
     *
     * @return
     */
    public IRubyObject checkStringType19() { throw undefinedOperation(); }

    /**
     *
     * @return
     */
    public IRubyObject checkArrayType() { throw undefinedOperation(); }

    /**
     * Convert the object to the specified Java class, if possible.
     *
     * @param cls The target type to which the object should be converted.
     */
    public Object toJava(Class cls) { throw undefinedOperation(); }

    /**
     * RubyMethod dup.
     * @return
     */
    public IRubyObject dup() { throw undefinedOperation(); }

    /**
     * RubyMethod inspect.
     * @return String
     */
    public IRubyObject inspect() { throw undefinedOperation(); }

    /**
     * RubyMethod rbClone.
     * @return IRubyObject
     */
    public IRubyObject rbClone() { throw undefinedOperation(); }

    /**
     * @return true if an object is Ruby Module instance (note that it will return false for Ruby Classes).
     * If is_a? semantics is required, use <code>(someObject instanceof RubyModule)</code> instead.
     */
    public boolean isModule() { throw undefinedOperation(); }

    /**
     * @return true if an object is Ruby Class instance (note that it will return false for Ruby singleton classes).
     * If is_a? semantics is required, use <code>(someObject instanceof RubyClass/MetaClass)</code> instead.
     */
    public boolean isClass() { throw undefinedOperation(); }

    /**
     * Our version of Data_Wrap_Struct.
     *
     * This method will just set a private pointer to the object provided. This pointer is transient
     * and will not be accessible from Ruby.
     *
     * @param obj the object to wrap
     */
    public void dataWrapStruct(Object obj) { throw undefinedOperation(); }

    /**
     * Our version of Data_Get_Struct.
     *
     * Returns a wrapped data value if there is one, otherwise returns null.
     *
     * @return the object wrapped.
     */
    public Object dataGetStruct() { throw undefinedOperation(); }
    public Object dataGetStructChecked() { throw undefinedOperation(); }

    /**
     *
     * @return
     */
    public IRubyObject id() { throw undefinedOperation(); }


    public IRubyObject op_equal(ThreadContext context, IRubyObject other) { throw undefinedOperation(); }
    public IRubyObject op_eqq(ThreadContext context, IRubyObject other) { throw undefinedOperation(); }
    public boolean eql(IRubyObject other) { throw undefinedOperation(); }

    public void addFinalizer(IRubyObject finalizer) { throw undefinedOperation(); }

    public void removeFinalizers() { throw undefinedOperation(); }

    //
    // COMMON VARIABLE METHODS
    //

    /**
     * Returns true if object has any variables, defined as:
     * <ul>
     * <li> instance variables
     * <li> class variables
     * <li> constants
     * <li> internal variables, such as those used when marshalling Ranges and Exceptions
     * </ul>
     * @return true if object has any variables, else false
     */
    public boolean hasVariables() { throw undefinedOperation(); }

    /**
     * @return the count of all variables (ivar/cvar/constant/internal)
     */
    public int getVariableCount() { throw undefinedOperation(); }

    /**
     * Sets object's variables to those in the supplied list,
     * removing/replacing any previously defined variables.  Applies
     * to all variable types (ivar/cvar/constant/internal).
     *
     * @param variables the variables to be set for object
     */
    @Deprecated
    public void syncVariables(List<Variable<Object>> variables) { throw undefinedOperation(); }

    /**
     * Sets object's variables to those in the supplied object,
     * removing/replacing any previously defined variables of the same name.
     * Applies to all variable types (ivar/cvar/constant/internal).
     *
     * @param source the source object containing the variables to sync
     */
    public void syncVariables(IRubyObject source) { throw undefinedOperation(); }

    /**
     * @return a list of all variables (ivar/cvar/constant/internal)
     */
    public List<Variable<Object>> getVariableList() { throw undefinedOperation(); }

    //
    // INSTANCE VARIABLE METHODS
    //

    public InstanceVariables getInstanceVariables() { throw undefinedOperation(); }

    //
    // INTERNAL VARIABLE METHODS
    //

    public InternalVariables getInternalVariables() { throw undefinedOperation(); }

    /**
     * @return a list of all variable names (ivar/cvar/constant/internal)
     */
    public List<String> getVariableNameList() { throw undefinedOperation(); }

    public void copySpecialInstanceVariables(IRubyObject clone) { throw undefinedOperation(); }

    public Object getVariable(int index) { throw undefinedOperation(); }
    public void setVariable(int index, Object value) { throw undefinedOperation(); }

    @Override
    public String toString() {
        return "%undefined";
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.UndefinedValue(this);
    }
}
