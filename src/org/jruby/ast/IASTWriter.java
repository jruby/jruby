package org.jruby.ast;

import org.ablaf.ast.INode;

import java.io.Writer;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public interface IASTWriter {
    public void init(Writer writer);

    public void writeAST(INode node);
}