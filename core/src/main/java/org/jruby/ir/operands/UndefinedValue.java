package org.jruby.ir.operands;

import org.jruby.*;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.JavaSites;
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

    private UndefinedValue() {
        super();
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.UNDEFINED_VALUE;
    }

    @Override
    public void addUsedVariables(List<org.jruby.ir.operands.Variable> l) {
        /* Nothing to do */
    }

    @Override
    public Operand cloneForInlining(CloneInfo ii) {
        return this;
    }

    @Override
    public boolean canCopyPropagate() {
        return true;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        return this;
    }

    private RuntimeException undefinedOperation() {
        return new RuntimeException("IR compiler/interpreter bug: org.jruby.ir.operands.UndefinedValue should not be used as a valid value during execution.");
    }

    @Override
    @Deprecated(since = "1.7.0")
    public IRubyObject callSuper(ThreadContext context, IRubyObject[] args, Block block) { throw undefinedOperation(); }

    @Override
    public IRubyObject callMethod(ThreadContext context, String name) { throw undefinedOperation(); }
    @Override
    public IRubyObject callMethod(ThreadContext context, String name, IRubyObject arg) { throw undefinedOperation(); }
    @Override
    public IRubyObject callMethod(ThreadContext context, String name, IRubyObject[] args) { throw undefinedOperation(); }
    @Override
    public IRubyObject callMethod(ThreadContext context, String name, IRubyObject[] args, Block block) { throw undefinedOperation(); }

    @Deprecated(since = "1.7.0")
    public IRubyObject callMethod(ThreadContext context, int methodIndex, String name) { throw undefinedOperation(); }
    @Deprecated(since = "1.7.0")
    public IRubyObject callMethod(ThreadContext context, int methodIndex, String name, IRubyObject arg) { throw undefinedOperation(); }

    @Override
    public IRubyObject checkCallMethod(ThreadContext context, String name) { throw undefinedOperation(); }

    @Override
    public IRubyObject checkCallMethod(ThreadContext context, JavaSites.CheckedSites sites) { throw undefinedOperation(); }

    @Override
    public boolean isNil() { throw undefinedOperation(); }

    @Override
    public boolean isTrue() { throw undefinedOperation(); }

    /**
     * RubyMethod isFrozen.
     * @return boolean
     */
    @Override
    public boolean isFrozen() { throw undefinedOperation(); }

    /**
     * RubyMethod setFrozen.
     * @param b
     */
    @Override
    public void setFrozen(boolean b) { throw undefinedOperation(); }

    @Override
    public boolean isImmediate() { throw undefinedOperation(); }

    @Override
    public boolean isSpecialConst() { throw undefinedOperation(); }

    @Override
    public RubyClass getMetaClass() { throw undefinedOperation(); }

    @Override
    public RubyClass getSingletonClass() { throw undefinedOperation(); }

    public RubyClass singletonClass(ThreadContext context) { throw undefinedOperation(); }

    @Override
    public RubyClass getType() { throw undefinedOperation(); }

    @Override
    public boolean respondsTo(String string) { throw undefinedOperation(); }

    @Override
    public boolean respondsToMissing(String string) { throw undefinedOperation(); }

    @Override
    public boolean respondsToMissing(String string, boolean priv) { throw undefinedOperation(); }

    @Override
    public Ruby getRuntime() { throw undefinedOperation(); }

    @Override
    public Class getJavaClass() { throw undefinedOperation(); }

    @Override
    public String asJavaString() { throw undefinedOperation(); }

    @Override
    public RubyString asString() { throw undefinedOperation(); }

    @Override
    public RubyArray convertToArray() { throw undefinedOperation(); }

    @Override
    public RubyHash convertToHash() { throw undefinedOperation(); }

    @Override
    public RubyFloat convertToFloat() { throw undefinedOperation(); }

    @Override
    public RubyInteger convertToInteger() { throw undefinedOperation(); }

    @Override
    public RubyInteger convertToInteger(String convertMethod) { throw undefinedOperation(); }

    @Override
    public RubyString convertToString() { throw undefinedOperation(); }

    @Override
    public IRubyObject anyToString() { throw undefinedOperation(); }

    @Override
    public IRubyObject checkStringType() { throw undefinedOperation(); }

    @Override
    public IRubyObject checkArrayType() { throw undefinedOperation(); }

    /**
     * Convert the object to the specified Java class, if possible.
     *
     * @param cls The target type to which the object should be converted.
     */
    @Override
    public Object toJava(Class cls) { throw undefinedOperation(); }

    @Override
    public IRubyObject dup() { throw undefinedOperation(); }

    @Override
    public IRubyObject inspect() { throw undefinedOperation(); }

    /**
     * RubyMethod rbClone.
     * @return IRubyObject
     */
    @Override
    public IRubyObject rbClone() { throw undefinedOperation(); }

    /**
     * @return true if an object is Ruby Module instance (note that it will return false for Ruby Classes).
     * If is_a? semantics is required, use <code>(someObject instanceof RubyModule)</code> instead.
     */
    @Override
    public boolean isModule() { throw undefinedOperation(); }

    /**
     * @return true if an object is Ruby Class instance (note that it will return false for Ruby singleton classes).
     * If is_a? semantics is required, use <code>(someObject instanceof RubyClass/MetaClass)</code> instead.
     */
    @Override
    public boolean isClass() { throw undefinedOperation(); }

    /**
     * Our version of Data_Wrap_Struct.
     *
     * This method will just set a private pointer to the object provided. This pointer is transient
     * and will not be accessible from Ruby.
     *
     * @param obj the object to wrap
     */
    @Override
    public void dataWrapStruct(Object obj) { throw undefinedOperation(); }

    /**
     * Our version of Data_Get_Struct.
     *
     * Returns a wrapped data value if there is one, otherwise returns null.
     *
     * @return the object wrapped.
     */
    @Override
    public Object dataGetStruct() { throw undefinedOperation(); }

    @Override
    public IRubyObject id() { throw undefinedOperation(); }

    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) { throw undefinedOperation(); }
    @Override
    public IRubyObject op_eqq(ThreadContext context, IRubyObject other) { throw undefinedOperation(); }
    @Override
    public boolean eql(IRubyObject other) { throw undefinedOperation(); }

    @SuppressWarnings("deprecation")
    @Override
    public void addFinalizer(IRubyObject finalizer) { throw undefinedOperation(); }

    @Override
    public IRubyObject addFinalizer(ThreadContext context, IRubyObject finalizer) { throw undefinedOperation(); }

    @Override
    public void removeFinalizers() { throw undefinedOperation(); }

    //
    // COMMON VARIABLE METHODS
    //

    @Override
    public boolean hasVariables() { throw undefinedOperation(); }

    @Override
    public int getVariableCount() { throw undefinedOperation(); }

    @Override
    @Deprecated(since = "1.7.0")
    public void syncVariables(List<Variable<Object>> variables) { throw undefinedOperation(); }

    @Override
    public void syncVariables(IRubyObject source) { throw undefinedOperation(); }

    @Override
    public List<Variable<Object>> getVariableList() { throw undefinedOperation(); }

    //
    // INSTANCE VARIABLE METHODS
    //

    @Override
    public InstanceVariables getInstanceVariables() { throw undefinedOperation(); }

    //
    // INTERNAL VARIABLE METHODS
    //

    @Override
    public InternalVariables getInternalVariables() { throw undefinedOperation(); }

    @Override
    public List<String> getVariableNameList() { throw undefinedOperation(); }

    @Override
    public void copySpecialInstanceVariables(IRubyObject clone) { throw undefinedOperation(); }

    @Override
    public Object getVariable(int index) { throw undefinedOperation(); }
    @Override
    public void setVariable(int index, Object value) { throw undefinedOperation(); }

    @Override
    public String toString() {
        return "%undefined";
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.UndefinedValue(this);
    }

    @Override
    @Deprecated(since = "9.2.0.0")
    public Object dataGetStructChecked() { throw undefinedOperation(); }

    @Override
    @Deprecated(since = "9.4.0.0")
    public boolean isTaint() { throw undefinedOperation(); }

    @Override
    @Deprecated(since = "9.4.0.0")
    public void setTaint(boolean b) { throw undefinedOperation(); }

    @Override
    @Deprecated(since = "9.4.0.0")
    public IRubyObject infectBy(IRubyObject obj) { throw undefinedOperation(); }

    @Override
    @Deprecated(since = "9.4.0.0")
    public boolean isUntrusted() { throw undefinedOperation(); }

    @Override
    @Deprecated(since = "9.4.0.0")
    public void setUntrusted(boolean b) { throw undefinedOperation(); }
}
