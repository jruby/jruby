package org.jruby.internal.ast.util;

import java.io.IOException;
import java.io.Writer;

import org.ablaf.ast.INode;
import org.jruby.ast.IASTWriter;
import org.jruby.util.Asserts;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class ASTWriter implements IASTWriter {
    private Writer writer;

    /**
     * Constructor for ASTWriter.
     */
    public ASTWriter() {
        super();
    }

    /**
     * @see org.jruby.ast.IASTWriter#init(Writer)
     */
    public void init(Writer writer) {
        Asserts.assertNotNull(writer, "writer must not be null");
        this.writer = writer;
    }

    /**
     * @see org.jruby.ast.IASTWriter#writeAST(INode)
     */
    public void writeAST(INode node) {
        Asserts.assertNotNull(writer, "not initialized");

        try {
            writer.write("<?xml version=\"1.0\"?>\n");
            writer.write("\n");
            writer.write("<ast lang=\"ruby\">\n");

            node.accept(new ASTWriterVisitor(writer));

            writer.write("</ast>\n");
        } catch (IOException ioExcptn) {
            Asserts.assertNotReached(ioExcptn.getMessage());
        }
    }
}