package org.jruby.ast.util;

import java.io.File;
import org.jruby.ast.AliasNode;
import org.jruby.ast.ArgumentNode;
import org.jruby.ast.AttrAssignNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BignumNode;
import org.jruby.ast.BlockArgNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ClassVarDeclNode;
import org.jruby.ast.ClassVarNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.Colon3Node;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.ConstNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.DRegexpNode;
import org.jruby.ast.DVarNode;
import org.jruby.ast.DotNode;
import org.jruby.ast.FCallNode;
import org.jruby.ast.FileNode;
import org.jruby.ast.FixnumNode;
import org.jruby.ast.FlipNode;
import org.jruby.ast.FloatNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.GlobalVarNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.LocalVarNode;
import org.jruby.ast.Node;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpElementAsgnNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SymbolNode;
import org.jruby.ast.UndefNode;
import org.jruby.ast.VAliasNode;
import org.jruby.ast.VCallNode;
import org.jruby.ast.XStrNode;

public class SexpMaker {
    private final StringBuilder sb;
    
    public static String create(Node node) {
        SexpMaker maker = new SexpMaker();
        
        maker.process(node);
        
        return maker.toString();
    }
    
    public static String create(String methodName, Node argsNode, Node body) {
        SexpMaker maker = new SexpMaker();
        
        maker.processMethod(methodName, argsNode, body);
        
        return maker.toString();
    }
    
    private SexpMaker() {
        sb = new StringBuilder();
    }
    
    private void processMethod(String methodName, Node argsNode, Node body) {
        sb.append("(method ").append(methodName).append(' ');
        // JRUBY-4301, include filename and line in sexp
        sb.append("(file ").append(new File(body.getPosition().getFile()).getName()).append(") ");
        sb.append("(line ").append(body.getPosition().getStartLine()).append(") ");
        process(argsNode);
        sb.append(" ");
        process(body);
        sb.append(")");
    }
    
    @Override
    public String toString() {
        return sb.toString();
    }

    /**
     * process each node by printing out '(' name data child* ')'
     * @param node
     */
    private void process(Node node) {
        if (node == null) {
            sb.append("null");
            return;
        }
        
        sb.append("(");
        shortName(node);
        leafInfo(node);
        
        for (Node child: node.childNodes()) {
            sb.append(" ");
            process(child);
        }
        
        sb.append(")");
    }

    private void shortName(Node node) {
        String className = node.getClass().getName();
        
        if (className.endsWith("Node")) {
            className = className.substring(0, className.length() - 4);
            int index = className.lastIndexOf('.');
            
            if (index != -1) {
                className = className.substring(index+1);
            }
        }
        
        sb.append(className.toLowerCase());
    }

    /**
     * Extra information that is not child nodes, but actual leaf data.
     *
     * @param node
     */
    private void leafInfo(Node node) {
        switch (node.getNodeType()) {
        case ALIASNODE: aliasNode((AliasNode) node); break;
        case ANDNODE: noDataContents(node); break;
        case ARGSCATNODE: noDataContents(node); break;
        case ARGSPUSHNODE: noDataContents(node); break;
        case ARGUMENTNODE: argumentNode((ArgumentNode) node); break;
        case ARRAYNODE: noDataContents(node); break;
        case ATTRASSIGNNODE: attrAssignNode((AttrAssignNode) node); break;
        case BACKREFNODE: backRefNode((BackRefNode) node); break;
        case BEGINNODE: noDataContents(node); break;
        case BIGNUMNODE: bignumNode((BignumNode) node); break;
        case BLOCKARGNODE: blockArgNode((BlockArgNode) node); break;
        case BLOCKNODE: noDataContents(node); break;
        case BLOCKPASSNODE: noDataContents(node); break;
        case BREAKNODE: noDataContents(node); break;
        case CALLNODE: callNode((CallNode) node); break;
        case CASENODE: noDataContents(node); break;
        case CLASSNODE: noDataContents(node); break;
        case CLASSVARASGNNODE: classVarAsgnNode((ClassVarAsgnNode) node); break;
        case CLASSVARDECLNODE: classVarDeclNode((ClassVarDeclNode) node); break;
        case CLASSVARNODE: classVarNode((ClassVarNode) node); break;
        case COLON2NODE: colon2Node((Colon2Node) node); break;
        case COLON3NODE: colon3Node((Colon3Node) node); break;
        case CONSTDECLNODE: constDeclNode((ConstDeclNode) node); break;
        case CONSTNODE: constNode((ConstNode) node); break;
        case DASGNNODE: dAsgnNode((DAsgnNode) node); break;
        case DEFINEDNODE: noDataContents(node); break;
        case DEFNNODE: noDataContents(node); break;
        case DEFSNODE: noDataContents(node); break;
        case DOTNODE: dotNode((DotNode) node); break;
        case DREGEXPNODE: dRegexpNode((DRegexpNode) node); break;
        case DSTRNODE: noDataContents(node); break;
        case DSYMBOLNODE: noDataContents(node); break;
        case DVARNODE: dVarNode((DVarNode) node); break;
        case DXSTRNODE: noDataContents(node); break;
        case ENSURENODE: noDataContents(node); break;
        case EVSTRNODE: noDataContents(node); break;
        case FALSENODE: noDataContents(node); break;
        case FCALLNODE: fCallNode((FCallNode) node); break;
        case FIXNUMNODE: fixnumNode((FixnumNode) node); break;
        case FLIPNODE: flipNode((FlipNode) node); break;
        case FLOATNODE: floatNode((FloatNode) node); break;
        case FORNODE: noDataContents(node); break;
        case GLOBALASGNNODE: globalAsgnNode((GlobalAsgnNode) node); break;
        case GLOBALVARNODE: globalVarNode((GlobalVarNode) node); break;
        case HASHNODE: noDataContents(node); break;
        case IFNODE: noDataContents(node); break;
        case INSTASGNNODE: noDataContents(node); instAsgnNode((InstAsgnNode) node); break;
        case INSTVARNODE: noDataContents(node); instVarNode((InstVarNode) node); break;
        case ITERNODE: noDataContents(node); break;
        case LOCALASGNNODE: localAsgnNode((LocalAsgnNode) node); break;
        case LOCALVARNODE: localVarNode((LocalVarNode) node); break;
        case MATCH2NODE: noDataContents(node); break;
        case MATCH3NODE: noDataContents(node); break;
        case MATCHNODE: noDataContents(node); break;
        case MODULENODE: noDataContents(node); break;
        case MULTIPLEASGNNODE: noDataContents(node); break;
        case NEWLINENODE: noDataContents(node); break;
        case NEXTNODE: noDataContents(node); break;
        case NILNODE: noDataContents(node); break;
        case NOTNODE: noDataContents(node); break;
        case NTHREFNODE: nthRefNode((NthRefNode) node); break;
        case OPASGNANDNODE: noDataContents(node); break;
        case OPASGNNODE: opAsgnNode((OpAsgnNode) node); break;
        case OPASGNORNODE: noDataContents(node); break;
        case OPELEMENTASGNNODE: opElementAsgnNode((OpElementAsgnNode) node); break;
        case ORNODE: noDataContents(node); break;
        case PREEXENODE: noDataContents(node); break;
        case POSTEXENODE: noDataContents(node); break;
        case REDONODE: noDataContents(node); break;
        case REGEXPNODE: regexpNode((RegexpNode) node); break;
        case RESCUEBODYNODE: noDataContents(node); break;
        case RESCUENODE: noDataContents(node); break;
        case RETRYNODE: noDataContents(node); break;
        case RETURNNODE: noDataContents(node); break;
        case ROOTNODE: noDataContents(node); break;
        case SCLASSNODE: noDataContents(node); break;
        case SELFNODE: noDataContents(node); break;
        case SPLATNODE: noDataContents(node); break;
        case STRNODE: strNode((StrNode) node); break;
        case SUPERNODE: noDataContents(node); break;
        case SVALUENODE: noDataContents(node); break;
        case SYMBOLNODE: symbolNode((SymbolNode) node); break;
        case TOARYNODE: noDataContents(node); break;
        case TRUENODE: noDataContents(node); break;
        case UNDEFNODE: undefNode((UndefNode) node); break;
        case UNTILNODE: noDataContents(node); break;
        case VALIASNODE: valiasNode((VAliasNode) node); break;
        case VCALLNODE: vcallNode((VCallNode) node); break;
        case WHENNODE: noDataContents(node); break;
        case WHILENODE: noDataContents(node); break;
        case XSTRNODE: xStrNode((XStrNode) node); break;
        case YIELDNODE: noDataContents(node); break;
        case ZARRAYNODE: noDataContents(node); break;
        case ZSUPERNODE: noDataContents(node); break;
        default:
        }
    }

    private void xStrNode(XStrNode node) {
        sb.append(" '").append(node.getValue()).append("'");
    }

    private void vcallNode(VCallNode node) {
        sb.append(" ").append(node.getName());
    }

    private void valiasNode(VAliasNode node) {
        sb.append(" ").append(node.getOldName()).append(node.getNewName());
    }

    private void undefNode(UndefNode node) {
        sb.append(" ").append(node.getName());
    }

    private void strNode(StrNode node) {
        if (node instanceof FileNode) {
            // don't put the filename in, since it can vary based on filesystem
            // layout and does not change behavior directly
            sb.append(" __FILE__");
        } else {
            sb.append(" '").append(node.getValue()).append("'");
        }
    }

    private void regexpNode(RegexpNode node) {
        sb.append(" ").append(node.getValue()).append(" ").append(node.getOptions());
    }

    private void opElementAsgnNode(OpElementAsgnNode node) {
        sb.append(" ").append(node.getOperatorName());
    }

    private void nthRefNode(NthRefNode node) {
        sb.append(" ").append(node.getMatchNumber());
    }

    private void localAsgnNode(LocalAsgnNode node) {
        sb.append(" ").append(node.getName());
    }

    private void instVarNode(InstVarNode node) {
        sb.append(" ").append(node.getName());
    }

    private void instAsgnNode(InstAsgnNode node) {
        sb.append(" ").append(node.getName());
    }

    private void globalVarNode(GlobalVarNode node) {
        sb.append(" ").append(node.getName());
    }

    private void globalAsgnNode(GlobalAsgnNode node) {
        sb.append(" ").append(node.getName());
    }

    private void floatNode(FloatNode node) {
        sb.append(" ").append(node.getValue());
    }

    private void flipNode(FlipNode node) {
        sb.append(" ").append(node.isExclusive());
    }

    private void fixnumNode(FixnumNode node) {
        sb.append(" ").append(node.getValue());
    }

    private void fCallNode(FCallNode node) {
        sb.append(" ").append(node.getName());
    }

    private void dVarNode(DVarNode node) {
        sb.append(" ").append(node.getName());
    }

    private void blockArgNode(BlockArgNode node) {
        sb.append(" ").append(node.getName());
    }

    private void backRefNode(BackRefNode node) {
        sb.append(" ").append(node.getType());
    }

    private void symbolNode(SymbolNode node) {
        sb.append(" ").append(node.getName());
    }

    private void localVarNode(LocalVarNode node) {
        sb.append(" ").append(node.getName());
    }

    private void argumentNode(ArgumentNode node) {
        sb.append(" ").append(node.getName());
    }

    private void dRegexpNode(DRegexpNode node) {
        sb.append(" ").append(node.getOnce()).append(" ").append(node.getOptions());
    }

    private void dotNode(DotNode node) {
        sb.append(" ").append(node.isExclusive()).append(" ").append(node.isLiteral());
    }

    private void dAsgnNode(DAsgnNode node) {
        sb.append(" ").append(node.getName());
    }

    private void constNode(ConstNode node) {
        sb.append(" ").append(node.getName());
    }

    private void constDeclNode(ConstDeclNode node) {
        sb.append(" ").append(node.getName());
    }

    private void colon3Node(Colon3Node node) {
        sb.append(" ").append(node.getName());
    }

    private void colon2Node(Colon2Node node) {
        sb.append(" ").append(node.getName());
    }

    private void classVarNode(ClassVarNode node) {
        sb.append(" ").append(node.getName());
    }

    private void classVarDeclNode(ClassVarDeclNode node) {
        sb.append(" ").append(node.getName());
    }

    private void classVarAsgnNode(ClassVarAsgnNode node) {
        sb.append(" ").append(node.getName());
    }

    private void callNode(CallNode node) {
        sb.append(" ").append(node.getName());
    }


    private void bignumNode(BignumNode node) {
        sb.append(" ").append(node.getValue());
    }

    private void attrAssignNode(AttrAssignNode node) {
        sb.append(" ").append(node.getName());
    }

    private void aliasNode(AliasNode node) {
        sb.append(" ").append(node.getOldName()).append(node.getNewName());
    }

    private void opAsgnNode(OpAsgnNode node) {
        sb.append(" '").append(node.getOperatorName()).append("'");
    }

    private void noDataContents(Node node) {
    }
        
}
