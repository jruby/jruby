/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jvyamlb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;

import org.jvyamlb.nodes.MappingNode;
import org.jvyamlb.nodes.Node;
import org.jvyamlb.nodes.ScalarNode;
import org.jvyamlb.nodes.SequenceNode;

import org.jruby.util.ByteList;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class ResolverImpl implements Resolver {
    private final static Map yamlPathResolvers = new HashMap();

    private final static ResolverScanner SCANNER = new ResolverScanner();

    private List resolverExactPaths = new LinkedList();
    private List resolverPrefixPaths = new LinkedList();

    public static void addPathResolver(final String tag, final List path, final Class kind) {
        final List newPath = new LinkedList();
        Object nodeCheck=null;
        Object indexCheck=null;
        for(final Iterator iter = path.iterator();iter.hasNext();) {
            final Object element = iter.next();
            if(element instanceof List) {
                final List eList = (List)element;
                if(eList.size() == 2) {
                    nodeCheck = eList.get(0);
                    indexCheck = eList.get(1);
                } else if(eList.size() == 1) {
                    nodeCheck = eList.get(0);
                    indexCheck = Boolean.TRUE;
                } else {
                    throw new ResolverException("Invalid path element: " + element);
                }
            } else {
                nodeCheck = null;
                indexCheck = element;
            }

            if(nodeCheck instanceof String || nodeCheck instanceof ByteList) {
                nodeCheck = ScalarNode.class;
            } else if(nodeCheck instanceof List) {
                nodeCheck = SequenceNode.class;
            } else if(nodeCheck instanceof Map) {
                nodeCheck = MappingNode.class;
            } else if(null != nodeCheck && !ScalarNode.class.equals(nodeCheck) && !SequenceNode.class.equals(nodeCheck) && !MappingNode.class.equals(nodeCheck)) {
                throw new ResolverException("Invalid node checker: " + nodeCheck);
            }
            if(!(indexCheck instanceof String || nodeCheck instanceof ByteList || indexCheck instanceof Integer) && null != indexCheck) {
                throw new ResolverException("Invalid index checker: " + indexCheck);
            }
            newPath.add(new Object[]{nodeCheck,indexCheck});
        }
        Class newKind = null;
        if(String.class.equals(kind) || ByteList.class.equals(kind)) {
            newKind = ScalarNode.class;
        } else if(List.class.equals(kind)) {
            newKind = SequenceNode.class;
        } else if(Map.class.equals(kind)) {
            newKind = MappingNode.class;
        } else if(kind != null && !ScalarNode.class.equals(kind) && !SequenceNode.class.equals(kind) && !MappingNode.class.equals(kind)) {
            throw new ResolverException("Invalid node kind: " + kind);
        } else {
            newKind = kind;
        }
        final List x = new ArrayList(1);
        x.add(newPath);
        final List y = new ArrayList(2);
        y.add(x);
        y.add(kind);
        yamlPathResolvers.put(y,tag);
    }

    public void descendResolver(final Node currentNode, final Object currentIndex) {
        final Map exactPaths = new HashMap();
        final List prefixPaths = new LinkedList();
        if(null != currentNode) {
            final int depth = resolverPrefixPaths.size();
            for(final Iterator iter = ((List)resolverPrefixPaths.get(0)).iterator();iter.hasNext();) {
                final Object[] obj = (Object[])iter.next();
                final List path = (List)obj[0];
                if(checkResolverPrefix(depth,path,(Class)obj[1],currentNode,currentIndex)) {
                    if(path.size() > depth) {
                        prefixPaths.add(new Object[] {path,obj[1]});
                    } else {
                        final List resPath = new ArrayList(2);
                        resPath.add(path);
                        resPath.add(obj[1]);
                        exactPaths.put(obj[1],yamlPathResolvers.get(resPath));
                    }
                }
            }
        } else {
            for(final Iterator iter = yamlPathResolvers.keySet().iterator();iter.hasNext();) {
                final List key = (List)iter.next();
                final List path = (List)key.get(0);
                final Class kind = (Class)key.get(1);
                if(null == path) {
                    exactPaths.put(kind,yamlPathResolvers.get(key));
                } else {
                    prefixPaths.add(key);
                }
            }
        }
        resolverExactPaths.add(0,exactPaths);
        resolverPrefixPaths.add(0,prefixPaths);
    }

    public void ascendResolver() {
        resolverExactPaths.remove(0);
        resolverPrefixPaths.remove(0);
    }

    public boolean checkResolverPrefix(final int depth, final List path, final Class kind, final Node currentNode, final Object currentIndex) {
        final Object[] check = (Object[])path.get(depth-1);
        final Object nodeCheck = check[0];
        final Object indexCheck = check[1];
        if(nodeCheck instanceof String) {
            if(!currentNode.getTag().equals(nodeCheck)) {
                return false;
            }
        } else if(null != nodeCheck) {
            if(!((Class)nodeCheck).isInstance(currentNode)) {
                return false;
            }
        }
        if(indexCheck == Boolean.TRUE && currentIndex != null) {
            return false;
        }
        if(indexCheck == Boolean.FALSE && currentIndex == null) {
            return false;
        }
        if(indexCheck instanceof String) {
            if(!(currentIndex instanceof ScalarNode && indexCheck.equals(((ScalarNode)currentIndex).getValue()))) {
                return false;
            }
        } else if(indexCheck instanceof ByteList) {
            if(!(currentIndex instanceof ScalarNode && indexCheck.equals(((ScalarNode)currentIndex).getValue()))) {
                return false;
            }
        } else if(indexCheck instanceof Integer) {
            if(!currentIndex.equals(indexCheck)) {
                return false;
            }
        }
        return true;
    }
    
    public String resolve(final Class kind, final ByteList value, final boolean[] implicit) {
        List resolvers = null;
        if(kind.equals(ScalarNode.class) && implicit[0]) {
            String resolv = SCANNER.recognize(value);
            if(resolv != null) {
                return resolv;
            }
        }
        final Map exactPaths = (Map)resolverExactPaths.get(0);
        if(exactPaths.containsKey(kind)) {
            return (String)exactPaths.get(kind);
        }
        if(exactPaths.containsKey(null)) {
            return (String)exactPaths.get(null);
        }
        if(kind.equals(ScalarNode.class)) {
            return YAML.DEFAULT_SCALAR_TAG;
        } else if(kind.equals(SequenceNode.class)) {
            return YAML.DEFAULT_SEQUENCE_TAG;
        } else if(kind.equals(MappingNode.class)) {
            return YAML.DEFAULT_MAPPING_TAG;
        }
        return null;
    } 
    
    private static ByteList s(String se){
        return new ByteList(se.getBytes());
    }

    public static void main(String[] args) {
        ByteList[] strings = {s("yes"), s("NO"), s("booooooooooooooooooooooooooooooooooooooooooooooool"), s("false"),s(""), s("~"),s("~a"),
                            s("<<"), s("10.1"), s("10000000000003435345.2324E+13"), s(".23"), s(".nan"), s("null"), s("124233333333333333"),
                            s("0b030323"), s("+0b0111111011010101"), s("0xaafffdf"), s("2005-05-03"), s("2005-05-03a"), s(".nana"),
                            s("2005-03-05T05:23:22"), s("="), s("= "), s("=a")};
        boolean[] implicit = new boolean[]{true,true};
        Resolver res = new ResolverImpl();
        res.descendResolver(null,null);
        Class s = ScalarNode.class;
        final long before = System.currentTimeMillis();
        final int NUM = 100000;
        for(int j=0;j<NUM;j++) {
            for(int i=0;i<strings.length;i++) {
                res.resolve(s,strings[i%(strings.length)],implicit);
            }
        }
        final long after = System.currentTimeMillis();
        final long time = after-before;
        final double timeS = (after-before)/1000.0;
        System.out.println("Resolving " + NUM*strings.length + " nodes took " + time + "ms, or " + timeS + " seconds"); 
    }
}// ResolverImpl
