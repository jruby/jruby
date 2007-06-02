package org.jruby;

import java.util.Iterator;
import java.util.Map;

import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyUndef implements IRubyObject {

    public void addFinalizer(RubyProc finalizer) {
    }

    public IRubyObject anyToString() {
        return null;
    }

    public RubyString asString() {
        return null;
    }

    public String asSymbol() {
        return null;
    }

    public IRubyObject callMethod(ThreadContext context, RubyModule rubyclass, String name,
            IRubyObject[] args, CallType callType, Block block) {
        return null;
    }

    public IRubyObject callMethod(ThreadContext context, RubyModule rubyclass, int methodIndex,
            String name, IRubyObject[] args, CallType callType, Block block) {
        return null;
    }

    public IRubyObject callMethod(ThreadContext context, int methodIndex, String name,
            IRubyObject arg) {
        return null;
    }

    public IRubyObject callMethod(ThreadContext context, int methodIndex, String name,
            IRubyObject[] args) {
        return null;
    }

    public IRubyObject callMethod(ThreadContext context, int methodIndex, String name,
            IRubyObject[] args, CallType callType) {
        return null;
    }

    public IRubyObject callMethod(ThreadContext context, String name, IRubyObject[] args,
            CallType callType) {
        return null;
    }

    public IRubyObject callMethod(ThreadContext context, String name, IRubyObject[] args,
            CallType callType, Block block) {
        return null;
    }

    public IRubyObject callMethod(ThreadContext context, String string) {
        return null;
    }

    public IRubyObject callMethod(ThreadContext context, int methodIndex, String string) {
        return null;
    }

    public IRubyObject callMethod(ThreadContext context, String string, Block aBlock) {
        return null;
    }

    public IRubyObject callMethod(ThreadContext context, String string, IRubyObject arg) {
        return null;
    }

    public IRubyObject callMethod(ThreadContext context, String method, IRubyObject[] rubyArgs) {
        return null;
    }

    public IRubyObject callMethod(ThreadContext context, String method, IRubyObject[] rubyArgs,
            Block block) {
        return null;
    }

    public IRubyObject callSuper(ThreadContext context, IRubyObject[] args, Block block) {
        return null;
    }

    public IRubyObject checkArrayType() {
        return null;
    }

    public IRubyObject checkStringType() {
        return null;
    }

    public IRubyObject compilerCallMethod(ThreadContext context, String name, IRubyObject[] args,
            IRubyObject caller, CallType callType, Block block) {
        return null;
    }

    public IRubyObject compilerCallMethodWithIndex(ThreadContext context, int methodIndex,
            String name, IRubyObject[] args, IRubyObject caller, CallType callType, Block block) {
        return null;
    }

    public RubyArray convertToArray() {
        return null;
    }

    public RubyFloat convertToFloat() {
        return null;
    }

    public RubyHash convertToHash() {
        return null;
    }

    public RubyInteger convertToInteger() {
        return null;
    }

    public RubyString convertToString() {
        return null;
    }

    public IRubyObject convertToType(RubyClass targetType, int convertMethodIndex,
            String convertMethod, boolean raiseOnError) {
        return null;
    }

    public IRubyObject convertToType(RubyClass targetType, int convertMethodIndex,
            String convertMethod, boolean raiseOnMissingMethod, boolean raiseOnWrongTypeResult,
            boolean allowNilThrough) {
        return null;
    }

    public IRubyObject convertToTypeWithCheck(RubyClass targetType, int convertMethodIndex,
            String convertMethod) {
        return null;
    }

    public Object dataGetStruct() {
        return null;
    }

    public void dataWrapStruct(Object obj) {
    }

    public IRubyObject dup() {
        return null;
    }

    public boolean eql(IRubyObject other) {
        return false;
    }

    public boolean eqlInternal(ThreadContext context, IRubyObject other) {
        return false;
    }

    public IRubyObject equal(IRubyObject other) {
        return null;
    }

    public IRubyObject equalInternal(ThreadContext context, IRubyObject other) {
        return null;
    }

    public IRubyObject evalSimple(ThreadContext context, IRubyObject evalString, String file) {
        return null;
    }

    public IRubyObject evalWithBinding(ThreadContext context, IRubyObject evalString,
            IRubyObject binding, String file, int lineNumber) {
        return null;
    }

    public IRubyObject getInstanceVariable(String string) {
        return null;
    }

    public Map getInstanceVariables() {
        return null;
    }

    public Map getInstanceVariablesSnapshot() {
        return null;
    }

    public Class getJavaClass() {
        return null;
    }

    public RubyClass getMetaClass() {
        throw new RuntimeException("Undef is not a real RubyObject and should never be seen");
    }

    public int getNativeTypeIndex() {
        return 0;
    }

    public Ruby getRuntime() {
        return null;
    }

    public RubyClass getSingletonClass() {
        return null;
    }

    public RubyClass getType() {
        return null;
    }

    public RubyFixnum id() {
        return null;
    }

    public IRubyObject infectBy(IRubyObject obj) {
        return null;
    }

    public IRubyObject inspect() {
        return null;
    }

    public Iterator instanceVariableNames() {
        return null;
    }

    public boolean isFrozen() {
        return false;
    }

    public boolean isImmediate() {
        return false;
    }

    public boolean isKindOf(RubyModule rubyClass) {
        return false;
    }

    public boolean isNil() {
        return false;
    }

    public boolean isSingleton() {
        return false;
    }

    public boolean isTaint() {
        return false;
    }

    public boolean isTrue() {
        return false;
    }

    public IRubyObject rbClone(Block unusedBlock) {
        return null;
    }

    public void removeFinalizers() {
    }

    public boolean respondsTo(String string) {
        return false;
    }

    public Map safeGetInstanceVariables() {
        return null;
    }

    public boolean safeHasInstanceVariables() {
        return false;
    }

    public void setFrozen(boolean b) {
    }

    public IRubyObject setInstanceVariable(String string, IRubyObject rubyObject) {
        return null;
    }

    public void setInstanceVariables(Map instanceVariables) {
    }

    public void setMetaClass(RubyClass metaClass) {
    }

    public void setTaint(boolean b) {
    }
}
