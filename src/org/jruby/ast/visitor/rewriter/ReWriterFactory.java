package org.jruby.ast.visitor.rewriter;

import org.jruby.ast.visitor.rewriter.utils.DRegxReWriteVisitor;
import org.jruby.ast.visitor.rewriter.utils.HereDocReWriteVisitor;
import org.jruby.ast.visitor.rewriter.utils.IgnoreCommentsReWriteVisitor;
import org.jruby.ast.visitor.rewriter.utils.MultipleAssignmentReWriteVisitor;
import org.jruby.ast.visitor.rewriter.utils.ReWriterContext;
import org.jruby.ast.visitor.rewriter.utils.ShortIfNodeReWriteVisitor;

public class ReWriterFactory {
	
	private ReWriterContext config;

	public ReWriterFactory(ReWriterContext config) {
		this.config = config;
	}
	
	public ReWriteVisitor createShortIfNodeReWriteVisitor() {
		return new ShortIfNodeReWriteVisitor(config);
	}
	
	public ReWriteVisitor createMultipleAssignmentReWriteVisitor() {
		return new MultipleAssignmentReWriteVisitor(config);
	}
	
	public ReWriteVisitor createDRegxReWriteVisitor() {
		return new DRegxReWriteVisitor(config);
	}
	
	public ReWriteVisitor createHereDocReWriteVisitor() {
		return new HereDocReWriteVisitor(config);
	}
	
	public ReWriteVisitor createIgnoreCommentsReWriteVisitor() {
		return new IgnoreCommentsReWriteVisitor(config);
	}
	
	public ReWriteVisitor createReWriteVisitor() {
		return new ReWriteVisitor(config);
	}
}
