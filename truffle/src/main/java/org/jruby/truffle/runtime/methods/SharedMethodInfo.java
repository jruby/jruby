/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.methods;

import java.util.ArrayList;
import java.util.List;

import org.jruby.ast.ArgsNode;
import org.jruby.ast.AssignableNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.KeywordArgNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.Node;

import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.LexicalScope;

/**
 * {@link InternalMethod} objects are copied as properties such as visibility are changed. {@link SharedMethodInfo} stores
 * the state that does not change, such as where the method was defined.
 */
public class SharedMethodInfo {

    private final SourceSection sourceSection;
    private final LexicalScope lexicalScope;
    private final String name;
    private final boolean isBlock;
    private final org.jruby.ast.Node parseTree;
    private final boolean alwaysSplit;
    
    private final List<String> keywordArguments;
    private final Arity arity;

    public SharedMethodInfo(SourceSection sourceSection, LexicalScope lexicalScope, String name, boolean isBlock, org.jruby.ast.Node parseTree, boolean alwaysSplit) {
        assert sourceSection != null;
        assert name != null;

        this.sourceSection = sourceSection;
        this.lexicalScope = lexicalScope;
        this.name = name;
        this.isBlock = isBlock;
        this.parseTree = parseTree;
        this.alwaysSplit = alwaysSplit;
        this.keywordArguments = null;
        this.arity = null;
    }
    
	public SharedMethodInfo(SourceSection sourceSection,
			LexicalScope lexicalScope, String name, boolean isBlock,
			org.jruby.ast.Node parseTree, boolean alwaysSplit, ArgsNode argsNode) {
		assert sourceSection != null;
		assert name != null;

		this.sourceSection = sourceSection;
		this.lexicalScope = lexicalScope;
		this.name = name;
		this.isBlock = isBlock;
		this.parseTree = parseTree;
		this.alwaysSplit = alwaysSplit;

		if (argsNode.hasKwargs()) {
			keywordArguments = new ArrayList<String>();
			if (argsNode.getKeywords() != null) {
				for (Node node : argsNode.getKeywords().childNodes()) {
					final KeywordArgNode kwarg = (KeywordArgNode) node;
					final AssignableNode assignableNode = kwarg.getAssignable();

					if (assignableNode instanceof LocalAsgnNode) {
						keywordArguments.add(((LocalAsgnNode) assignableNode)
								.getName());
					} else if (assignableNode instanceof DAsgnNode) {
						keywordArguments.add(((DAsgnNode) assignableNode)
								.getName());
					} else {
						throw new UnsupportedOperationException(
								"unsupported keyword arg " + node);
					}
				}
			}
		} else {
			keywordArguments = null;
		}
		
		this.arity = getArity(argsNode);
	}

	// TODO: copied from MethodTranslator
	private static Arity getArity(org.jruby.ast.ArgsNode argsNode) {
		final int minimum = argsNode.getRequiredArgsCount();
		final int maximum = argsNode.getMaxArgumentsCount();
		return new Arity(minimum, argsNode.getOptionalArgsCount(),
				maximum == -1, argsNode.hasKwargs(), argsNode.hasKeyRest(), argsNode.countKeywords());
	}

	public Arity getArity() {
		return arity;
	}

	public SourceSection getSourceSection() {
        return sourceSection;
    }

    public LexicalScope getLexicalScope() {
        return lexicalScope;
    }

    public String getName() {
        return name;
    }

    public boolean isBlock() {
        return isBlock;
    }

    public org.jruby.ast.Node getParseTree() {
        return parseTree;
    }

    public boolean shouldAlwaysSplit() {
        return alwaysSplit;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();

        if (isBlock) {
            builder.append("block in ");
        }

        builder.append(name);
        builder.append(":");
        builder.append(sourceSection.getShortDescription());

        return builder.toString();
    }
    
	public List<String> getKeywordArguments() {
		return keywordArguments;
	}

}
