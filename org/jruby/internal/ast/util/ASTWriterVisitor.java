package org.jruby.internal.ast.util;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import org.ablaf.ast.INode;
import org.jruby.ast.AbstractNode;
import org.jruby.ast.AliasNode;
import org.jruby.ast.AndNode;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.AttrSetNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BignumNode;
import org.jruby.ast.BlockArgNode;
import org.jruby.ast.BlockNode;
import org.jruby.ast.BlockPassNode;
import org.jruby.ast.BreakNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.CaseNode;
import org.jruby.ast.ClassNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ClassVarDeclNode;
import org.jruby.ast.ClassVarNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.Colon3Node;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.ConstNode;
import org.jruby.ast.DAsgnCurrNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.DRegexpNode;
import org.jruby.ast.DStrNode;
import org.jruby.ast.DVarNode;
import org.jruby.ast.DXStrNode;
import org.jruby.ast.DefinedNode;
import org.jruby.ast.DefnNode;
import org.jruby.ast.DefsNode;
import org.jruby.ast.DotNode;
import org.jruby.ast.EnsureNode;
import org.jruby.ast.EvStrNode;
import org.jruby.ast.ExpandArrayNode;
import org.jruby.ast.FCallNode;
import org.jruby.ast.FalseNode;
import org.jruby.ast.FixnumNode;
import org.jruby.ast.FlipNode;
import org.jruby.ast.FloatNode;
import org.jruby.ast.ForNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.GlobalVarNode;
import org.jruby.ast.HashNode;
import org.jruby.ast.IfNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.LocalVarNode;
import org.jruby.ast.Match2Node;
import org.jruby.ast.Match3Node;
import org.jruby.ast.MatchNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.NextNode;
import org.jruby.ast.NilNode;
import org.jruby.ast.NotNode;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.OpAsgnAndNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpAsgnOrNode;
import org.jruby.ast.OpElementAsgnNode;
import org.jruby.ast.OptNNode;
import org.jruby.ast.OrNode;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.RedoNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.RestArgsNode;
import org.jruby.ast.RetryNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.ScopeNode;
import org.jruby.ast.SelfNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SuperNode;
import org.jruby.ast.SymbolNode;
import org.jruby.ast.TrueNode;
import org.jruby.ast.UndefNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.VAliasNode;
import org.jruby.ast.VCallNode;
import org.jruby.ast.WhenNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.XStrNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.ZArrayNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.ast.types.IListNode;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.util.Asserts;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class ASTWriterVisitor implements NodeVisitor {
    private Writer writer;
    private int indentLevel = 1;

    /**
     * Constructor for ASTWriterVisitor.
     */
    public ASTWriterVisitor(Writer writer) {
        super();
        this.writer = writer;
    }

    private String indent() {
        StringBuffer sb = new StringBuffer(4 * indentLevel);
        for (int i = 0; i < indentLevel; i++) {
            sb.append("    ");
        }
        return sb.toString();
    }

    private void writeBeginTag(String name, AbstractNode node) {
        try {
            writer.write(indent());
            writer.write("<");
            writer.write(name);
            if (node.getPosition() != null) {
                if (node.getPosition().getFile() != null) {
                    writer.write(" file=\"");
                    writer.write(node.getPosition().getFile());
                    writer.write("\"");
                }
                writer.write(" line=\"");
                writer.write(node.getPosition().getLine());
                writer.write("\"");
            }
        } catch (IOException ioExcptn) {
            Asserts.assertNotReached(ioExcptn.getMessage());
        }
    }

    private void writeAttribute(String key, int value) {
        writeAttribute(key, String.valueOf(value));
    }

    private void writeAttribute(String key, char value) {
        writeAttribute(key, String.valueOf(value));
    }

    private void writeAttribute(String key, String value) {
        try {
            writer.write(" ");
            writer.write(key);
            writer.write("=\"");
            writer.write(value);
            writer.write("\"");
        } catch (IOException ioExcptn) {
            Asserts.assertNotReached(ioExcptn.getMessage());
        }
    }

    private void startContent() {
        try {
            writer.write(">\n");
            indentLevel++;
        } catch (IOException ioExcptn) {
            Asserts.assertNotReached(ioExcptn.getMessage());
        }

    }

    private void noContent() {
        try {
            writer.write("/>\n");
        } catch (IOException ioExcptn) {
            Asserts.assertNotReached(ioExcptn.getMessage());
        }

    }

    private void writeEndTag(String name) {
        try {
            indentLevel--;
            writer.write(indent());
            writer.write("</");
            writer.write(name);
            writer.write(">\n");
        } catch (IOException ioExcptn) {
            Asserts.assertNotReached(ioExcptn.getMessage());
        }

    }

    private void writeNode(String name, INode node) {
        try {
            writer.write(indent());
            writer.write("<");
            writer.write(name);
            if (node != null) {
                writer.write(">\n");
                indentLevel++;
                node.accept(this);
                indentLevel--;
                writer.write(indent());
                writer.write("</");
                writer.write(name);
            } else {
                writer.write("/");
            }
            writer.write(">\n");
        } catch (IOException ioExcptn) {
            Asserts.assertNotReached(ioExcptn.getMessage());
        }
    }

    private void writeListNode(IListNode node) {
        try {
            writer.write(indent());
            writer.write("<list");
            if (node != null) {
                writer.write(">\n");
                indentLevel++;
                Iterator iter = node.iterator();
                for (int i = 0, size = node.size(); i < size; i++) {
                    ((INode)iter.next()).accept(this);
                }
                indentLevel--;
                writer.write(indent());
                writer.write("</list");
            } else {
                writer.write("/");
            }
            writer.write(">\n");
        } catch (IOException ioExcptn) {
            Asserts.assertNotReached(ioExcptn.getMessage());
        }
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitAliasNode(AliasNode)
     */
    public void visitAliasNode(AliasNode iVisited) {
        writeBeginTag("alias", iVisited);
        writeAttribute("new", iVisited.getNewName());
        writeAttribute("old", iVisited.getOldName());
        noContent();
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitAndNode(AndNode)
     */
    public void visitAndNode(AndNode iVisited) {
        writeBeginTag("and", iVisited);
        startContent();
        writeNode("first", iVisited.getFirstNode());
        writeNode("second", iVisited.getSecondNode());
        writeEndTag("and");
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitArgsNode(ArgsNode)
     */
    public void visitArgsNode(ArgsNode iVisited) {
        writeBeginTag("args", iVisited);
        writeAttribute("args-count", iVisited.getArgsCount());
        writeAttribute("rest-arg", iVisited.getRestArg());
        startContent();
        writeNode("block-arg", iVisited.getBlockArgNode());
        writeNode("opt-args", iVisited.getOptArgs());
        writeEndTag("args");
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitArrayNode(ArrayNode)
     */
    public void visitArrayNode(ArrayNode iVisited) {
        writeBeginTag("array", iVisited);
        startContent();
        writeListNode(iVisited);
        writeEndTag("array");
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitAttrSetNode(AttrSetNode)
     */
    public void visitAttrSetNode(AttrSetNode iVisited) {
        writeBeginTag("attr-set", iVisited);
        writeAttribute("attribute-name", iVisited.getAttributeName());
        noContent();
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitBackRefNode(BackRefNode)
     */
    public void visitBackRefNode(BackRefNode iVisited) {
        writeBeginTag("back-ref", iVisited);
        writeAttribute("type", iVisited.getType());
        noContent();
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitBeginNode(BeginNode)
     */
    public void visitBeginNode(BeginNode iVisited) {
        writeBeginTag("begin", iVisited);
        startContent();
        writeNode("body", iVisited.getBodyNode());
        writeEndTag("begin");
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitBignumNode(BignumNode)
     */
    public void visitBignumNode(BignumNode iVisited) {
        writeBeginTag("bignum", iVisited);
        writeAttribute("value", iVisited.getValue().toString());
        noContent();
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitBlockArgNode(BlockArgNode)
     */
    public void visitBlockArgNode(BlockArgNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitBlockNode(BlockNode)
     */
    public void visitBlockNode(BlockNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitBlockPassNode(BlockPassNode)
     */
    public void visitBlockPassNode(BlockPassNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitBreakNode(BreakNode)
     */
    public void visitBreakNode(BreakNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitConstDeclNode(ConstDeclNode)
     */
    public void visitConstDeclNode(ConstDeclNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitClassVarAsgnNode(ClassVarAsgnNode)
     */
    public void visitClassVarAsgnNode(ClassVarAsgnNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitClassVarDeclNode(ClassVarDeclNode)
     */
    public void visitClassVarDeclNode(ClassVarDeclNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitClassVarNode(ClassVarNode)
     */
    public void visitClassVarNode(ClassVarNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitCallNode(CallNode)
     */
    public void visitCallNode(CallNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitCaseNode(CaseNode)
     */
    public void visitCaseNode(CaseNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitClassNode(ClassNode)
     */
    public void visitClassNode(ClassNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitColon2Node(Colon2Node)
     */
    public void visitColon2Node(Colon2Node iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitColon3Node(Colon3Node)
     */
    public void visitColon3Node(Colon3Node iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitConstNode(ConstNode)
     */
    public void visitConstNode(ConstNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitDAsgnCurrNode(DAsgnCurrNode)
     */
    public void visitDAsgnCurrNode(DAsgnCurrNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitDAsgnNode(DAsgnNode)
     */
    public void visitDAsgnNode(DAsgnNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitDRegxNode(DRegexpNode)
     */
    public void visitDRegxNode(DRegexpNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitDStrNode(DStrNode)
     */
    public void visitDStrNode(DStrNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitDVarNode(DVarNode)
     */
    public void visitDVarNode(DVarNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitDXStrNode(DXStrNode)
     */
    public void visitDXStrNode(DXStrNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitDefinedNode(DefinedNode)
     */
    public void visitDefinedNode(DefinedNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitDefnNode(DefnNode)
     */
    public void visitDefnNode(DefnNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitDefsNode(DefsNode)
     */
    public void visitDefsNode(DefsNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitDotNode(DotNode)
     */
    public void visitDotNode(DotNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitEnsureNode(EnsureNode)
     */
    public void visitEnsureNode(EnsureNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitEvStrNode(EvStrNode)
     */
    public void visitEvStrNode(EvStrNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitExpandArrayNode(ExpandArrayNode)
     */
    public void visitExpandArrayNode(ExpandArrayNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitFCallNode(FCallNode)
     */
    public void visitFCallNode(FCallNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitFalseNode(FalseNode)
     */
    public void visitFalseNode(FalseNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitFixnumNode(FixnumNode)
     */
    public void visitFixnumNode(FixnumNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitFlipNode(FlipNode)
     */
    public void visitFlipNode(FlipNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitFloatNode(FloatNode)
     */
    public void visitFloatNode(FloatNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitForNode(ForNode)
     */
    public void visitForNode(ForNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitGlobalAsgnNode(GlobalAsgnNode)
     */
    public void visitGlobalAsgnNode(GlobalAsgnNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitGlobalVarNode(GlobalVarNode)
     */
    public void visitGlobalVarNode(GlobalVarNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitHashNode(HashNode)
     */
    public void visitHashNode(HashNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitInstAsgnNode(InstAsgnNode)
     */
    public void visitInstAsgnNode(InstAsgnNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitInstVarNode(InstVarNode)
     */
    public void visitInstVarNode(InstVarNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitIfNode(IfNode)
     */
    public void visitIfNode(IfNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitIterNode(IterNode)
     */
    public void visitIterNode(IterNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitLocalAsgnNode(LocalAsgnNode)
     */
    public void visitLocalAsgnNode(LocalAsgnNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitLocalVarNode(LocalVarNode)
     */
    public void visitLocalVarNode(LocalVarNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitMultipleAsgnNode(MultipleAsgnNode)
     */
    public void visitMultipleAsgnNode(MultipleAsgnNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitMatch2Node(Match2Node)
     */
    public void visitMatch2Node(Match2Node iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitMatch3Node(Match3Node)
     */
    public void visitMatch3Node(Match3Node iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitMatchNode(MatchNode)
     */
    public void visitMatchNode(MatchNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitModuleNode(ModuleNode)
     */
    public void visitModuleNode(ModuleNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitNewlineNode(NewlineNode)
     */
    public void visitNewlineNode(NewlineNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitNextNode(NextNode)
     */
    public void visitNextNode(NextNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitNilNode(NilNode)
     */
    public void visitNilNode(NilNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitNotNode(NotNode)
     */
    public void visitNotNode(NotNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitNthRefNode(NthRefNode)
     */
    public void visitNthRefNode(NthRefNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitOpElementAsgnNode(OpElementAsgnNode)
     */
    public void visitOpElementAsgnNode(OpElementAsgnNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitOpAsgnNode(OpAsgnNode)
     */
    public void visitOpAsgnNode(OpAsgnNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitOpAsgnAndNode(OpAsgnAndNode)
     */
    public void visitOpAsgnAndNode(OpAsgnAndNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitOpAsgnOrNode(OpAsgnOrNode)
     */
    public void visitOpAsgnOrNode(OpAsgnOrNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitOptNNode(OptNNode)
     */
    public void visitOptNNode(OptNNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitOrNode(OrNode)
     */
    public void visitOrNode(OrNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitPostExeNode(PostExeNode)
     */
    public void visitPostExeNode(PostExeNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitRedoNode(RedoNode)
     */
    public void visitRedoNode(RedoNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitRegexpNode(RegexpNode)
     */
    public void visitRegexpNode(RegexpNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitRescueBodyNode(RescueBodyNode)
     */
    public void visitRescueBodyNode(RescueBodyNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitRescueNode(RescueNode)
     */
    public void visitRescueNode(RescueNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitRestArgsNode(RestArgsNode)
     */
    public void visitRestArgsNode(RestArgsNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitRetryNode(RetryNode)
     */
    public void visitRetryNode(RetryNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitReturnNode(ReturnNode)
     */
    public void visitReturnNode(ReturnNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitSClassNode(SClassNode)
     */
    public void visitSClassNode(SClassNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitScopeNode(ScopeNode)
     */
    public void visitScopeNode(ScopeNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitSelfNode(SelfNode)
     */
    public void visitSelfNode(SelfNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitStrNode(StrNode)
     */
    public void visitStrNode(StrNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitSuperNode(SuperNode)
     */
    public void visitSuperNode(SuperNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitSymbolNode(SymbolNode)
     */
    public void visitSymbolNode(SymbolNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitTrueNode(TrueNode)
     */
    public void visitTrueNode(TrueNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitUndefNode(UndefNode)
     */
    public void visitUndefNode(UndefNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitUntilNode(UntilNode)
     */
    public void visitUntilNode(UntilNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitVAliasNode(VAliasNode)
     */
    public void visitVAliasNode(VAliasNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitVCallNode(VCallNode)
     */
    public void visitVCallNode(VCallNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitWhenNode(WhenNode)
     */
    public void visitWhenNode(WhenNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitWhileNode(WhileNode)
     */
    public void visitWhileNode(WhileNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitXStrNode(XStrNode)
     */
    public void visitXStrNode(XStrNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitYieldNode(YieldNode)
     */
    public void visitYieldNode(YieldNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitZArrayNode(ZArrayNode)
     */
    public void visitZArrayNode(ZArrayNode iVisited) {
    }

    /**
     * @see org.jruby.ast.visitor.NodeVisitor#visitZSuperNode(ZSuperNode)
     */
    public void visitZSuperNode(ZSuperNode iVisited) {
    }

}
