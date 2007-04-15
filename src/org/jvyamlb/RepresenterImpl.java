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

import java.io.IOException;
import java.io.Serializable;

import java.lang.reflect.Method;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Iterator;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Set;
import java.util.Date;
import java.util.Calendar;

import org.jvyamlb.nodes.Node;
import org.jvyamlb.nodes.LinkNode;
import org.jvyamlb.nodes.CollectionNode;
import org.jvyamlb.nodes.MappingNode;
import org.jvyamlb.nodes.ScalarNode;
import org.jvyamlb.nodes.SequenceNode;

import org.jruby.RubyHash;
import org.jruby.util.ByteList;
import org.jruby.util.collections.IntHashMap;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class RepresenterImpl implements Representer {
    private final Serializer serializer;
    private final char defaultStyle;
    private final Map representedObjects;
    private final Map links;

    public RepresenterImpl(final Serializer serializer, final YAMLConfig opts) {
        this.serializer = serializer;
        this.defaultStyle = opts.useDouble() ? '"' : (opts.useSingle() ? '\'' : 0);
        this.representedObjects = new IdentityHashMap();
        this.links = new IdentityHashMap();
    }

    private Node representData(final Object data) throws IOException {
        Node node = null;

        boolean ignoreAlias = ignoreAliases(data);

        if(!ignoreAlias) {
            if(this.representedObjects.containsKey(data)) {
                node = (Node)this.representedObjects.get(data);
                if(null == node) {
                    node = new LinkNode();
                    List ll = (List)links.get(data);
                    if(ll == null) {
                        ll = new ArrayList();
                        links.put(data,ll);
                    }
                    ll.add(node);
                }
                return node;
            }
            this.representedObjects.put(data,null);
        }

        node = getNodeCreatorFor(data).toYamlNode(this);

        if(!ignoreAlias) {
            this.representedObjects.put(data,node);
            List ll = (List)this.links.remove(data);
            if(ll != null) {
                for(Iterator iter = ll.iterator();iter.hasNext();) {
                    ((LinkNode)iter.next()).setAnchor(node);
                }
            }
        }
        return node;
    }

    public Node scalar(final String tag, final ByteList value, char style) throws IOException {
        return representScalar(tag,value,style);
    }

    public Node representScalar(final String tag, final ByteList value, char style) throws IOException {
        char realStyle = style == 0 ? this.defaultStyle : style;
        return new ScalarNode(tag,value,style);
    }

    public Node seq(final String tag, final List sequence, final boolean flowStyle) throws IOException {
        return representSequence(tag,sequence,flowStyle);
    }

    public Node representSequence(final String tag, final List sequence, final boolean flowStyle) throws IOException {
        List value = new ArrayList(sequence.size());
        for(final Iterator iter = sequence.iterator();iter.hasNext();) {
            value.add(representData(iter.next()));
        }
        return new SequenceNode(tag,value,flowStyle);
    }

    public Node map(final String tag, final Map mapping, final boolean flowStyle) throws IOException {
        return representMapping(tag,mapping,flowStyle);
    }

    public Node representMapping(final String tag, final Map mapping, final boolean flowStyle) throws IOException {
        Map value = new HashMap();
        final Iterator iter = (mapping instanceof RubyHash) ? ((RubyHash)mapping).directEntrySet().iterator() : mapping.entrySet().iterator();
        while(iter.hasNext()) {
            Map.Entry entry = (Map.Entry)iter.next();
            value.put(representData(entry.getKey()),representData(entry.getValue()));
        }
        return new MappingNode(tag,value,flowStyle);
    }

    public void represent(final Object data) throws IOException {
        Node node = representData(data);
        this.serializer.serialize(node);
        this.representedObjects.clear();
    }

    protected boolean ignoreAliases(final Object data) {
        return false;
    }

    protected YAMLNodeCreator getNodeCreatorFor(final Object data) {
        if(data instanceof YAMLNodeCreator) {
            return (YAMLNodeCreator)data;
        } else if(data instanceof Map) {
            return new MappingYAMLNodeCreator(data);
        } else if(data instanceof List) {
            return new SequenceYAMLNodeCreator(data);
        } else if(data instanceof Set) {
            return new SetYAMLNodeCreator(data);
        } else if(data instanceof Date) {
            return new DateYAMLNodeCreator(data);
        } else if(data instanceof String) {
            return new StringYAMLNodeCreator(data);
        } else if(data instanceof ByteList) {
            return new ByteListYAMLNodeCreator(data);
        } else if(data instanceof Number) {
            return new NumberYAMLNodeCreator(data);
        } else if(data instanceof Boolean) {
            return new ScalarYAMLNodeCreator("tag:yaml.org,2002:bool",data);
        } else if(data == null) {
            return new ScalarYAMLNodeCreator("tag:yaml.org,2002:null","");
        } else if(data.getClass().isArray()) {
            return new ArrayYAMLNodeCreator(data);
        } else { // Fallback, handles JavaBeans and other
            return new JavaBeanYAMLNodeCreator(data);
        }
    }

    public static class DateYAMLNodeCreator implements YAMLNodeCreator {
        private final Date data;
        public DateYAMLNodeCreator(final Object data) {
            this.data = (Date)data;
        }

        public String taguri() {
            return "tag:yaml.org,2002:timestamp";
        }

        private static DateFormat dateOutput = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        private static DateFormat dateOutputUsec = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z");
        public Node toYamlNode(final Representer representer) throws IOException {
            final Calendar c = Calendar.getInstance();
            c.setTime(data);
            String out = null;
            if(c.get(Calendar.MILLISECOND) != 0) {
                out = dateOutputUsec.format(data);
            } else {
                out = dateOutput.format(data);
            }
            out = out.substring(0, 23) + ":" + out.substring(23);
            return representer.scalar(taguri(), ByteList.create(out), (char)0);
        }
    }

    public static class SetYAMLNodeCreator implements YAMLNodeCreator {
        private final Set data;
        public SetYAMLNodeCreator(final Object data) {
            this.data = (Set)data;
        }

        public String taguri() {
            return "tag:yaml.org,2002:set";
        }

        public Node toYamlNode(final Representer representer) throws IOException {
            final Map entries = new HashMap();
            for(final Iterator iter = data.iterator();iter.hasNext();) {
                entries.put(iter.next(),null);
            }
            return representer.map(taguri(), entries, false);
        }
    }

    public static class ArrayYAMLNodeCreator implements YAMLNodeCreator {
        private final Object data;
        public ArrayYAMLNodeCreator(final Object data) {
            this.data = data;
        }

        public String taguri() {
            return "tag:yaml.org,2002:seq";
        }

        public Node toYamlNode(final Representer representer) throws IOException {
            final int l = java.lang.reflect.Array.getLength(data);
            final List lst = new ArrayList(l);
            for(int i=0;i<l;i++) {
                lst.add(java.lang.reflect.Array.get(data,i));
            }
            return representer.seq(taguri(), lst, false);
        }
    }

    public static class NumberYAMLNodeCreator implements YAMLNodeCreator {
        private final Number data;
        public NumberYAMLNodeCreator(final Object data) {
            this.data = (Number)data;
        }

        public String taguri() {
            if(data instanceof Float || data instanceof Double || data instanceof java.math.BigDecimal) {
                return "tag:yaml.org,2002:float";
            } else {
                return "tag:yaml.org,2002:int";
            }
        }

        public Node toYamlNode(Representer representer) throws IOException {
            String str = data.toString();
            if(str.equals("Infinity")) {
                str = ".inf";
            } else if(str.equals("-Infinity")) {
                str = "-.inf";
            } else if(str.equals("NaN")) {
                str = ".nan";
            }
            return representer.scalar(taguri(), ByteList.create(str), (char)0);
        }
    }

    public static class ScalarYAMLNodeCreator implements YAMLNodeCreator {
        private final String tag;
        private final Object data;
        public ScalarYAMLNodeCreator(final String tag, final Object data) {
            this.tag = tag;
            this.data = data;
        }

        public String taguri() {
            return this.tag;
        }

        public Node toYamlNode(Representer representer) throws IOException {
            return representer.scalar(taguri(), ByteList.create(data.toString()), (char)0);
        }
    }

    public static class StringYAMLNodeCreator implements YAMLNodeCreator {
        private final Object data;
        public StringYAMLNodeCreator(final Object data) {
            this.data = data;
        }

        public String taguri() {
            if(data instanceof String) {
                return "tag:yaml.org,2002:str";
            } else {
                return "tag:yaml.org,2002:str:"+data.getClass().getName();
            }
        }

        public Node toYamlNode(Representer representer) throws IOException {
            return representer.scalar(taguri(), ByteList.create(data.toString()), (char)0);
        }
    }

    public static class ByteListYAMLNodeCreator implements YAMLNodeCreator {
        private final Object data;
        public ByteListYAMLNodeCreator(final Object data) {
            this.data = data;
        }

        public String taguri() {
            return "tag:yaml.org,2002:str";
        }

        public Node toYamlNode(Representer representer) throws IOException {
            return representer.scalar(taguri(), (ByteList)data, (char)0);
        }
    }

    public static class SequenceYAMLNodeCreator implements YAMLNodeCreator {
        private final List data;
        public SequenceYAMLNodeCreator(final Object data) {
            this.data = (List)data;
        }

        public String taguri() {
            if(data instanceof ArrayList) {
                return "tag:yaml.org,2002:seq";
            } else {
                return "tag:yaml.org,2002:seq:"+data.getClass().getName();
            }
        }

        public Node toYamlNode(Representer representer) throws IOException {
            return representer.seq(taguri(), data, false);
        }
    }

    public static class MappingYAMLNodeCreator implements YAMLNodeCreator {
        private final Map data;
        public MappingYAMLNodeCreator(final Object data) {
            this.data = (Map)data;
        }

        public String taguri() {
            if(data instanceof HashMap) {
                return "tag:yaml.org,2002:map";
            } else {
                return "tag:yaml.org,2002:map:"+data.getClass().getName();
            }
        }

        public Node toYamlNode(Representer representer) throws IOException {
            return representer.map(taguri(), data, false);
        }
    }

    public static class JavaBeanYAMLNodeCreator implements YAMLNodeCreator {
        private final Object data;
        public JavaBeanYAMLNodeCreator(final Object data) {
            this.data = data;
        }

        public String taguri() {
            return "!java/object:" + data.getClass().getName();
        }

        public Node toYamlNode(Representer representer) throws IOException {
            final Map values = new HashMap();
            final Method[] ems = data.getClass().getMethods();
            for(int i=0,j=ems.length;i<j;i++) {
                if(ems[i].getParameterTypes().length == 0) {
                    final String name = ems[i].getName();
                    if(name.equals("getClass")) {
                        continue;
                    }
                    String pname = null;
                    if(name.startsWith("get")) {
                        pname = "" + Character.toLowerCase(name.charAt(3)) + name.substring(4);
                    } else if(name.startsWith("is")) {
                        pname = "" + Character.toLowerCase(name.charAt(2)) + name.substring(3);
                    }
                    if(null != pname) {
                        try {
                            values.put(pname, ems[i].invoke(data,null));
                        } catch(final Exception exe) {
                            values.put(pname, null);
                        }
                    }
                }
            }
            return representer.map(taguri(),values,false);
        }
    }

    public static void main(final String[] args) throws IOException {
        final YAMLConfig cfg = YAML.config();
        final Serializer s = new SerializerImpl(new EmitterImpl(System.out,cfg),new ResolverImpl(),cfg);
        s.open();
        final Representer r = new RepresenterImpl(s, cfg);
        final Map test1 = new HashMap();
        final List test1Val = new LinkedList();
        test1Val.add("hello");
        test1Val.add(Boolean.TRUE);
        test1Val.add(new Integer(31337));
        test1.put("val1",test1Val);
        final List test2Val = new ArrayList();
        test2Val.add("hello");
        test2Val.add(Boolean.FALSE);
        test2Val.add(new Integer(31337));
        test1.put("val2",test2Val);
        test1.put("afsdf", "hmm");
        TestJavaBean bean1 = new TestJavaBean();
        bean1.setName("Ola");
        bean1.setSurName("Bini");
        bean1.setAge(24);
        test1.put(new Integer(25),bean1);
        r.represent(test1);
        s.close();
    }
    private static class TestJavaBean implements Serializable {
        private String val1;
        private String val2;
        private int val3;
        public TestJavaBean() {
        }
        public void setName(final String name) {
            this.val1 = name;
        }
        public String getName() {
            return this.val1;
        }

        public void setSurName(final String sname) {
            this.val2 = sname;
        }
        public String getSurName() {
            return this.val2;
        }

        public void setAge(final int age) {
            this.val3 = age;
        }
        public int getAge() {
            return this.val3;
        }
    }
}// RepresenterImpl

