package org.jruby.ast.visitor;

import org.ablaf.ast.INode;
import org.ablaf.common.IErrorHandler;
import org.jruby.ast.*;
import org.jruby.common.IErrors;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class UselessStatementVisitor extends AbstractVisitor {
    private IErrorHandler errorHandler;
    
    public UselessStatementVisitor(IErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * @see AbstractVisitor#visitNode(INode)
     */
    protected void visitNode(INode iVisited) {
    }

    private void handleUselessWarn(INode node, String useless) {
        errorHandler.handleError(IErrors.WARN, node.getPosition(), "Useless use of " + useless + "in void context.", null);
    }

    /**
     * @see NodeVisitor#visitCallNode(CallNode)
     */
    public void visitCallNode(CallNode iVisited) {
        String name = iVisited.getName().intern();

        if (name == "+"  || name == "-"  || name == "*"   ||
            name == "/"  || name == "%"  || name == "**"  ||
            name == "+@" || name == "-@" || name == "|"   ||
            name == "^"  || name == "&"  || name == "<=>" ||
            name == ">"  || name == ">=" || name == "<"   ||
            name == "<=" || name == "==" || name == "!=") {

            handleUselessWarn(iVisited, name);
        }
    }
    /**
     * @see NodeVisitor#visitBackRefNode(BackRefNode)
     */
    public void visitBackRefNode(BackRefNode iVisited) {
        handleUselessWarn(iVisited, "a variable");
    }

    /**
     * @see NodeVisitor#visitDVarNode(DVarNode)
     */
    public void visitDVarNode(DVarNode iVisited) {
        handleUselessWarn(iVisited, "a variable");
    }

    /**
     * @see NodeVisitor#visitGlobalVarNode(GlobalVarNode)
     */
    public void visitGlobalVarNode(GlobalVarNode iVisited) {
        handleUselessWarn(iVisited, "a variable");
    }

    /**
     * @see NodeVisitor#visitLocalVarNode(LocalVarNode)
     */
    public void visitLocalVarNode(LocalVarNode iVisited) {
        handleUselessWarn(iVisited, "a variable");
    }

    /**
     * @see NodeVisitor#visitNthRefNode(NthRefNode)
     */
    public void visitNthRefNode(NthRefNode iVisited) {
        handleUselessWarn(iVisited, "a variable");
    }

    /**
     * @see NodeVisitor#visitClassVarNode(ClassVarNode)
     */
    public void visitClassVarNode(ClassVarNode iVisited) {
        handleUselessWarn(iVisited, "a variable");
    }

    /**
     * @see NodeVisitor#visitInstVarNode(InstVarNode)
     */
    public void visitInstVarNode(InstVarNode iVisited) {
        handleUselessWarn(iVisited, "a variable");
    }

    /**
     * @see NodeVisitor#visitConstNode(ConstNode)
     */
    public void visitConstNode(ConstNode iVisited) {
        handleUselessWarn(iVisited, "a constant");
    }

    /**
     * @see NodeVisitor#visitBignumNode(BignumNode)
     */
    public void visitBignumNode(BignumNode iVisited) {
        handleUselessWarn(iVisited, "a literal");
    }

    /**
     * @see NodeVisitor#visitDRegxNode(DRegexpNode)
     */
    public void visitDRegxNode(DRegexpNode iVisited) {
        handleUselessWarn(iVisited, "a literal");
    }

    /**
     * @see NodeVisitor#visitDStrNode(DStrNode)
     */
    public void visitDStrNode(DStrNode iVisited) {
        handleUselessWarn(iVisited, "a literal");
    }

    /**
     * @see NodeVisitor#visitFixnumNode(FixnumNode)
     */
    public void visitFixnumNode(FixnumNode iVisited) {
        handleUselessWarn(iVisited, "a literal");
    }

    /**
     * @see NodeVisitor#visitFloatNode(FloatNode)
     */
    public void visitFloatNode(FloatNode iVisited) {
        handleUselessWarn(iVisited, "a literal");
    }

    /**
     * @see NodeVisitor#visitRegexpNode(RegexpNode)
     */
    public void visitRegexpNode(RegexpNode iVisited) {
        handleUselessWarn(iVisited, "a literal");
    }

    /**
     * @see NodeVisitor#visitStrNode(StrNode)
     */
    public void visitStrNode(StrNode iVisited) {
        handleUselessWarn(iVisited, "a literal");
    }

    /**
     * @see NodeVisitor#visitSymbolNode(SymbolNode)
     */
    public void visitSymbolNode(SymbolNode iVisited) {
        handleUselessWarn(iVisited, "a literal");
    }

	/**
     * @see NodeVisitor#visitClassNode(ClassNode)
     */
    public void visitClassNode(ClassNode iVisited) {
        handleUselessWarn(iVisited, "::");
    }

    /**
     * @see NodeVisitor#visitColon2Node(Colon2Node)
     */
    public void visitColon2Node(Colon2Node iVisited) {
        handleUselessWarn(iVisited, "::");
    }

    /**
     * @see NodeVisitor#visitDotNode(DotNode)
     */
    public void visitDotNode(DotNode iVisited) {
        handleUselessWarn(iVisited, iVisited.isExclusive() ? "..." : "..");
    }
    /**
     * @see NodeVisitor#visitDefinedNode(DefinedNode)
     */
    public void visitDefinedNode(DefinedNode iVisited) {
        handleUselessWarn(iVisited, "defined?");
    }

    /**
     * @see NodeVisitor#visitFalseNode(FalseNode)
     */
    public void visitFalseNode(FalseNode iVisited) {
        handleUselessWarn(iVisited, "false");
    }

    /**
     * @see NodeVisitor#visitNilNode(NilNode)
     */
    public void visitNilNode(NilNode iVisited) {
        handleUselessWarn(iVisited, "nil");
    }

    /**
     * @see NodeVisitor#visitSelfNode(SelfNode)
     */
    public void visitSelfNode(SelfNode iVisited) {
        handleUselessWarn(iVisited, "self");
    }

    /**
     * @see NodeVisitor#visitTrueNode(TrueNode)
     */
    public void visitTrueNode(TrueNode iVisited) {
        handleUselessWarn(iVisited, "true");
    }
    /**
     * @see NodeVisitor#visitNewlineNode(NewlineNode)
     */
    public void visitNewlineNode(NewlineNode iVisited) {
        acceptNode(iVisited.getNextNode());
    }
}