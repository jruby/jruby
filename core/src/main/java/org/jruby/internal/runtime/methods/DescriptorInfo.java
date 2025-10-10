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
    // Will this method be able to potentially process or forward keywords.  Gives access to ThreadContext#callInfo.
    private boolean keywords;
    private boolean frame;
    private boolean rest;
    private boolean block;
    private String parameterDesc;

    // These are sourced from here because ArgumentType references Ruby, which can't load during invoker generation.
    public static final char ANONREQ_CHAR = 'n';
    public static final char ANONOPT_CHAR = 'O';
    public static final char ANONREST_CHAR = 'R';

    private static final boolean RICH_NATIVE_METHOD_PARAMETERS = false;

    public DescriptorInfo(MethodDescriptor... descs) {
        this(Arrays.asList(descs));
    }

    public DescriptorInfo(List<? extends MethodDescriptor> descs) {
        min = Integer.MAX_VALUE;
        max = 0;
        keywords = false;
        frame = false;
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
            keywords |= desc.anno.keywords();
            frame |= desc.anno.frame();
            block |= desc.hasBlock;
        }

        // Core methods currently only show :req's for fixed-arity or a single
        // :rest if it's variable arity. I have filed a bug to improve this
        // (using the skipped logic below, when the time comes) but for now
        // we follow suit. See https://bugs.ruby-lang.org/issues/8088

        StringBuilder descBuilder = new StringBuilder();

        // FIXME: argument type names duplicated from ArgumentType, because it pulls in org.jruby.Ruby
        if (RICH_NATIVE_METHOD_PARAMETERS) {
            int i = 0;
            for (; i < min; i++) {
                if (i > 0) descBuilder.append(';');
                descBuilder.append(ANONREQ_CHAR);
            }

            for (; i < max; i++) {
                if (i > 0) descBuilder.append(';');
                descBuilder.append(ANONOPT_CHAR);
            }

            if (rest) {
                if (i > 0) descBuilder.append(';');
                descBuilder.append(ANONOPT_CHAR);
            }
        } else {
            if (rest || min != max) {
                descBuilder.append(ANONREST_CHAR);
            } else {
                int i = 0;
                for (; i < min; i++) {
                    if (i > 0) descBuilder.append(';');
                    descBuilder.append(ANONREQ_CHAR);
                }
            }
        }

        parameterDesc = descBuilder.toString();
    }

    @Deprecated(since = "9.0.5.0")
    public boolean isBacktrace() {
        return false;
    }

    public boolean acceptsKeywords() {
        return keywords;
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
