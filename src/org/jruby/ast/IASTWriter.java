package org.jruby.ast;

import java.io.Writer;

import org.ablaf.ast.INode;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public interface IASTWriter {
    public void init(Writer writer);

    public void writeAST(INode node);
}