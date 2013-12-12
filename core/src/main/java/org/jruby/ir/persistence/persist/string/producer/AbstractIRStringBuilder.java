package org.jruby.ir.persistence.persist.string.producer;

import org.jruby.ir.IRScope;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.persist.string.IRToStringTranslator;
import org.jruby.parser.IRStaticScope;

public abstract class AbstractIRStringBuilder<T> {
    
    private static final String DOUBLE_QUOTES = "\"";
    private static final String ESCAPED_DOUBLE_QUOTES = "\\\\\"";
    
    private static final String ARRAY_START_MARKER = "[";
    private static final String ARRAY_END_MARKER = "]";
    
    private final String PARAMETER_LIST_START_MARKER = getParameterListStartMarker();
    private final String PARAMETER_SEPARATOR = getParameterSeparator();
    private final String PARAMETER_LIST_END_MARKER = getParameterListEndMarker();

    protected final StringBuilder builder;

    // Take StringBuilder from parent or create it if there is no parent
    AbstractIRStringBuilder(StringBuilder constructedBuilder) {
        if (constructedBuilder == null) {
            builder = new StringBuilder();
        } else {
            builder = constructedBuilder;
        }
    }

    abstract String getParameterListStartMarker();

    abstract String getParameterSeparator();

    abstract String getParameterListEndMarker();

    public void appendParameters(Object... parameters) {
        builder.append(PARAMETER_LIST_START_MARKER);

        for (int i = 0; i < parameters.length; i++) {
            if (i != 0) builder.append(PARAMETER_SEPARATOR);

            appendParameter(parameters[i]);
        }

        builder.append(PARAMETER_LIST_END_MARKER);
    }

    private void appendParameter(Object parameter) {
        
        // We need these ugly instanceof's because the choice of
        // which overloading to invoke is made at compile time
        // so we can't simply write appendParameter(String string) etc.
        if (parameter instanceof Operand) {
            appendOperandParameter((Operand) parameter);

        } else if (parameter instanceof String) {
            appendEscapedString((String) parameter);

        } else if (parameter instanceof Number) {
            appendParameterWithoutModifications(parameter);
        } else if (parameter instanceof Boolean) {
            appendParameterWithoutModifications(parameter);
        } else if (parameter instanceof Object[]) {
            appendArrayParameter((Object[]) parameter);

        } else if (parameter instanceof IRScope) {
            appendIRScopeParameter((IRScope) parameter);

        } else if (parameter instanceof IRStaticScope) {
            appendStaticScopeParameter((IRStaticScope) parameter);
            
        } else if (parameter == null) {
            builder.append(parameter);
            
        } else {
            appendOtherParameter(parameter);

        }
    }

    private void appendOperandParameter(Operand operand) {
        IRToStringTranslator.continueTranslation(builder, operand);
    }

    void appendEscapedString(String string) {
        String escapedStringValue = string.replaceAll(DOUBLE_QUOTES, ESCAPED_DOUBLE_QUOTES);
        builder.append(DOUBLE_QUOTES).append(escapedStringValue).append(DOUBLE_QUOTES);
    }
    
    private void appendParameterWithoutModifications(Object parameter) {
        builder.append(parameter);
    }
    
    private void appendArrayParameter(Object[] parameters) {
        builder.append(ARRAY_START_MARKER);
        
        for (int i = 0; i < parameters.length; i++) {
            if (i != 0) builder.append(PARAMETER_SEPARATOR);
            
            appendParameter(parameters[i]);
        }
        
        builder.append(ARRAY_END_MARKER);
    }

    private void appendIRScopeParameter(IRScope scope) {
        appendEscapedString(scope.getName()+":"+scope.getLineNumber());
    }
    
    private void appendStaticScopeParameter(IRStaticScope staticScope) {
        appendOtherParameter(staticScope.getType());
        builder.append(PARAMETER_SEPARATOR);
        appendArrayParameter(staticScope.getVariables());
        builder.append(PARAMETER_SEPARATOR);
        appendParameterWithoutModifications(staticScope.getRequiredArgs());
    }

    private void appendOtherParameter(Object parameter) {
        appendEscapedString(parameter.toString());
    }
    
    public String getResultString() {
        return builder.toString();
    }
}

