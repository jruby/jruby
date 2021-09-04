package org.jruby.parser;

import org.jruby.Ruby;
import org.jruby.RubySymbol;
import org.jruby.common.IRubyWarnings;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.jruby.util.RubyStringBuilder.str;

/**
 * Each scope while parsing contains extra information as part of the parsing process.
 * This class holds this instead of StaticScope which will outlive parsing.
 */
public class ScopedParserState {
    private ScopedParserState enclosingScope;
    private long commandArgumentStack;
    private long condArgumentStack;
    // A list of booleans indicating which variables are named captures from regexp
    private boolean[] namedCaptures;
    private Map<RubySymbol, Integer> definedVariables;
    private Set<RubySymbol> usedVariables;

    // block scope constructor
    public ScopedParserState(ScopedParserState enclosingScope) {
        this.enclosingScope = enclosingScope;
        this.commandArgumentStack = 0;
        this.condArgumentStack = 0;
    }
    // local scope constructor
    public ScopedParserState(ScopedParserState enclosingScope, long commandArgumentStack, long condArgumentStack) {
        this.enclosingScope = enclosingScope;
        this.commandArgumentStack = commandArgumentStack;
        this.condArgumentStack = condArgumentStack;
    }

    public void setCondArgumentStack(long condArgumentStack) {
        this.condArgumentStack = condArgumentStack;
    }

    public long getCondArgumentStack() {
        return condArgumentStack;
    }

    public void setCommandArgumentStack(long commandArgumentStack) {
        this.commandArgumentStack = commandArgumentStack;
    }

    public long getCommandArgumentStack() {
        return commandArgumentStack;
    }

    public ScopedParserState getEnclosingScope() {
        return enclosingScope;
    }

    public void growNamedCaptures(int index) {
        boolean[] namedCaptures = this.namedCaptures;
        boolean[] newNamedCaptures;
        if (namedCaptures != null) {
            newNamedCaptures = new boolean[Math.max(index + 1, namedCaptures.length)];
            System.arraycopy(namedCaptures, 0, newNamedCaptures, 0, namedCaptures.length);
        } else {
            newNamedCaptures = new boolean[index + 1];
        }
        newNamedCaptures[index] = true;
        this.namedCaptures = newNamedCaptures;
    }

    public boolean isNamedCapture(int index) {
        boolean[] namedCaptures = this.namedCaptures;
        return namedCaptures != null && index < namedCaptures.length && namedCaptures[index];
    }

    public void addDefinedVariable(RubySymbol name, int line) {
        if (definedVariables == null) definedVariables = new HashMap<>();

        definedVariables.put(name, line);
    }

    public void markUsedVariable(RubySymbol name, int depth) {
        if (depth > 0) {
            // If we walk down past an eval script scope we enter the callers lexical scope.  This will
            // have no enclosingScope and also is not something lvar add/use warning does anything with.
            // Only both to traverse deeper is an enclosingScope is present.
            if (enclosingScope != null) enclosingScope.markUsedVariable(name, depth - 1);
            return;
        }

        if (usedVariables == null) usedVariables = new HashSet<>();

        usedVariables.add(name);
    }

    public void warnUnusedVariables(Ruby runtime, IRubyWarnings warnings, String file) {
        if (definedVariables == null) return;

        for(RubySymbol name: definedVariables.keySet()) {
            if (ParserSupport.is_private_local_id(name.getBytes())) continue; // Hidden variable cannot be used.
            if (usedVariables == null || !usedVariables.contains(name)) {
                warnings.warn(IRubyWarnings.ID.AMBIGUOUS_ARGUMENT, file, definedVariables.get(name), str(runtime, "assigned but unused variable - ", name));
            }
        }

    }
}
