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
    public static String create(Node node) {
        StringBuilder sb = new StringBuilder(100);
        
        process(sb, node);
        
        return sb.toString();
    }
    
    public static String create(String methodName, Node argsNode, Node body) {
        StringBuilder sb = new StringBuilder(100);
        
        processMethod(sb, methodName, argsNode, body);
        
        return sb.toString();
    }
    
    private static void processMethod(StringBuilder sb, String methodName, Node argsNode, Node body) {
        sb.append("(method ").append(methodName).append(' ');
        // JRUBY-4301, include filename and line in sexp
        sb.append("(file ").append(new File(body.getPosition().getFile()).getName()).append(") ");
        sb.append("(line ").append(body.getPosition().getStartLine()).append(") ");
        process(sb, argsNode);
        sb.append(' ');
        process(sb, body);
        sb.append(')');
    }

    /**
     * process each node by printing out '(' name data child* ')'
     * @param node
     */
    private static void process(StringBuilder sb, Node node) {
        if (node == null) {
            sb.append("null");
            return;
        }
        
        sb.append('(');
        shortName(sb, node);
        leafInfo(sb, node);
        
        for (Node child: node.childNodes()) {
            sb.append(' ');
            process(sb, child);
        }
        
        sb.append(')');
    }

    private static void shortName(StringBuilder sb, Node node) {
        sb.append(node.getNodeType().simpleName());
    }

    /**
     * Extra information that is not child nodes, but actual leaf data.
     *
     * @param node
     */
    private static void leafInfo(StringBuilder sb, Node node) {
        switch (node.getNodeType()) {
            case ALIASNODE: aliasNode(sb, (AliasNode) node); break;
            case ARGUMENTNODE: argumentNode(sb, (ArgumentNode) node); break;
            case ATTRASSIGNNODE: attrAssignNode(sb, (AttrAssignNode) node); break;
            case BACKREFNODE: backRefNode(sb, (BackRefNode) node); break;
            case BIGNUMNODE: bignumNode(sb, (BignumNode) node); break;
            case BLOCKARGNODE: blockArgNode(sb, (BlockArgNode) node); break;
            case CALLNODE: callNode(sb, (CallNode) node); break;
            case CLASSVARASGNNODE: classVarAsgnNode(sb, (ClassVarAsgnNode) node); break;
            case CLASSVARDECLNODE: classVarDeclNode(sb, (ClassVarDeclNode) node); break;
            case CLASSVARNODE: classVarNode(sb, (ClassVarNode) node); break;
            case COLON2NODE: colon2Node(sb, (Colon2Node) node); break;
            case COLON3NODE: colon3Node(sb, (Colon3Node) node); break;
            case CONSTDECLNODE: constDeclNode(sb, (ConstDeclNode) node); break;
            case CONSTNODE: constNode(sb, (ConstNode) node); break;
            case DASGNNODE: dAsgnNode(sb, (DAsgnNode) node); break;
            case DOTNODE: dotNode(sb, (DotNode) node); break;
            case DREGEXPNODE: dRegexpNode(sb, (DRegexpNode) node); break;
            case DVARNODE: dVarNode(sb, (DVarNode) node); break;
            case FCALLNODE: fCallNode(sb, (FCallNode) node); break;
            case FIXNUMNODE: fixnumNode(sb, (FixnumNode) node); break;
            case FLIPNODE: flipNode(sb, (FlipNode) node); break;
            case FLOATNODE: floatNode(sb, (FloatNode) node); break;
            case GLOBALASGNNODE: globalAsgnNode(sb, (GlobalAsgnNode) node); break;
            case GLOBALVARNODE: globalVarNode(sb, (GlobalVarNode) node); break;
            case INSTASGNNODE: instAsgnNode(sb, (InstAsgnNode) node); break;
            case INSTVARNODE: instVarNode(sb, (InstVarNode) node); break;
            case LOCALASGNNODE: localAsgnNode(sb, (LocalAsgnNode) node); break;
            case LOCALVARNODE: localVarNode(sb, (LocalVarNode) node); break;
            case NTHREFNODE: nthRefNode(sb, (NthRefNode) node); break;
            case OPASGNNODE: opAsgnNode(sb, (OpAsgnNode) node); break;
            case OPELEMENTASGNNODE: opElementAsgnNode(sb, (OpElementAsgnNode) node); break;
            case REGEXPNODE: regexpNode(sb, (RegexpNode) node); break;
            case STRNODE: strNode(sb, (StrNode) node); break;
            case SYMBOLNODE: symbolNode(sb, (SymbolNode) node); break;
            case UNDEFNODE: undefNode(sb, (UndefNode) node); break;
            case VALIASNODE: valiasNode(sb, (VAliasNode) node); break;
            case VCALLNODE: vcallNode(sb, (VCallNode) node); break;
            case XSTRNODE: xStrNode(sb, (XStrNode) node); break;

            /* these do nothing
            case ANDNODE:
            case ARGSCATNODE:
            case ARGSPUSHNODE:
            case ARRAYNODE:
            case BEGINNODE:
            case BLOCKNODE:
            case BLOCKPASSNODE:
            case BREAKNODE:
            case CASENODE:
            case CLASSNODE:
            case DEFINEDNODE:
            case DEFNNODE:
            case DEFSNODE:
            case DSTRNODE:
            case DSYMBOLNODE:
            case DXSTRNODE:
            case ENSURENODE:
            case EVSTRNODE:
            case FALSENODE:
            case FORNODE:
            case HASHNODE:
            case IFNODE:
            case ITERNODE:
            case MATCH2NODE:
            case MATCH3NODE:
            case MATCHNODE:
            case MODULENODE:
            case MULTIPLEASGNNODE:
            case NEWLINENODE:
            case NEXTNODE:
            case NILNODE:
            case NOTNODE:
            case OPASGNORNODE:
            case OPASGNANDNODE:
            case ORNODE:
            case PREEXENODE:
            case POSTEXENODE:
            case REDONODE:
            case RESCUEBODYNODE:
            case RESCUENODE:
            case RETRYNODE:
            case RETURNNODE:
            case ROOTNODE:
            case SCLASSNODE:
            case SELFNODE:
            case SPLATNODE:
            case SUPERNODE:
            case SVALUENODE:
            case TOARYNODE:
            case TRUENODE:
            case UNTILNODE:
            case WHENNODE:
            case WHILENODE:
            case YIELDNODE:
            case ZARRAYNODE:
            case ZSUPERNODE:
                noDataContents(node);
                break;
            */
            
            default:
        }
    }

    private static void xStrNode(StringBuilder sb, XStrNode node) {
        sb.append(" '").append(node.getValue()).append('\'');
    }

    private static void vcallNode(StringBuilder sb, VCallNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void valiasNode(StringBuilder sb, VAliasNode node) {
        sb.append(' ').append(node.getOldName()).append(node.getNewName());
    }

    private static void undefNode(StringBuilder sb, UndefNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void strNode(StringBuilder sb, StrNode node) {
        if (node instanceof FileNode) {
            // don't put the filename in, since it can vary based on filesystem
            // layout and does not change behavior directly
            sb.append(" __FILE__");
        } else {
            sb.append(" '").append(node.getValue()).append('\'');
        }
    }

    private static void regexpNode(StringBuilder sb, RegexpNode node) {
        sb.append(' ').append(node.getValue()).append(' ').append(node.getOptions());
    }

    private static void opElementAsgnNode(StringBuilder sb, OpElementAsgnNode node) {
        sb.append(' ').append(node.getOperatorName());
    }

    private static void nthRefNode(StringBuilder sb, NthRefNode node) {
        sb.append(' ').append(node.getMatchNumber());
    }

    private static void localAsgnNode(StringBuilder sb, LocalAsgnNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void instVarNode(StringBuilder sb, InstVarNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void instAsgnNode(StringBuilder sb, InstAsgnNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void globalVarNode(StringBuilder sb, GlobalVarNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void globalAsgnNode(StringBuilder sb, GlobalAsgnNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void floatNode(StringBuilder sb, FloatNode node) {
        sb.append(' ').append(node.getValue());
    }

    private static void flipNode(StringBuilder sb, FlipNode node) {
        sb.append(' ').append(node.isExclusive());
    }

    private static void fixnumNode(StringBuilder sb, FixnumNode node) {
        sb.append(' ').append(node.getValue());
    }

    private static void fCallNode(StringBuilder sb, FCallNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void dVarNode(StringBuilder sb, DVarNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void blockArgNode(StringBuilder sb, BlockArgNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void backRefNode(StringBuilder sb, BackRefNode node) {
        sb.append(' ').append(node.getType());
    }

    private static void symbolNode(StringBuilder sb, SymbolNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void localVarNode(StringBuilder sb, LocalVarNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void argumentNode(StringBuilder sb, ArgumentNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void dRegexpNode(StringBuilder sb, DRegexpNode node) {
        sb.append(' ').append(node.getOnce()).append(' ').append(node.getOptions());
    }

    private static void dotNode(StringBuilder sb, DotNode node) {
        sb.append(' ').append(node.isExclusive()).append(' ').append(node.isLiteral());
    }

    private static void dAsgnNode(StringBuilder sb, DAsgnNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void constNode(StringBuilder sb, ConstNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void constDeclNode(StringBuilder sb, ConstDeclNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void colon3Node(StringBuilder sb, Colon3Node node) {
        sb.append(' ').append(node.getName());
    }

    private static void colon2Node(StringBuilder sb, Colon2Node node) {
        sb.append(' ').append(node.getName());
    }

    private static void classVarNode(StringBuilder sb, ClassVarNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void classVarDeclNode(StringBuilder sb, ClassVarDeclNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void classVarAsgnNode(StringBuilder sb, ClassVarAsgnNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void callNode(StringBuilder sb, CallNode node) {
        sb.append(' ').append(node.getName());
    }


    private static void bignumNode(StringBuilder sb, BignumNode node) {
        sb.append(' ').append(node.getValue());
    }

    private static void attrAssignNode(StringBuilder sb, AttrAssignNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void aliasNode(StringBuilder sb, AliasNode node) {
        sb.append(' ').append(node.getOldName()).append(node.getNewName());
    }

    private static void opAsgnNode(StringBuilder sb, OpAsgnNode node) {
        sb.append(" '").append(node.getOperatorName()).append('\'');
    }
        
}
