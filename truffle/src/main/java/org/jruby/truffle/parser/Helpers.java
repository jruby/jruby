/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.truffle.parser;

import org.jruby.truffle.parser.ast.ArgsParseNode;
import org.jruby.truffle.parser.ast.ArgumentParseNode;
import org.jruby.truffle.parser.ast.DAsgnParseNode;
import org.jruby.truffle.parser.ast.LocalAsgnParseNode;
import org.jruby.truffle.parser.ast.MultipleAsgnParseNode;
import org.jruby.truffle.parser.ast.OptArgParseNode;
import org.jruby.truffle.parser.ast.ParseNode;
import org.jruby.truffle.parser.ast.RequiredKeywordArgumentValueParseNode;
import org.jruby.truffle.parser.ast.UnnamedRestArgParseNode;
import org.jruby.truffle.parser.ast.types.INameNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper methods which are called by the compiler.  Note: These will show no consumers, but
 * generated code does call these so don't remove them thinking they are dead code.
 *
 */
public class Helpers {

    public static final Map<String, String> map(String... keyValues) {
        HashMap<String, String> map = new HashMap<>(keyValues.length / 2);
        for (int i = 0; i < keyValues.length;) {
            map.put(keyValues[i++], keyValues[i++]);
        }
        return map;
    }

    /** Use an ArgsParseNode (used for blocks) to generate ArgumentDescriptors */
    public static ArgumentDescriptor[] argsNodeToArgumentDescriptors(ArgsParseNode argsNode) {
        ArrayList<ArgumentDescriptor> descs = new ArrayList<>();
        ParseNode[] args = argsNode.getArgs();
        int preCount = argsNode.getPreCount();

        if (preCount > 0) {
            for (int i = 0; i < preCount; i++) {
                if (args[i] instanceof MultipleAsgnParseNode) {
                    descs.add(new ArgumentDescriptor(ArgumentType.anonreq));
                } else {
                    descs.add(new ArgumentDescriptor(ArgumentType.req, ((ArgumentParseNode) args[i]).getName()));
                }
            }
        }


        int optCount = argsNode.getOptionalArgsCount();
        if (optCount > 0) {
            int optIndex = argsNode.getOptArgIndex();

            for (int i = 0; i < optCount; i++) {
                ArgumentType type = ArgumentType.opt;
                ParseNode optNode = args[optIndex + i];
                String name = null;
                if (optNode instanceof OptArgParseNode) {
                    name = ((OptArgParseNode)optNode).getName();
                } else if (optNode instanceof LocalAsgnParseNode) {
                    name = ((LocalAsgnParseNode)optNode).getName();
                } else if (optNode instanceof DAsgnParseNode) {
                    name = ((DAsgnParseNode)optNode).getName();
                } else {
                    type = ArgumentType.anonopt;
                }
                descs.add(new ArgumentDescriptor(type, name));
            }
        }

        ArgumentParseNode restArg = argsNode.getRestArgNode();
        if (restArg != null) {
            if (restArg instanceof UnnamedRestArgParseNode) {
                if (((UnnamedRestArgParseNode) restArg).isStar()) descs.add(new ArgumentDescriptor(ArgumentType.anonrest));
            } else {
                descs.add(new ArgumentDescriptor(ArgumentType.rest, restArg.getName()));
            }
        }

        int postCount = argsNode.getPostCount();
        if (postCount > 0) {
            int postIndex = argsNode.getPostIndex();
            for (int i = 0; i < postCount; i++) {
                ParseNode postNode = args[postIndex + i];
                if (postNode instanceof MultipleAsgnParseNode) {
                    descs.add(new ArgumentDescriptor(ArgumentType.anonreq));
                } else {
                    descs.add(new ArgumentDescriptor(ArgumentType.req, ((ArgumentParseNode)postNode).getName()));
                }
            }
        }

        int keywordsCount = argsNode.getKeywordCount();
        if (keywordsCount > 0) {
            int keywordsIndex = argsNode.getKeywordsIndex();
            for (int i = 0; i < keywordsCount; i++) {
                ParseNode keyWordNode = args[keywordsIndex + i];
                for (ParseNode asgnNode : keyWordNode.childNodes()) {
                    if (isRequiredKeywordArgumentValueNode(asgnNode)) {
                        descs.add(new ArgumentDescriptor(ArgumentType.keyreq, ((INameNode) asgnNode).getName()));
                    } else {
                        descs.add(new ArgumentDescriptor(ArgumentType.key, ((INameNode) asgnNode).getName()));
                    }
                }
            }
        }

        if (argsNode.getKeyRest() != null) {
            String argName = argsNode.getKeyRest().getName();
            if (argName == null || argName.length() == 0) {
                descs.add(new ArgumentDescriptor(ArgumentType.anonkeyrest, argName));
            } else {
                descs.add(new ArgumentDescriptor(ArgumentType.keyrest, argsNode.getKeyRest().getName()));
            }
        }
        if (argsNode.getBlock() != null) descs.add(new ArgumentDescriptor(ArgumentType.block, argsNode.getBlock().getName()));

        return descs.toArray(new ArgumentDescriptor[descs.size()]);
    }

    public static boolean isRequiredKeywordArgumentValueNode(ParseNode asgnNode) {
        return asgnNode.childNodes().get(0) instanceof RequiredKeywordArgumentValueParseNode;
    }

}
