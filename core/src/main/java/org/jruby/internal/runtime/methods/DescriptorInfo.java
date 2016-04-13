package org.jruby.internal.runtime.methods;

import org.jruby.anno.MethodDescriptor;

import java.util.Arrays;
import java.util.List;

/**
 * An aggregate data object based on a collection of MethodDescriptors. Provides information about the collectio
 * of descriptors, such as min and max arguments.
 */
public class DescriptorInfo {
    private int min;
    private int max;
    private boolean frame;
    private boolean scope;
    private boolean rest;
    private boolean block;
    private String parameterDesc;

    private static final boolean RICH_NATIVE_METHOD_PARAMETERS = false;

    public DescriptorInfo(MethodDescriptor... descs) {
        this(Arrays.asList(descs));
    }

    public DescriptorInfo(List<? extends MethodDescriptor> descs) {
        min = Integer.MAX_VALUE;
        max = 0;
        frame = false;
        scope = false;
        rest = false;
        block = false;
        boolean first = true;
        boolean lastBlock = false;

        for (MethodDescriptor desc : descs) {
            // make sure we don't have some methods with blocks and others without
            // the handle generation logic can't handle such cases yet
            if (first) {
                first = false;
            } else {
                if (lastBlock != desc.hasBlock) {
                    throw new RuntimeException("Mismatched block parameters for method " + desc.declaringClassName + "." + desc.name);
                }
            }
            lastBlock = desc.hasBlock;

            int specificArity = -1;
            if (desc.hasVarArgs) {
                if (desc.optional == 0 && !desc.rest && desc.required == 0) {
                    throw new RuntimeException("IRubyObject[] args but neither of optional or rest specified for method " + desc.declaringClassName + "." + desc.name);
                }
                rest = true;
                if (descs.size() == 1) {
                    min = -1;
                }
            } else {
                if (desc.optional == 0 && !desc.rest) {
                    if (desc.required == 0) {
                        // No required specified, check actual number of required args
                        if (desc.actualRequired <= 3) {
                            // actual required is less than 3, so we use specific arity
                            specificArity = desc.actualRequired;
                        } else {
                            // actual required is greater than 3, raise error (we don't support actual required > 3)
                            throw new RuntimeException("Invalid specific-arity number of arguments (" + desc.actualRequired + ") on method " + desc.declaringClassName + "." + desc.name);
                        }
                    } else if (desc.required >= 0 && desc.required <= 3) {
                        if (desc.actualRequired != desc.required) {
                            throw new RuntimeException("Specified required args does not match actual on method " + desc.declaringClassName + "." + desc.name);
                        }
                        specificArity = desc.required;
                    }
                }

                if (specificArity < min) {
                    min = specificArity;
                }

                if (specificArity > max) {
                    max = specificArity;
                }
            }

            if (frame && !desc.anno.frame())
                throw new RuntimeException("Unbalanced frame property on method " + desc.declaringClassName + '.' + desc.name);
            if (scope && !desc.anno.scope())
                throw new RuntimeException("Unbalanced scope property on method " + desc.declaringClassName + '.' + desc.name);
            frame |= desc.anno.frame();
            scope |= desc.anno.scope();
            block |= desc.hasBlock;
        }

        // Core methods currently only show :req's for fixed-arity or a single
        // :rest if it's variable arity. I have filed a bug to improve this
        // (using the skipped logic below, when the time comes) but for now
        // we follow suit. See https://bugs.ruby-lang.org/issues/8088

        StringBuilder descBuilder = new StringBuilder();

        // FIXME: argument type names duplicated from ArgumentType, because it pulls in org.jruby.Ruby
        if (min == max) {
            int i = 0;
            for (; i < min; i++) {
                if (i > 0) descBuilder.append(';');
                descBuilder.append('n');
            }
            // variable arity
        } else if (RICH_NATIVE_METHOD_PARAMETERS) {
            int i = 0;
            for (; i < min; i++) {
                if (i > 0) descBuilder.append(';');
                descBuilder.append('n');
            }

            for (; i < max; i++) {
                if (i > 0) descBuilder.append(';');
                descBuilder.append('O');
            }

            if (rest) {
                if (i > 0) descBuilder.append(';');
                descBuilder.append('R');
            }
        } else {
            descBuilder.append('R');
        }

        parameterDesc = descBuilder.toString();
    }

    @Deprecated
    public boolean isBacktrace() {
        return false;
    }

    public boolean isFrame() {
        return frame;
    }

    public int getMax() {
        return max;
    }

    public int getMin() {
        return min;
    }

    public boolean isScope() {
        return scope;
    }

    public boolean isRest() {
        return rest;
    }

    public boolean isBlock() {
        return block;
    }

    public String getParameterDesc() {
        return parameterDesc;
    }
}
