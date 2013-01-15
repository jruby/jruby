/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.runtime.invokedynamic;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Signature represents a series of method arguments plus their symbolic names.
 * 
 * In order to make it easier to permute arguments, track their flow, and debug
 * cases where reordering or permuting fails to work properly, the Signature
 * class also tracks symbolic names for all arguments. This allows permuting
 * by name or by name pattern, avoiding the error-prone juggling of int[] for
 * the standard MethodHandles.permuteArguments call.
 * 
 * A Signature is created starting using #thatReturns method, and expanded using
 * #withArgument for each named argument in sequence. Order is preserved.
 * 
 * A Signature can be mutated into another by manipuating the argument list as
 * with java.lang.invoke.MethodType, but using argument names and name patterns
 * instead of integer offsets.
 * 
 * Two signatures can be used to produce a permute array suitable for use in
 * java.lang.invoke.MethodHandles#permuteArguments using the #to methods. The
 * #to method can also accept a list of argument names, as a shortcut.
 * 
 * @author headius
 */
public class Signature {
    private final MethodType methodType;
    private final String[] argNames;

    Signature(Class retval) {
        this(MethodType.methodType(retval));
    }

    Signature(Class retval, Class[] argTypes, String... argNames) {
        this(MethodType.methodType(retval, argTypes), argNames);
    }

    Signature(MethodType methodType, String... argNames) {
        assert methodType.parameterCount() == argNames.length : "arg name count " + argNames.length + " does not match parameter count " + methodType.parameterCount();
        this.methodType = methodType;
        this.argNames = argNames;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < argNames.length; i++) {
            sb.append(methodType.parameterType(i).getSimpleName()).append(' ').append(argNames[i]);
            if (i + 1 < argNames.length) {
                sb.append(", ");
            }
        }
        sb.append(")").append(methodType.returnType().getSimpleName());
        return sb.toString();
    }

    public static Signature returning(Class retval) {
        Signature sig = new Signature(retval);
        return sig;
    }

    public Signature asFold(Class retval) {
        return new Signature(methodType.changeReturnType(retval), argNames);
    }

    public Signature appendArg(String name, Class type) {
        String[] newArgNames = new String[argNames.length + 1];
        System.arraycopy(argNames, 0, newArgNames, 0, argNames.length);
        newArgNames[argNames.length] = name;
        MethodType newMethodType = methodType.appendParameterTypes(type);
        return new Signature(newMethodType, newArgNames);
    }
    
    public Signature prependArg(String name, Class type) {
        String[] newArgNames = new String[argNames.length + 1];
        System.arraycopy(argNames, 0, newArgNames, 1, argNames.length);
        newArgNames[0] = name;
        MethodType newMethodType = methodType.insertParameterTypes(0, type);
        return new Signature(newMethodType, newArgNames);
    }
    
    public Signature insertArg(int index, String name, Class type) {
        return insertArgs(index, new String[]{name}, new Class[]{type});
    }
    
    public Signature insertArgs(int index, String[] names, Class[] types) {
        assert names.length == types.length : "names and types must be of the same length";
        
        String[] newArgNames = new String[argNames.length + names.length];
        System.arraycopy(names, 0, newArgNames, index, names.length);
        if (index != 0) System.arraycopy(argNames, 0, newArgNames, 0, index);
        if (argNames.length - index != 0) System.arraycopy(argNames, index, newArgNames, index + names.length, argNames.length - index);
        
        MethodType newMethodType = methodType.insertParameterTypes(0, types);
        
        return new Signature(newMethodType, newArgNames);
    }

    public MethodType methodType() {
        return methodType;
    }

    public String[] argNames() {
        return argNames;
    }

    public Signature permute(String... permuteArgs) {
        List<Class> types = new ArrayList<Class>(argNames.length);
        List<String> names = new ArrayList<String>(argNames.length);
        for (String permuteArg : permuteArgs) {
            Pattern pattern = Pattern.compile(permuteArg);
            boolean found = false;
            for (int argOffset = 0; argOffset < argNames.length; argOffset++) {
                String arg = argNames[argOffset];
                if (pattern.matcher(arg).find()) {
                    found = true;
                    types.add(methodType.parameterType(argOffset));
                    names.add(argNames[argOffset]);
                }
            }
        }
        return new Signature(MethodType.methodType(methodType.returnType(), types.toArray(new Class[0])), names.toArray(new String[0]));
    }
    
    public MethodHandle permuteTo(MethodHandle target, String... permuteArgs) {
        return MethodHandles.permuteArguments(target, methodType, to(permute(permuteArgs)));
    }

    public int[] to(Signature other) {
        return nonMatchingTo(other.argNames);
    }

    public int[] to(String... otherArgPatterns) {
        return to(permute(otherArgPatterns));
    }

    public int[] nonMatchingTo(String... otherArgNames) {
        int[] offsets = new int[otherArgNames.length];
        int i = 0;
        for (String arg : otherArgNames) {
            int pos = -1;
            for (int offset = 0; offset < argNames.length; offset++) {
                if (argNames[offset].equals(arg)) {
                    pos = offset;
                    break;
                }
            }
            assert pos >= 0 : "argument not found: \"" + arg + "\"";
            offsets[i++] = pos;
        }
        return offsets;
    }
    
}
