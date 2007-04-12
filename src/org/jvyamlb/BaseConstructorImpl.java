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

import java.io.FileInputStream;
import java.io.InputStream;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import java.util.regex.Pattern;

import org.jvyamlb.nodes.Node;
import org.jvyamlb.nodes.LinkNode;
import org.jvyamlb.nodes.ScalarNode;
import org.jvyamlb.nodes.SequenceNode;
import org.jvyamlb.nodes.MappingNode;

import org.jruby.util.ByteList;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class BaseConstructorImpl implements Constructor {
    private final static Map yamlConstructors = new HashMap();
    private final static Map yamlMultiConstructors = new HashMap();
    private final static Map yamlMultiRegexps = new HashMap();
    public YamlConstructor getYamlConstructor(final Object key) {
        return (YamlConstructor)yamlConstructors.get(key);
    }

    public YamlMultiConstructor getYamlMultiConstructor(final Object key) {
        return (YamlMultiConstructor)yamlMultiConstructors.get(key);
    }

    public Pattern getYamlMultiRegexp(final Object key) {
        return (Pattern)yamlMultiRegexps.get(key);
    }

    public Set getYamlMultiRegexps() {
        return yamlMultiRegexps.keySet();
    }

    public static void addConstructor(final String tag, final YamlConstructor ctor) {
        yamlConstructors.put(tag,ctor);
    }

    public static void addMultiConstructor(final String tagPrefix, final YamlMultiConstructor ctor) {
        yamlMultiConstructors.put(tagPrefix,ctor);
        yamlMultiRegexps.put(tagPrefix,Pattern.compile("^"+tagPrefix));
    }

    private Composer composer;
    private Map constructedObjects = new HashMap();
    private Map recursiveObjects = new HashMap();

    public BaseConstructorImpl(final Composer composer) {
        this.composer = composer;
    }

    public boolean checkData() {
        return composer.checkNode();
    }

    public Object getData() {
        if(composer.checkNode()) {
            Node node = composer.getNode();
            if(null != node) {
                return constructDocument(node);
            }
        }
        return null;
    }

    private class DocumentIterator implements Iterator {
        public boolean hasNext() {return checkData();}
        public Object next() {return getData();}
        public void remove() {}
    }
    
    public Iterator eachDocument() {
        return new DocumentIterator();
    }

    public Iterator iterator() {
        return eachDocument();
    }
    
    public Object constructDocument(final Node node) {
        final Object data = constructObject(node);
        constructedObjects.clear();
        recursiveObjects.clear();
        return data;
    }

    public static class YamlMultiAdapter implements YamlConstructor {
        private YamlMultiConstructor ctor;
        private String prefix;
        public YamlMultiAdapter(final YamlMultiConstructor ctor, final String prefix) {
            this.ctor = ctor;
            this.prefix = prefix;
        }

        public Object call(final Constructor self, final Node node) {
            return ctor.call(self,this.prefix,node);
        }
    }

    public Object constructObject(final Node node) {
        if(constructedObjects.containsKey(node)) {
            return constructedObjects.get(node);
        }
        if(recursiveObjects.containsKey(node)) {
            LinkNode n = new LinkNode();
            n.setValue(node);
            return n;
            //            throw new ConstructorException(null,"found recursive node",null);
        }
        recursiveObjects.put(node,new ArrayList());
        YamlConstructor ctor = getYamlConstructor(node.getTag());
        if(ctor == null) {
            boolean through = true;
            for(final Iterator iter = getYamlMultiRegexps().iterator();iter.hasNext();) {
                final String tagPrefix = (String)iter.next();
                final Pattern reg = getYamlMultiRegexp(tagPrefix);
                if(reg.matcher(node.getTag()).find()) {
                    final String tagSuffix = node.getTag().substring(tagPrefix.length());
                    ctor = new YamlMultiAdapter(getYamlMultiConstructor(tagPrefix),tagSuffix);
                    through = false;
                    break;
                }
            }
            if(through) {
                final YamlMultiConstructor xctor = getYamlMultiConstructor(null);
                if(null != xctor) {
                    ctor = new YamlMultiAdapter(xctor,node.getTag());
                } else {
                    ctor = getYamlConstructor(null);
                    if(ctor == null) {
                        ctor = CONSTRUCT_PRIMITIVE;
                    }
                }
            }
        }
        final Object data = ctor.call(this,node);
        constructedObjects.put(node,data);
        doRecursionFix(node,data);
        return data;
    }

    public void doRecursionFix(Node node, Object obj) {
        List ll = (List)recursiveObjects.remove(node);
        if(null != ll) {
            for(Iterator iter = ll.iterator();iter.hasNext();) {
                ((RecursiveFixer)iter.next()).replace(node, obj);
            }
        }
    }

    public Object constructPrimitive(final Node node) {
        if(node instanceof ScalarNode) {
            return constructScalar(node);
        } else if(node instanceof SequenceNode) {
            return constructSequence(node);
        } else if(node instanceof MappingNode) {
            return constructMapping(node);
        } else {
            System.err.println(node.getTag());
        }
        return null;
    }

    public Object constructScalar(final Node node) {
        if(!(node instanceof ScalarNode)) {
            if(node instanceof MappingNode) {
                final Map vals = ((Map)node.getValue());
                for(final Iterator iter = vals.keySet().iterator();iter.hasNext();) {
                    final Node key = (Node)iter.next();
                    if("tag:yaml.org,2002:value".equals(key.getTag())) {
                        return constructScalar((Node)vals.get(key));
                    }
                }
            }
            throw new ConstructorException(null,"expected a scalar node, but found " + node.getClass().getName(),null);
        }
        return node.getValue();
    }

    public Object constructPrivateType(final Node node) {
        Object val = null;
        if(node.getValue() instanceof Map) {
            val = constructMapping(node);
        } else if(node.getValue() instanceof List) {
            val = constructSequence(node);
        } else {
            val = node.getValue().toString();
        }
        return new PrivateType(node.getTag(),val);
    } 
    
    public Object constructSequence(final Node node) {
        if(!(node instanceof SequenceNode)) {
            throw new ConstructorException(null,"expected a sequence node, but found " + node.getClass().getName(),null);
        }
        final List internal = (List)node.getValue();
        final List val = new ArrayList(internal.size());
        for(final Iterator iter = internal.iterator();iter.hasNext();) {
            final Object obj = constructObject((Node)iter.next());
            if(obj instanceof LinkNode) {
                final int ix = val.size();
                addFixer((Node)(((LinkNode)obj).getValue()), new RecursiveFixer() {
                        public void replace(Node node, Object real) {
                            val.set(ix, real);
                        }
                    });
            }
            val.add(obj);
        }
        return val;
    }

    public Object constructMapping(final Node node) {
        if(!(node instanceof MappingNode)) {
            throw new ConstructorException(null,"expected a mapping node, but found " + node.getClass().getName(),null);
        }
        final Map[] mapping = new Map[]{new HashMap()};
        List merge = null;
        final Map val = (Map)node.getValue();
        for(final Iterator iter = val.keySet().iterator();iter.hasNext();) {
            final Node key_v = (Node)iter.next();
            final Node value_v = (Node)val.get(key_v);
            if(key_v.getTag().equals("tag:yaml.org,2002:merge")) {
                if(merge != null) {
                    throw new ConstructorException("while constructing a mapping", "found duplicate merge key",null);
                }
                if(value_v instanceof MappingNode) {
                    merge = new LinkedList();
                    merge.add(constructMapping(value_v));
                } else if(value_v instanceof SequenceNode) {
                    merge = new LinkedList();
                    final List vals = (List)value_v.getValue();
                    for(final Iterator sub = vals.iterator();sub.hasNext();) {
                        final Node subnode = (Node)sub.next();
                        if(!(subnode instanceof MappingNode)) {
                            throw new ConstructorException("while constructing a mapping","expected a mapping for merging, but found " + subnode.getClass().getName(),null);
                        }
                        merge.add(0,constructMapping(subnode));
                    }
                } else {
                    throw new ConstructorException("while constructing a mapping","expected a mapping or list of mappings for merging, but found " + value_v.getClass().getName(),null);
                }
            } else if(key_v.getTag().equals("tag:yaml.org,2002:value")) {
                if(mapping[0].containsKey("=")) {
                    throw new ConstructorException("while construction a mapping", "found duplicate value key", null);
                }
                mapping[0].put("=",constructObject(value_v));
            } else {
                final Object kk = constructObject(key_v);
                final Object vv = constructObject(value_v);
                if(vv instanceof LinkNode) {
                    addFixer((Node)((LinkNode)vv).getValue(), new RecursiveFixer() {
                            public void replace(Node node, Object real) {
                                mapping[0].put(kk, real);
                            }
                        });
                }
                mapping[0].put(kk,vv);
            }
        }
        if(null != merge) {
            merge.add(mapping[0]);
            mapping[0] = new HashMap();
            for(final Iterator iter = merge.iterator();iter.hasNext();) {
                mapping[0].putAll((Map)iter.next());
            }
        }
        return mapping[0];
    }

    public void addFixer(Node node, RecursiveFixer fixer) {
        List ll = (List)recursiveObjects.get(node);
        if(ll == null) {
            ll = new ArrayList();
            recursiveObjects.put(node, ll);
        }
        ll.add(fixer);
    }

    public Object constructPairs(final Node node) {
        if(!(node instanceof MappingNode)) {
            throw new ConstructorException(null,"expected a mapping node, but found " + node.getClass().getName(), null);
        }
        final List value = new LinkedList();
        final Map vals = (Map)node.getValue();
        for(final Iterator iter = vals.keySet().iterator();iter.hasNext();) {
            final Node key = (Node)iter.next();
            final Node val = (Node)vals.get(key);
            value.add(new Object[]{constructObject(key),constructObject(val)});
        }
        return value;
    }

    public final static YamlConstructor CONSTRUCT_PRIMITIVE = new YamlConstructor() {
            public Object call(final Constructor self, final Node node) {
                return self.constructPrimitive(node);
            }};
    public final static YamlConstructor CONSTRUCT_SCALAR = new YamlConstructor() {
            public Object call(final Constructor self, final Node node) {
                return self.constructScalar(node);
            }};
    public final static YamlConstructor CONSTRUCT_PRIVATE = new YamlConstructor() {
            public Object call(final Constructor self, final Node node) {
                return self.constructPrivateType(node);
            }};
    public final static YamlConstructor CONSTRUCT_SEQUENCE = new YamlConstructor() {
            public Object call(final Constructor self, final Node node) {
                return self.constructSequence(node);
            }};
    public final static YamlConstructor CONSTRUCT_MAPPING = new YamlConstructor() {
            public Object call(final Constructor self, final Node node) {
                return self.constructMapping(node);
            }};

    public static void main(final String[] args) throws Exception {
        final String filename = args[0];
        System.out.println("Reading of file: \"" + filename + "\"");

        final ByteList input = new ByteList(1024);
        final InputStream reader = new FileInputStream(filename);
        byte[] buff = new byte[1024];
        int read = 0;
        while(true) {
            read = reader.read(buff);
            input.append(buff,0,read);
            if(read < 1024) {
                break;
            }
        }
        reader.close();
        final long before = System.currentTimeMillis();
        for(int i=0;i<1;i++) {
            final Constructor ctor = new BaseConstructorImpl(new ComposerImpl(new ParserImpl(new ScannerImpl(input)),new ResolverImpl()));
            for(final Iterator iter = ctor.eachDocument();iter.hasNext();) {
                iter.next();
                //System.out.println(iter.next());
            }
        }
        final long after = System.currentTimeMillis();
        final long time = after-before;
        final double timeS = (after-before)/1000.0;
        System.out.println("Walking through the nodes for the file: " + filename + " took " + time + "ms, or " + timeS + " seconds"); 
    }
}// BaseConstructorImpl
