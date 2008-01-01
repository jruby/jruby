package org.jruby.ast.visitor.rewriter;

import java.util.Iterator;

import org.jruby.ast.BlockNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.Node;
import org.jruby.ast.visitor.rewriter.utils.ReWriterContext;


public class ClassBodyWriter {
	private ReWriteVisitor visitor;
	private Node bodyNode;
	private ReWriterContext context;

	public ClassBodyWriter(ReWriteVisitor visitor, Node bodyNode) {
		this.visitor = visitor;
		this.bodyNode = bodyNode;
		this.context = visitor.getConfig();
	}
	
	public void write(){
		if (bodyNode instanceof BlockNode) {
			context.getIndentor().indent();
			writeContent((BlockNode) bodyNode);
			context.getIndentor().outdent();
		} else if (bodyNode instanceof NewlineNode) {
			visitor.visitNodeInIndentation(bodyNode);
		} else {
			visitor.visitNode(bodyNode);
		}
	}
	
	private void writeContent(BlockNode node) {
		for (Iterator<Node> it = node.childNodes().iterator(); it.hasNext(); ) {
			visitor.visitNode(it.next());
            
			if (it.hasNext()) {
				context.getOutput().print(context.getFormatHelper().classBodyElementsSeparator());
            }
		}
	}
}
