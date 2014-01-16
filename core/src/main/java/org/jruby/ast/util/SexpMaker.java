package org.jruby.ast.util;

import java.io.File;
import java.nio.CharBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;

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
import org.jruby.util.ConvertBytes;

public class SexpMaker {
    private interface Builder {
        public Builder append(String str);
        public Builder append(char ch);
        public Builder append(int i);
        public Builder append(Object o);
        public Builder append(boolean b);
        public Builder append(long l);
        public Builder append(double d);
    }

    private static class StringBuilder implements Builder {
        final java.lang.StringBuilder b;

        StringBuilder(int size) {
            b = new java.lang.StringBuilder(size);
        }

        @Override
        public Builder append(Object o) {
            append(o.toString());
            return this;
        }

        @Override
        public Builder append(String str) {
            b.append(str);
            return this;
        }

        @Override
        public Builder append(boolean bool) {
            b.append(bool);
            return this;
        }

        @Override
        public Builder append(char ch) {
            b.append(ch);
            return this;
        }

        @Override
        public Builder append(int i) {
            b.append(i);
            return this;
        }

        @Override
        public Builder append(long l) {
            b.append(l);
            return this;
        }

        @Override
        public Builder append(double d) {
            b.append(d);
            return this;
        }
    }

    private static class DigestBuilder implements Builder {
        MessageDigest d;

        DigestBuilder(MessageDigest digest) {
            this.d = digest;
        }

        @Override
        public Builder append(Object o) {
            append(o.toString());
            return this;
        }

        @Override
        public Builder append(String str) {
            d.update(str.getBytes());
            return this;
        }

        @Override
        public Builder append(boolean b) {
            append((byte) (b ? 1 : 0));
            return this;
        }

        @Override
        public Builder append(char ch) {
            d.update((byte)(ch >> 8));
            d.update((byte)(ch));
            return this;
        }

        @Override
        public Builder append(int i) {
            append((char) (i >> 16));
            append((char) i);
            return this;
        }

        @Override
        public Builder append(long l) {
            append((int) (l >> 32));
            append((int) l);
            return this;
        }

        @Override
        public Builder append(double d) {
            append(Double.doubleToLongBits(d));
            return this;
        }
    }
    
    public static String create(Node node) {
        Builder sb = new StringBuilder(100);

        process(sb, node);

        return sb.toString();
    }

    public static String create(String methodName, Node argsNode, Node body) {
        Builder sb = new StringBuilder(100);

        processMethod(sb, methodName, argsNode, body);

        return sb.toString();
    }

    public static String sha1(String methodName, Node argsNode, Node body) {
        MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException(nsae);
        }

        DigestBuilder db = new DigestBuilder(sha1);

        processMethod(db, methodName, argsNode, body);

        byte[] digest = db.d.digest();

        return new String(ConvertBytes.twosComplementToHexBytes(digest, false));
    }

    private static void processMethod(Builder sb, String methodName, Node argsNode, Node body) {
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
    private static void process(Builder sb, Node node) {
        if (node == null) {
            sb.append("null");
            return;
        }

        sb.append('(');
        shortName(sb, node);
        leafInfo(sb, node);

        List<Node> nodes = node.childNodes();
        for (int i = 0; i < nodes.size(); i++) {
            Node child = nodes.get(i);
            sb.append(' ');
            process(sb, child);
        }

        sb.append(')');
    }

    private static void shortName(Builder sb, Node node) {
        sb.append(node.getNodeType().simpleName());
    }

    /**
     * Extra information that is not child nodes, but actual leaf data.
     *
     * @param node
     */
    private static void leafInfo(Builder sb, Node node) {
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

    private static void xStrNode(Builder sb, XStrNode node) {
        sb.append(" '").append(node.getValue()).append('\'');
    }

    private static void vcallNode(Builder sb, VCallNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void valiasNode(Builder sb, VAliasNode node) {
        sb.append(' ').append(node.getOldName()).append(node.getNewName());
    }

    private static void undefNode(Builder sb, UndefNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void strNode(Builder sb, StrNode node) {
        if (node instanceof FileNode) {
            // don't put the filename in, since it can vary based on filesystem
            // layout and does not change behavior directly
            sb.append(" __FILE__");
        } else {
            sb.append(" '").append(node.getValue()).append('\'');
        }
    }

    private static void regexpNode(Builder sb, RegexpNode node) {
        sb.append(' ').append(node.getValue()).append(' ').append(node.getOptions());
    }

    private static void opElementAsgnNode(Builder sb, OpElementAsgnNode node) {
        sb.append(' ').append(node.getOperatorName());
    }

    private static void nthRefNode(Builder sb, NthRefNode node) {
        sb.append(' ').append(node.getMatchNumber());
    }

    private static void localAsgnNode(Builder sb, LocalAsgnNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void instVarNode(Builder sb, InstVarNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void instAsgnNode(Builder sb, InstAsgnNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void globalVarNode(Builder sb, GlobalVarNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void globalAsgnNode(Builder sb, GlobalAsgnNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void floatNode(Builder sb, FloatNode node) {
        sb.append(' ').append(node.getValue());
    }

    private static void flipNode(Builder sb, FlipNode node) {
        sb.append(' ').append(node.isExclusive());
    }

    private static void fixnumNode(Builder sb, FixnumNode node) {
        sb.append(' ').append(node.getValue());
    }

    private static void fCallNode(Builder sb, FCallNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void dVarNode(Builder sb, DVarNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void blockArgNode(Builder sb, BlockArgNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void backRefNode(Builder sb, BackRefNode node) {
        sb.append(' ').append(node.getType());
    }

    private static void symbolNode(Builder sb, SymbolNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void localVarNode(Builder sb, LocalVarNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void argumentNode(Builder sb, ArgumentNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void dRegexpNode(Builder sb, DRegexpNode node) {
        sb.append(' ').append(node.getOnce()).append(' ').append(node.getOptions());
    }

    private static void dotNode(Builder sb, DotNode node) {
        sb.append(' ').append(node.isExclusive()).append(' ').append(node.isLiteral());
    }

    private static void dAsgnNode(Builder sb, DAsgnNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void constNode(Builder sb, ConstNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void constDeclNode(Builder sb, ConstDeclNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void colon3Node(Builder sb, Colon3Node node) {
        sb.append(' ').append(node.getName());
    }

    private static void colon2Node(Builder sb, Colon2Node node) {
        sb.append(' ').append(node.getName());
    }

    private static void classVarNode(Builder sb, ClassVarNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void classVarDeclNode(Builder sb, ClassVarDeclNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void classVarAsgnNode(Builder sb, ClassVarAsgnNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void callNode(Builder sb, CallNode node) {
        sb.append(' ').append(node.getName());
    }


    private static void bignumNode(Builder sb, BignumNode node) {
        sb.append(' ').append(node.getValue());
    }

    private static void attrAssignNode(Builder sb, AttrAssignNode node) {
        sb.append(' ').append(node.getName());
    }

    private static void aliasNode(Builder sb, AliasNode node) {
        sb.append(' ').append(node.getOldName()).append(node.getNewName());
    }

    private static void opAsgnNode(Builder sb, OpAsgnNode node) {
        sb.append(" '").append(node.getOperatorName()).append('\'');
    }
        
}
