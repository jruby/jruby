/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast.executable;

import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyModule;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import java.math.BigInteger;

/**
 *
 * @author headius
 */
public abstract class AbstractScript implements Script {
    public AbstractScript() {
    }

    public IRubyObject __file__(ThreadContext context, IRubyObject self, Block block) {
        return __file__(context, self, IRubyObject.NULL_ARRAY, block);
    }
    
    public IRubyObject __file__(ThreadContext context, IRubyObject self, IRubyObject arg, Block block) {
        return __file__(context, self, new IRubyObject[] {arg}, block);
    }
    
    public IRubyObject __file__(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
        return __file__(context, self, new IRubyObject[] {arg1, arg2}, block);
    }
    
    public IRubyObject __file__(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        return __file__(context, self, new IRubyObject[] {arg1, arg2, arg3}, block);
    }
    
    @Deprecated
    public IRubyObject load(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        return load(context, self, false);
    }
    
    public IRubyObject load(ThreadContext context, IRubyObject self, boolean wrap) {
        return null;
    }
    
    public IRubyObject run(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        return __file__(context, self, args, block);
    }

    public RuntimeCache runtimeCache;

    public static final int NUMBERED_SCOPE_COUNT = 10;

    public final StaticScope getScope(ThreadContext context, StaticScope parent, String varNamesDescriptor, int i) {return runtimeCache.getScope(context, parent, varNamesDescriptor, i);}
    public final StaticScope getScope0(ThreadContext context, StaticScope parent, String varNamesDescriptor) {return runtimeCache.getScope(context, parent, varNamesDescriptor, 0);}
    public final StaticScope getScope1(ThreadContext context, StaticScope parent, String varNamesDescriptor) {return runtimeCache.getScope(context, parent, varNamesDescriptor, 1);}
    public final StaticScope getScope2(ThreadContext context, StaticScope parent, String varNamesDescriptor) {return runtimeCache.getScope(context, parent, varNamesDescriptor, 2);}
    public final StaticScope getScope3(ThreadContext context, StaticScope parent, String varNamesDescriptor) {return runtimeCache.getScope(context, parent, varNamesDescriptor, 3);}
    public final StaticScope getScope4(ThreadContext context, StaticScope parent, String varNamesDescriptor) {return runtimeCache.getScope(context, parent, varNamesDescriptor, 4);}
    public final StaticScope getScope5(ThreadContext context, StaticScope parent, String varNamesDescriptor) {return runtimeCache.getScope(context, parent, varNamesDescriptor, 5);}
    public final StaticScope getScope6(ThreadContext context, StaticScope parent, String varNamesDescriptor) {return runtimeCache.getScope(context, parent, varNamesDescriptor, 6);}
    public final StaticScope getScope7(ThreadContext context, StaticScope parent, String varNamesDescriptor) {return runtimeCache.getScope(context, parent, varNamesDescriptor, 7);}
    public final StaticScope getScope8(ThreadContext context, StaticScope parent, String varNamesDescriptor) {return runtimeCache.getScope(context, parent, varNamesDescriptor, 8);}
    public final StaticScope getScope9(ThreadContext context, StaticScope parent, String varNamesDescriptor) {return runtimeCache.getScope(context, parent, varNamesDescriptor, 9);}

    public final StaticScope getScope(int i) {return runtimeCache.getScope(i);}
    public final StaticScope getScope0() {return runtimeCache.getScope(0);}
    public final StaticScope getScope1() {return runtimeCache.getScope(1);}
    public final StaticScope getScope2() {return runtimeCache.getScope(2);}
    public final StaticScope getScope3() {return runtimeCache.getScope(3);}
    public final StaticScope getScope4() {return runtimeCache.getScope(4);}
    public final StaticScope getScope5() {return runtimeCache.getScope(5);}
    public final StaticScope getScope6() {return runtimeCache.getScope(6);}
    public final StaticScope getScope7() {return runtimeCache.getScope(7);}
    public final StaticScope getScope8() {return runtimeCache.getScope(8);}
    public final StaticScope getScope9() {return runtimeCache.getScope(9);}

    public static final int NUMBERED_CALLSITE_COUNT = 10;

    public final CallSite getCallSite(int i) {return runtimeCache.callSites[i];}
    public final CallSite getCallSite0() {return runtimeCache.callSites[0];}
    public final CallSite getCallSite1() {return runtimeCache.callSites[1];}
    public final CallSite getCallSite2() {return runtimeCache.callSites[2];}
    public final CallSite getCallSite3() {return runtimeCache.callSites[3];}
    public final CallSite getCallSite4() {return runtimeCache.callSites[4];}
    public final CallSite getCallSite5() {return runtimeCache.callSites[5];}
    public final CallSite getCallSite6() {return runtimeCache.callSites[6];}
    public final CallSite getCallSite7() {return runtimeCache.callSites[7];}
    public final CallSite getCallSite8() {return runtimeCache.callSites[8];}
    public final CallSite getCallSite9() {return runtimeCache.callSites[9];}

    public static final int NUMBERED_SYMBOL_COUNT = 10;

    public final RubySymbol getSymbol(ThreadContext context, int i, String name, String encoding) {return runtimeCache.getSymbol(context, i, name, encoding);}
    public final RubySymbol getSymbol0(ThreadContext context, String name, String encoding) {return runtimeCache.getSymbol(context, 0, name, encoding);}
    public final RubySymbol getSymbol1(ThreadContext context, String name, String encoding) {return runtimeCache.getSymbol(context, 1, name, encoding);}
    public final RubySymbol getSymbol2(ThreadContext context, String name, String encoding) {return runtimeCache.getSymbol(context, 2, name, encoding);}
    public final RubySymbol getSymbol3(ThreadContext context, String name, String encoding) {return runtimeCache.getSymbol(context, 3, name, encoding);}
    public final RubySymbol getSymbol4(ThreadContext context, String name, String encoding) {return runtimeCache.getSymbol(context, 4, name, encoding);}
    public final RubySymbol getSymbol5(ThreadContext context, String name, String encoding) {return runtimeCache.getSymbol(context, 5, name, encoding);}
    public final RubySymbol getSymbol6(ThreadContext context, String name, String encoding) {return runtimeCache.getSymbol(context, 6, name, encoding);}
    public final RubySymbol getSymbol7(ThreadContext context, String name, String encoding) {return runtimeCache.getSymbol(context, 7, name, encoding);}
    public final RubySymbol getSymbol8(ThreadContext context, String name, String encoding) {return runtimeCache.getSymbol(context, 8, name, encoding);}
    public final RubySymbol getSymbol9(ThreadContext context, String name, String encoding) {return runtimeCache.getSymbol(context, 9, name, encoding);}

    public static final int NUMBERED_STRING_COUNT = 10;

    public final RubyString getString(ThreadContext context, int i, int codeRange) {return runtimeCache.getString(context, i, codeRange);}
    public final RubyString getString0(ThreadContext context, int codeRange) {return runtimeCache.getString(context, 0, codeRange);}
    public final RubyString getString1(ThreadContext context, int codeRange) {return runtimeCache.getString(context, 1, codeRange);}
    public final RubyString getString2(ThreadContext context, int codeRange) {return runtimeCache.getString(context, 2, codeRange);}
    public final RubyString getString3(ThreadContext context, int codeRange) {return runtimeCache.getString(context, 3, codeRange);}
    public final RubyString getString4(ThreadContext context, int codeRange) {return runtimeCache.getString(context, 4, codeRange);}
    public final RubyString getString5(ThreadContext context, int codeRange) {return runtimeCache.getString(context, 5, codeRange);}
    public final RubyString getString6(ThreadContext context, int codeRange) {return runtimeCache.getString(context, 6, codeRange);}
    public final RubyString getString7(ThreadContext context, int codeRange) {return runtimeCache.getString(context, 7, codeRange);}
    public final RubyString getString8(ThreadContext context, int codeRange) {return runtimeCache.getString(context, 8, codeRange);}
    public final RubyString getString9(ThreadContext context, int codeRange) {return runtimeCache.getString(context, 9, codeRange);}

    public final RubyString getFrozenString(ThreadContext context, int i, int j, int codeRange) {return runtimeCache.getFrozenString(context, i, j, codeRange);}
    public final RubyString getFrozenString0(ThreadContext context, int j, int codeRange) {return runtimeCache.getFrozenString(context, 0, j, codeRange);}
    public final RubyString getFrozenString1(ThreadContext context, int j, int codeRange) {return runtimeCache.getFrozenString(context, 1, j, codeRange);}
    public final RubyString getFrozenString2(ThreadContext context, int j, int codeRange) {return runtimeCache.getFrozenString(context, 2, j, codeRange);}
    public final RubyString getFrozenString3(ThreadContext context, int j, int codeRange) {return runtimeCache.getFrozenString(context, 3, j, codeRange);}
    public final RubyString getFrozenString4(ThreadContext context, int j, int codeRange) {return runtimeCache.getFrozenString(context, 4, j, codeRange);}
    public final RubyString getFrozenString5(ThreadContext context, int j, int codeRange) {return runtimeCache.getFrozenString(context, 5, j, codeRange);}
    public final RubyString getFrozenString6(ThreadContext context, int j, int codeRange) {return runtimeCache.getFrozenString(context, 6, j, codeRange);}
    public final RubyString getFrozenString7(ThreadContext context, int j, int codeRange) {return runtimeCache.getFrozenString(context, 7, j, codeRange);}
    public final RubyString getFrozenString8(ThreadContext context, int j, int codeRange) {return runtimeCache.getFrozenString(context, 8, j, codeRange);}
    public final RubyString getFrozenString9(ThreadContext context, int j, int codeRange) {return runtimeCache.getFrozenString(context, 9, j, codeRange);}

    public final ByteList getByteList(int i) {return runtimeCache.getByteList(i);}
    public final ByteList getByteList0() {return runtimeCache.getByteList(0);}
    public final ByteList getByteList1() {return runtimeCache.getByteList(1);}
    public final ByteList getByteList2() {return runtimeCache.getByteList(2);}
    public final ByteList getByteList3() {return runtimeCache.getByteList(3);}
    public final ByteList getByteList4() {return runtimeCache.getByteList(4);}
    public final ByteList getByteList5() {return runtimeCache.getByteList(5);}
    public final ByteList getByteList6() {return runtimeCache.getByteList(6);}
    public final ByteList getByteList7() {return runtimeCache.getByteList(7);}
    public final ByteList getByteList8() {return runtimeCache.getByteList(8);}
    public final ByteList getByteList9() {return runtimeCache.getByteList(9);}

    public static final int NUMBERED_ENCODING_COUNT = 10;

    public final Encoding getEncoding(int i) {return runtimeCache.getEncoding(i);}
    public final Encoding getEncoding0() {return runtimeCache.getEncoding(0);}
    public final Encoding getEncoding1() {return runtimeCache.getEncoding(1);}
    public final Encoding getEncoding2() {return runtimeCache.getEncoding(2);}
    public final Encoding getEncoding3() {return runtimeCache.getEncoding(3);}
    public final Encoding getEncoding4() {return runtimeCache.getEncoding(4);}
    public final Encoding getEncoding5() {return runtimeCache.getEncoding(5);}
    public final Encoding getEncoding6() {return runtimeCache.getEncoding(6);}
    public final Encoding getEncoding7() {return runtimeCache.getEncoding(7);}
    public final Encoding getEncoding8() {return runtimeCache.getEncoding(8);}
    public final Encoding getEncoding9() {return runtimeCache.getEncoding(9);}

    public static final int NUMBERED_FIXNUM_COUNT = 10;

    public final RubyFixnum getFixnum(ThreadContext context, int i, int value) {return runtimeCache.getFixnum(context, i, value);}
    public final RubyFixnum getFixnum(ThreadContext context, int i, long value) {return runtimeCache.getFixnum(context, i, value);}
    public final RubyFixnum getFixnum0(ThreadContext context, int value) {return runtimeCache.getFixnum(context, 0, value);}
    public final RubyFixnum getFixnum1(ThreadContext context, int value) {return runtimeCache.getFixnum(context, 1, value);}
    public final RubyFixnum getFixnum2(ThreadContext context, int value) {return runtimeCache.getFixnum(context, 2, value);}
    public final RubyFixnum getFixnum3(ThreadContext context, int value) {return runtimeCache.getFixnum(context, 3, value);}
    public final RubyFixnum getFixnum4(ThreadContext context, int value) {return runtimeCache.getFixnum(context, 4, value);}
    public final RubyFixnum getFixnum5(ThreadContext context, int value) {return runtimeCache.getFixnum(context, 5, value);}
    public final RubyFixnum getFixnum6(ThreadContext context, int value) {return runtimeCache.getFixnum(context, 6, value);}
    public final RubyFixnum getFixnum7(ThreadContext context, int value) {return runtimeCache.getFixnum(context, 7, value);}
    public final RubyFixnum getFixnum8(ThreadContext context, int value) {return runtimeCache.getFixnum(context, 8, value);}
    public final RubyFixnum getFixnum9(ThreadContext context, int value) {return runtimeCache.getFixnum(context, 9, value);}

    public static final int NUMBERED_FLOAT_COUNT = 10;

    public final RubyFloat getFloat(ThreadContext context, int i, double value) {return runtimeCache.getFloat(context, i, value);}
    public final RubyFloat getFloat0(ThreadContext context, double value) {return runtimeCache.getFloat(context, 0, value);}
    public final RubyFloat getFloat1(ThreadContext context, double value) {return runtimeCache.getFloat(context, 1, value);}
    public final RubyFloat getFloat2(ThreadContext context, double value) {return runtimeCache.getFloat(context, 2, value);}
    public final RubyFloat getFloat3(ThreadContext context, double value) {return runtimeCache.getFloat(context, 3, value);}
    public final RubyFloat getFloat4(ThreadContext context, double value) {return runtimeCache.getFloat(context, 4, value);}
    public final RubyFloat getFloat5(ThreadContext context, double value) {return runtimeCache.getFloat(context, 5, value);}
    public final RubyFloat getFloat6(ThreadContext context, double value) {return runtimeCache.getFloat(context, 6, value);}
    public final RubyFloat getFloat7(ThreadContext context, double value) {return runtimeCache.getFloat(context, 7, value);}
    public final RubyFloat getFloat8(ThreadContext context, double value) {return runtimeCache.getFloat(context, 8, value);}
    public final RubyFloat getFloat9(ThreadContext context, double value) {return runtimeCache.getFloat(context, 9, value);}

    public static final int NUMBERED_REGEXP_COUNT = 10;

    public final RubyRegexp getRegexp(ThreadContext context, int i, ByteList pattern, int options) {return runtimeCache.getRegexp(context, i, pattern, options);}
    public final RubyRegexp getRegexp0(ThreadContext context, ByteList pattern, int options) {return runtimeCache.getRegexp(context, 0, pattern, options);}
    public final RubyRegexp getRegexp1(ThreadContext context, ByteList pattern, int options) {return runtimeCache.getRegexp(context, 1, pattern, options);}
    public final RubyRegexp getRegexp2(ThreadContext context, ByteList pattern, int options) {return runtimeCache.getRegexp(context, 2, pattern, options);}
    public final RubyRegexp getRegexp3(ThreadContext context, ByteList pattern, int options) {return runtimeCache.getRegexp(context, 3, pattern, options);}
    public final RubyRegexp getRegexp4(ThreadContext context, ByteList pattern, int options) {return runtimeCache.getRegexp(context, 4, pattern, options);}
    public final RubyRegexp getRegexp5(ThreadContext context, ByteList pattern, int options) {return runtimeCache.getRegexp(context, 5, pattern, options);}
    public final RubyRegexp getRegexp6(ThreadContext context, ByteList pattern, int options) {return runtimeCache.getRegexp(context, 6, pattern, options);}
    public final RubyRegexp getRegexp7(ThreadContext context, ByteList pattern, int options) {return runtimeCache.getRegexp(context, 7, pattern, options);}
    public final RubyRegexp getRegexp8(ThreadContext context, ByteList pattern, int options) {return runtimeCache.getRegexp(context, 8, pattern, options);}
    public final RubyRegexp getRegexp9(ThreadContext context, ByteList pattern, int options) {return runtimeCache.getRegexp(context, 9, pattern, options);}

    public static final int NUMBERED_BIGINTEGER_COUNT = 10;

    public final BigInteger getBigInteger(int i, String name) {return runtimeCache.getBigInteger(i, name);}
    public final BigInteger getBigInteger0(String name) {return runtimeCache.getBigInteger(0, name);}
    public final BigInteger getBigInteger1(String name) {return runtimeCache.getBigInteger(1, name);}
    public final BigInteger getBigInteger2(String name) {return runtimeCache.getBigInteger(2, name);}
    public final BigInteger getBigInteger3(String name) {return runtimeCache.getBigInteger(3, name);}
    public final BigInteger getBigInteger4(String name) {return runtimeCache.getBigInteger(4, name);}
    public final BigInteger getBigInteger5(String name) {return runtimeCache.getBigInteger(5, name);}
    public final BigInteger getBigInteger6(String name) {return runtimeCache.getBigInteger(6, name);}
    public final BigInteger getBigInteger7(String name) {return runtimeCache.getBigInteger(7, name);}
    public final BigInteger getBigInteger8(String name) {return runtimeCache.getBigInteger(8, name);}
    public final BigInteger getBigInteger9(String name) {return runtimeCache.getBigInteger(9, name);}

    public static final int NUMBERED_VARIABLEREADER_COUNT = 10;

    public final IRubyObject getVariable(ThreadContext context, int i, String name, IRubyObject object) {return runtimeCache.getVariable(context, i, name, object);}
    public final IRubyObject getVariable0(ThreadContext context, String name, IRubyObject object) {return runtimeCache.getVariable(context, 0, name, object);}
    public final IRubyObject getVariable1(ThreadContext context, String name, IRubyObject object) {return runtimeCache.getVariable(context, 1, name, object);}
    public final IRubyObject getVariable2(ThreadContext context, String name, IRubyObject object) {return runtimeCache.getVariable(context, 2, name, object);}
    public final IRubyObject getVariable3(ThreadContext context, String name, IRubyObject object) {return runtimeCache.getVariable(context, 3, name, object);}
    public final IRubyObject getVariable4(ThreadContext context, String name, IRubyObject object) {return runtimeCache.getVariable(context, 4, name, object);}
    public final IRubyObject getVariable5(ThreadContext context, String name, IRubyObject object) {return runtimeCache.getVariable(context, 5, name, object);}
    public final IRubyObject getVariable6(ThreadContext context, String name, IRubyObject object) {return runtimeCache.getVariable(context, 6, name, object);}
    public final IRubyObject getVariable7(ThreadContext context, String name, IRubyObject object) {return runtimeCache.getVariable(context, 7, name, object);}
    public final IRubyObject getVariable8(ThreadContext context, String name, IRubyObject object) {return runtimeCache.getVariable(context, 8, name, object);}
    public final IRubyObject getVariable9(ThreadContext context, String name, IRubyObject object) {return runtimeCache.getVariable(context, 9, name, object);}

    public final IRubyObject getVariableDefined(ThreadContext context, int i, String name, IRubyObject object) {return runtimeCache.getVariableDefined(context, i, name, object);}
    public final IRubyObject getVariableDefined0(ThreadContext context, String name, IRubyObject object) {return runtimeCache.getVariableDefined(context, 0, name, object);}
    public final IRubyObject getVariableDefined1(ThreadContext context, String name, IRubyObject object) {return runtimeCache.getVariableDefined(context, 1, name, object);}
    public final IRubyObject getVariableDefined2(ThreadContext context, String name, IRubyObject object) {return runtimeCache.getVariableDefined(context, 2, name, object);}
    public final IRubyObject getVariableDefined3(ThreadContext context, String name, IRubyObject object) {return runtimeCache.getVariableDefined(context, 3, name, object);}
    public final IRubyObject getVariableDefined4(ThreadContext context, String name, IRubyObject object) {return runtimeCache.getVariableDefined(context, 4, name, object);}
    public final IRubyObject getVariableDefined5(ThreadContext context, String name, IRubyObject object) {return runtimeCache.getVariableDefined(context, 5, name, object);}
    public final IRubyObject getVariableDefined6(ThreadContext context, String name, IRubyObject object) {return runtimeCache.getVariableDefined(context, 6, name, object);}
    public final IRubyObject getVariableDefined7(ThreadContext context, String name, IRubyObject object) {return runtimeCache.getVariableDefined(context, 7, name, object);}
    public final IRubyObject getVariableDefined8(ThreadContext context, String name, IRubyObject object) {return runtimeCache.getVariableDefined(context, 8, name, object);}
    public final IRubyObject getVariableDefined9(ThreadContext context, String name, IRubyObject object) {return runtimeCache.getVariableDefined(context, 9, name, object);}

    public static final int NUMBERED_VARIABLEWRITER_COUNT = 10;

    public final IRubyObject setVariable(int i, String name, IRubyObject object, IRubyObject value) {return runtimeCache.setVariable(i, name, object, value);}
    public final IRubyObject setVariable0(String name, IRubyObject object, IRubyObject value) {return runtimeCache.setVariable(0, name, object, value);}
    public final IRubyObject setVariable1(String name, IRubyObject object, IRubyObject value) {return runtimeCache.setVariable(1, name, object, value);}
    public final IRubyObject setVariable2(String name, IRubyObject object, IRubyObject value) {return runtimeCache.setVariable(2, name, object, value);}
    public final IRubyObject setVariable3(String name, IRubyObject object, IRubyObject value) {return runtimeCache.setVariable(3, name, object, value);}
    public final IRubyObject setVariable4(String name, IRubyObject object, IRubyObject value) {return runtimeCache.setVariable(4, name, object, value);}
    public final IRubyObject setVariable5(String name, IRubyObject object, IRubyObject value) {return runtimeCache.setVariable(5, name, object, value);}
    public final IRubyObject setVariable6(String name, IRubyObject object, IRubyObject value) {return runtimeCache.setVariable(6, name, object, value);}
    public final IRubyObject setVariable7(String name, IRubyObject object, IRubyObject value) {return runtimeCache.setVariable(7, name, object, value);}
    public final IRubyObject setVariable8(String name, IRubyObject object, IRubyObject value) {return runtimeCache.setVariable(8, name, object, value);}
    public final IRubyObject setVariable9(String name, IRubyObject object, IRubyObject value) {return runtimeCache.setVariable(9, name, object, value);}

    public static final int NUMBERED_CONSTANT_COUNT = 10;

    public final IRubyObject getConstant(ThreadContext context, StaticScope scope, String name, int i) {return runtimeCache.getConstant(context, scope, name, i);}
    public final IRubyObject getConstant0(ThreadContext context, StaticScope scope, String name) {return runtimeCache.getConstant(context, scope, name, 0);}
    public final IRubyObject getConstant1(ThreadContext context, StaticScope scope, String name) {return runtimeCache.getConstant(context, scope, name, 1);}
    public final IRubyObject getConstant2(ThreadContext context, StaticScope scope, String name) {return runtimeCache.getConstant(context, scope, name, 2);}
    public final IRubyObject getConstant3(ThreadContext context, StaticScope scope, String name) {return runtimeCache.getConstant(context, scope, name, 3);}
    public final IRubyObject getConstant4(ThreadContext context, StaticScope scope, String name) {return runtimeCache.getConstant(context, scope, name, 4);}
    public final IRubyObject getConstant5(ThreadContext context, StaticScope scope, String name) {return runtimeCache.getConstant(context, scope, name, 5);}
    public final IRubyObject getConstant6(ThreadContext context, StaticScope scope, String name) {return runtimeCache.getConstant(context, scope, name, 6);}
    public final IRubyObject getConstant7(ThreadContext context, StaticScope scope, String name) {return runtimeCache.getConstant(context, scope, name, 7);}
    public final IRubyObject getConstant8(ThreadContext context, StaticScope scope, String name) {return runtimeCache.getConstant(context, scope, name, 8);}
    public final IRubyObject getConstant9(ThreadContext context, StaticScope scope, String name) {return runtimeCache.getConstant(context, scope, name, 9);}

    public final IRubyObject getConstantDefined(ThreadContext context, StaticScope scope, String name, int i) {return runtimeCache.getConstantDefined(context, scope, name, i);}
    public final IRubyObject getConstantDefined0(ThreadContext context, StaticScope scope, String name) {return runtimeCache.getConstantDefined(context, scope, name, 0);}
    public final IRubyObject getConstantDefined1(ThreadContext context, StaticScope scope, String name) {return runtimeCache.getConstantDefined(context, scope, name, 1);}
    public final IRubyObject getConstantDefined2(ThreadContext context, StaticScope scope, String name) {return runtimeCache.getConstantDefined(context, scope, name, 2);}
    public final IRubyObject getConstantDefined3(ThreadContext context, StaticScope scope, String name) {return runtimeCache.getConstantDefined(context, scope, name, 3);}
    public final IRubyObject getConstantDefined4(ThreadContext context, StaticScope scope, String name) {return runtimeCache.getConstantDefined(context, scope, name, 4);}
    public final IRubyObject getConstantDefined5(ThreadContext context, StaticScope scope, String name) {return runtimeCache.getConstantDefined(context, scope, name, 5);}
    public final IRubyObject getConstantDefined6(ThreadContext context, StaticScope scope, String name) {return runtimeCache.getConstantDefined(context, scope, name, 6);}
    public final IRubyObject getConstantDefined7(ThreadContext context, StaticScope scope, String name) {return runtimeCache.getConstantDefined(context, scope, name, 7);}
    public final IRubyObject getConstantDefined8(ThreadContext context, StaticScope scope, String name) {return runtimeCache.getConstantDefined(context, scope, name, 8);}
    public final IRubyObject getConstantDefined9(ThreadContext context, StaticScope scope, String name) {return runtimeCache.getConstantDefined(context, scope, name, 9);}

    public static final int NUMBERED_CONSTANTFROM_COUNT = 10;

    public final IRubyObject getConstantFrom(RubyModule target, ThreadContext context, String name, int i) {return runtimeCache.getConstantFrom(target, context, name, i);}
    public final IRubyObject getConstantFrom0(RubyModule target, ThreadContext context, String name) {return runtimeCache.getConstantFrom(target, context, name, 0);}
    public final IRubyObject getConstantFrom1(RubyModule target, ThreadContext context, String name) {return runtimeCache.getConstantFrom(target, context, name, 1);}
    public final IRubyObject getConstantFrom2(RubyModule target, ThreadContext context, String name) {return runtimeCache.getConstantFrom(target, context, name, 2);}
    public final IRubyObject getConstantFrom3(RubyModule target, ThreadContext context, String name) {return runtimeCache.getConstantFrom(target, context, name, 3);}
    public final IRubyObject getConstantFrom4(RubyModule target, ThreadContext context, String name) {return runtimeCache.getConstantFrom(target, context, name, 4);}
    public final IRubyObject getConstantFrom5(RubyModule target, ThreadContext context, String name) {return runtimeCache.getConstantFrom(target, context, name, 5);}
    public final IRubyObject getConstantFrom6(RubyModule target, ThreadContext context, String name) {return runtimeCache.getConstantFrom(target, context, name, 6);}
    public final IRubyObject getConstantFrom7(RubyModule target, ThreadContext context, String name) {return runtimeCache.getConstantFrom(target, context, name, 7);}
    public final IRubyObject getConstantFrom8(RubyModule target, ThreadContext context, String name) {return runtimeCache.getConstantFrom(target, context, name, 8);}
    public final IRubyObject getConstantFrom9(RubyModule target, ThreadContext context, String name) {return runtimeCache.getConstantFrom(target, context, name, 9);}

    public static final int NUMBERED_METHOD_COUNT = 10;

    protected DynamicMethod getMethod(ThreadContext context, IRubyObject self, int i, String methodName) {
        return runtimeCache. getMethod(context, self, i, methodName);
    }
    protected DynamicMethod getMethod0(ThreadContext context, IRubyObject self, String methodName) {
        return runtimeCache. getMethod(context, self, 0, methodName);
    }
    protected DynamicMethod getMethod1(ThreadContext context, IRubyObject self, String methodName) {
        return runtimeCache. getMethod(context, self, 1, methodName);
    }
    protected DynamicMethod getMethod2(ThreadContext context, IRubyObject self, String methodName) {
        return runtimeCache. getMethod(context, self, 2, methodName);
    }
    protected DynamicMethod getMethod3(ThreadContext context, IRubyObject self, String methodName) {
        return runtimeCache. getMethod(context, self, 3, methodName);
    }
    protected DynamicMethod getMethod4(ThreadContext context, IRubyObject self, String methodName) {
        return runtimeCache. getMethod(context, self, 4, methodName);
    }
    protected DynamicMethod getMethod5(ThreadContext context, IRubyObject self, String methodName) {
        return runtimeCache. getMethod(context, self, 5, methodName);
    }
    protected DynamicMethod getMethod6(ThreadContext context, IRubyObject self, String methodName) {
        return runtimeCache. getMethod(context, self, 6, methodName);
    }
    protected DynamicMethod getMethod7(ThreadContext context, IRubyObject self, String methodName) {
        return runtimeCache. getMethod(context, self, 7, methodName);
    }
    protected DynamicMethod getMethod8(ThreadContext context, IRubyObject self, String methodName) {
        return runtimeCache. getMethod(context, self, 8, methodName);
    }
    protected DynamicMethod getMethod9(ThreadContext context, IRubyObject self, String methodName) {
        return runtimeCache. getMethod(context, self, 9, methodName);
    }

    public void setByteList(int index, String str, Encoding encoding) {
        // decode chars back into bytes
        char[] chars = str.toCharArray();
        byte[] bytes = new byte[chars.length];
        for (int i = 0; i < chars.length; i++) {
            bytes[i] = (byte)chars[i];
        }

        runtimeCache.byteLists[index] = new ByteList(bytes, encoding, false);
    }

    public void setEncoding(int index, String encStr) {
        runtimeCache.encodings[index] = EncodingDB.getEncodings().get(encStr.getBytes()).getEncoding();
    }

    public static CallSite[] setCallSite(CallSite[] callSites, int index, String name) {
        callSites[index] = MethodIndex.getCallSite(name);
        return callSites;
    }

    public static CallSite[] setFunctionalCallSite(CallSite[] callSites, int index, String name) {
        callSites[index] = MethodIndex.getFunctionalCallSite(name);
        return callSites;
    }

    public static CallSite[] setVariableCallSite(CallSite[] callSites, int index, String name) {
        callSites[index] = MethodIndex.getVariableCallSite(name);
        return callSites;
    }

    public static CallSite[] setSuperCallSite(CallSite[] callSites, int index) {
        callSites[index] = MethodIndex.getSuperCallSite();
        return callSites;
    }

    public final void setFilename(String filename) {
        this.filename = filename;
    }

    public final void initFromDescriptor(String descriptor) {
        runtimeCache.initFromDescriptor(descriptor);
    }

    public void setRootScope(StaticScope scope) {
        runtimeCache.scopes[0] = scope;
    }

    protected String filename;
}
