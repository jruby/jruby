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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
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
package org.jruby.yaml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.regex.Pattern;

import org.jvyamlb.Composer;
import org.jvyamlb.Constructor;
import org.jvyamlb.exceptions.ConstructorException;
import org.jvyamlb.ConstructorImpl;
import org.jvyamlb.SafeConstructorImpl;
import org.jvyamlb.Scanner;
import org.jvyamlb.ScannerImpl;
import org.jvyamlb.ComposerImpl;
import org.jvyamlb.ParserImpl;
import org.jvyamlb.ResolverImpl;
import org.jvyamlb.YAML;

import org.jvyamlb.nodes.Node;
import org.jvyamlb.nodes.LinkNode;


import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyHash;
import org.jruby.RubyString;
import org.jruby.RubyStruct;
import org.jruby.RubyRange;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.util.ByteList;

import org.joda.time.DateTime;
import org.jruby.runtime.ThreadContext;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class JRubyConstructor extends ConstructorImpl {
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

    private final Ruby runtime;

    public JRubyConstructor(final IRubyObject receiver, final Composer composer) {
        this(receiver.getRuntime(), composer);
    }

    public JRubyConstructor(final Ruby runtime, final Composer composer) {
        super(composer);
        this.runtime = runtime;
    }

    public Object constructRubyScalar(final Node node) {
        if(node instanceof org.jvyamlb.nodes.ScalarNode) {
            ByteList sc = (ByteList)super.constructScalar(node);
            if(sc.length() > 1 && sc.charAt(0) == ':' && ((org.jvyamlb.nodes.ScalarNode)node).getStyle() == 0) {
                int first = sc.get(1);
                int last = sc.get(sc.realSize-1);
                if((first == '"' && last == '"') ||
                   (first == '\'' && last == '\'')) {

                    Scanner scn = new ScannerImpl(sc.makeShared(1, sc.realSize-1));
                    Constructor ctor = new JRubyConstructor(runtime, new ComposerImpl(new ParserImpl(scn,YAML.config().version("1.0")),new ResolverImpl()));
                    ctor.checkData();
                    return ((RubyString)ctor.getData()).intern();
                }

                return runtime.newSymbol(new String(sc.bytes, sc.begin+1, sc.realSize-1));
            }

            return RubyString.newString(runtime,(ByteList)super.constructScalar(node));
        } else {
            // Assume it's a mapping node

            Map val = (Map)(constructMapping(node));
            RubyString str = (RubyString)val.get(runtime.newString("str"));

            Map props = new HashMap();
            for(Iterator iter = val.entrySet().iterator();iter.hasNext();) {
                Map.Entry em = (Map.Entry)iter.next();
                if(em.getKey().toString().startsWith("@")) {
                    props.put(em.getKey(),em.getValue());
                    iter.remove();
                }
            }
            for(Iterator iter = props.entrySet().iterator();iter.hasNext();) {
                Map.Entry em = (Map.Entry)iter.next();
                str.instance_variable_set((IRubyObject)em.getKey(),(IRubyObject)em.getValue());
            }

            return str;
        }
    }

    public Object constructPrivateType(final Node node) {
        Object val = null;
        if(node.getValue() instanceof Map) {
            val = constructRubyMapping(node);
        } else if(node.getValue() instanceof List) {
            val = constructRubySequence(node);
        } else {
            val = constructRubyScalar(node);
        }
        return RuntimeHelpers.invoke(
                runtime.getCurrentContext(),
                runtime.fastGetModule("YAML").fastGetConstant("PrivateType"),
                "new", runtime.newString(node.getTag()),(IRubyObject)val);
    }

    public Object constructRubySequence(final Node node) {
        final RubyArray arr = runtime.newArray();
        List l = (List)super.constructSequence(node);
        doRecursionFix(node, arr);
        int i = 0;
        for(Iterator iter = l.iterator();iter.hasNext();i++) {
            Object oo = iter.next();
            if(oo instanceof LinkNode) {
                arr.append(runtime.getNil());
                final IRubyObject ix = runtime.newFixnum(i);
                addFixer((Node)(((LinkNode)oo).getValue()), new RecursiveFixer() {
                        public void replace(Node node, Object real) {
                            arr.aset(ix, (IRubyObject)real);
                        }
                    });
            } else {
                arr.append((IRubyObject)oo);
            }
        }
        return arr;
    }

    public Object constructRubyMapping(final Node node) {
        RubyHash h1 = RubyHash.newHash(runtime, (Map)super.constructMapping(node), runtime.getNil());
        return h1;
    }

    public Object constructRubyPairs(final Node node) {
        return runtime.newArray((List)super.constructPairs(node));
    }

    public static Object constructYamlNull(final Constructor ctor, final Node node) {
        return ((JRubyConstructor)ctor).runtime.getNil();
    }
    
    public static Object constructYamlBool(final Constructor ctor, final Node node) {
        return SafeConstructorImpl.constructYamlBool(ctor,node) == Boolean.TRUE ? ((JRubyConstructor)ctor).runtime.getTrue() : ((JRubyConstructor)ctor).runtime.getFalse();
    }

    public static Object constructYamlOmap(final Constructor ctor, final Node node) {
        Ruby runtime = ((JRubyConstructor)ctor).runtime;
        RubyArray arr = (RubyArray)(runtime.fastGetModule("YAML").fastGetConstant("Omap").callMethod(runtime.getCurrentContext(),"new"));
        List l = (List)ctor.constructSequence(node);
        ctor.doRecursionFix(node, arr);
        for(Iterator iter = l.iterator();iter.hasNext();) {
            IRubyObject v = (IRubyObject)iter.next();
            if(v instanceof RubyHash) {
                arr.concat(((RubyHash)v).to_a());
            } else {
                throw new ConstructorException(null,"Invalid !omap entry: " + l,null);
            }
        }
        return arr;
    }

    public static Object constructYamlPairs(final Constructor ctor, final Node node) {
        return ((JRubyConstructor)ctor).constructRubyPairs(node);
    }

    public static Object constructYamlSet(final Constructor ctor, final Node node) {
        return SafeConstructorImpl.constructYamlSet(ctor,node);
    }

    public static Object constructYamlStr(final Constructor ctor, final Node node) {
        final Object _str = ((JRubyConstructor)ctor).constructRubyScalar(node);
        if(_str instanceof org.jruby.RubyString) {
            final org.jruby.RubyString str = (org.jruby.RubyString)_str;
            return (str.getByteList().realSize == 0 && ((org.jvyamlb.nodes.ScalarNode)node).getStyle() == 0) ? str.getRuntime().getNil() : str;
        }
        return _str;
    }

    public static Object constructYamlSeq(final Constructor ctor, final Node node) {
        return ((JRubyConstructor)ctor).constructRubySequence(node);
    }

    public static Object constructYamlMap(final Constructor ctor, final Node node) {
        return ((JRubyConstructor)ctor).constructRubyMapping(node);
    }

    public static Object constructUndefined(final Constructor ctor, final Node node) {
        throw new ConstructorException(null,"could not determine a constructor for the tag " + node.getTag(),null);
    }

    public static Object constructYamlTimestamp(final Constructor ctor, final Node node) {
        Object[] value = (Object[])SafeConstructorImpl.constructYamlTimestamp(ctor,node);
        DateTime dt = (DateTime)(value[0]);
        org.jruby.RubyTime rt = org.jruby.RubyTime.newTime(((JRubyConstructor)ctor).runtime,dt);
        rt.setUSec(((Integer)value[1]));
        return rt;
    }

    public static Object constructYamlTimestampYMD(final Constructor ctor, final Node node) {
        DateTime dt = (DateTime)(((Object[])SafeConstructorImpl.constructYamlTimestamp(ctor,node))[0]);
        Ruby runtime = ((JRubyConstructor)ctor).runtime;
        return RuntimeHelpers.invoke(runtime.getCurrentContext(), runtime.fastGetClass("Date"), "new", runtime.newFixnum(dt.getYear()),runtime.newFixnum(dt.getMonthOfYear()),runtime.newFixnum(dt.getDayOfMonth()));
    }

    public static Object constructYamlInt(final Constructor ctor, final Node node) {
        return org.jruby.javasupport.JavaUtil.convertJavaToRuby(((JRubyConstructor)ctor).runtime,SafeConstructorImpl.constructYamlInt(ctor,node));
    }
    public static Object constructYamlFloat(final Constructor ctor, final Node node) {
        return ((JRubyConstructor)ctor).runtime.newFloat(((Double)SafeConstructorImpl.constructYamlFloat(ctor,node)).doubleValue());
    }
    public static Object constructYamlBinary(final Constructor ctor, final Node node) {
        Object b = SafeConstructorImpl.constructYamlBinary(ctor,node);
        if(b instanceof byte[]) {
            return RubyString.newString(((JRubyConstructor)ctor).runtime, new ByteList((byte[])b,false));
        } else {
            return ((JRubyConstructor)ctor).runtime.newString((String)b);
        }
    }

    public static Object constructJava(final Constructor ctor, final String pref, final Node node) {
        return SafeConstructorImpl.constructJava(ctor,pref,node);
    }

    public static Object constructRubyException(final Constructor ctor, final String tag, final Node node) {
        final Ruby runtime = ((JRubyConstructor)ctor).runtime;
        RubyModule objClass = runtime.getObject();
        if(tag != null) {
            final String[] nms = tag.split("::");
            try {
                for(int i=0,j=nms.length;i<j;i++) {
                    objClass = (RubyModule)objClass.getConstant(nms[i]);
                }
            } catch(Exception e) {
                // No constant available, so we'll fall back on YAML::Object
                objClass = (RubyClass)runtime.fastGetModule("YAML").fastGetConstant("Object");
                final RubyHash vars = (RubyHash)(((JRubyConstructor)ctor).constructRubyMapping(node));
                return RuntimeHelpers.invoke(runtime.getCurrentContext(), objClass, "new", runtime.newString(tag), vars);
            }
        }
        final RubyClass theCls = (RubyClass)objClass;
        final RubyObject oo = (RubyObject)theCls.getAllocator().allocate(runtime, theCls);
        final Map vars = (Map)(ctor.constructMapping(node));
        ctor.doRecursionFix(node, oo);
        for(final Iterator iter = vars.keySet().iterator();iter.hasNext();) {
            final IRubyObject key = (IRubyObject)iter.next();
            final Object val = vars.get(key);
            if(val instanceof LinkNode) {
                final String KEY = "@" + key.toString();
                ctor.addFixer((Node)(((LinkNode)val).getValue()), new RecursiveFixer() {
                        public void replace(Node node, Object real) {
                            oo.setInstanceVariable(KEY,(IRubyObject)real);
                        }
                    });
            } else {
                oo.setInstanceVariable("@" + key.toString(),(IRubyObject)val);
            }
        }
        return oo;
    }

    public static Object constructRubyStruct(final Constructor ctor, final String tag, final Node node) {
        final Ruby runtime = ((JRubyConstructor)ctor).runtime;
        RubyModule sClass = runtime.fastGetModule("Struct");
        RubyClass struct_type;
        String[] nms = tag.split("::");
        for(int i=0,j=nms.length;i<j && sClass != null;i++) {
            sClass = (RubyModule)sClass.getConstant(nms[i]);
        }

        Map props = new HashMap();
        Map val = (Map)(ctor.constructMapping(node));
        for(Iterator iter = val.entrySet().iterator();iter.hasNext();) {
            Map.Entry em = (Map.Entry)iter.next();
            if(em.getKey().toString().startsWith("@")) {
                props.put(em.getKey(),em.getValue());
                iter.remove();
            }
        }

        // If no such struct exists...
        if(sClass == null) {
            IRubyObject[] params = new IRubyObject[val.size()+1];
            params[0] = runtime.newString(tag);
            int i = 1;
            for(Iterator iter = val.entrySet().iterator();iter.hasNext();i++) {
                Map.Entry em = (Map.Entry)iter.next();
                params[i] = ((RubyString)em.getKey()).intern();
            }
            struct_type = RubyStruct.newInstance(runtime.fastGetModule("Struct"),params,Block.NULL_BLOCK);
        } else {
            struct_type = (RubyClass)sClass;
        }
        IRubyObject st = struct_type.callMethod(runtime.getCurrentContext(),"new");
        RubyArray members = RubyStruct.members(struct_type,Block.NULL_BLOCK);
        for(int i=0,j=members.size();i<j;i++) {
            IRubyObject m = members.eltInternal(i);
            st.callMethod(runtime.getCurrentContext(), m.toString() + "=", (IRubyObject)val.get(m));
        }
        for(Iterator iter = props.entrySet().iterator();iter.hasNext();) {
            Map.Entry em = (Map.Entry)iter.next();
            ((RubyObject)st).instance_variable_set((IRubyObject)em.getKey(),(IRubyObject)em.getValue());
        }
        return st;
    }

    public static Object constructRuby(final Constructor ctor, final RubyClass theCls, final Node node) {
        final Ruby runtime = ((JRubyConstructor)ctor).runtime;
        if(theCls.respondsTo("yaml_new")) {
            final RubyHash vars = (RubyHash)(((JRubyConstructor)ctor).constructRubyMapping(node));
            return RuntimeHelpers.invoke(runtime.getCurrentContext(), theCls, "yaml_new", theCls, runtime.newString(node.getTag()), vars);
        } else {
            final RubyObject oo = (RubyObject)theCls.getAllocator().allocate(runtime, theCls);
            if (oo.respondsTo("yaml_initialize")) {
                RubyHash vars = (RubyHash)(((JRubyConstructor)ctor).constructRubyMapping(node));
                RuntimeHelpers.invoke(runtime.getCurrentContext(), oo, "yaml_initialize", runtime.newString(node.getTag()), vars);
            } else {
                final Map vars = (Map)(ctor.constructMapping(node));
                ctor.doRecursionFix(node, oo);
                for(final Iterator iter = vars.keySet().iterator();iter.hasNext();) {
                    final IRubyObject key = (IRubyObject)iter.next();
                    final Object val = vars.get(key);
                    if(val instanceof LinkNode) {
                        final String KEY = "@" + key.toString();
                        ctor.addFixer((Node)(((LinkNode)val).getValue()), new RecursiveFixer() {
                                public void replace(Node node, Object real) {
                                    oo.setInstanceVariable(KEY,(IRubyObject)real);
                                }
                            });
                    } else {
                        oo.setInstanceVariable("@" + key.toString(),(IRubyObject)val);
                    }
                }
            }
            return oo;
        }
    }

    public static Object constructRuby(final Constructor ctor, final String tag, final Node node) {
        final Ruby runtime = ((JRubyConstructor)ctor).runtime;
        RubyModule objClass = runtime.getObject();
        if(tag != null) {
            final String[] nms = tag.split("::");
            try {
                for(int i=0,j=nms.length;i<j;i++) {
                    objClass = (RubyModule)objClass.getConstant(nms[i]);
                }
            } catch(Exception e) {
                // No constant available, so we'll fall back on YAML::Object
                objClass = (RubyClass)runtime.fastGetModule("YAML").fastGetConstant("Object");
                final RubyHash vars = (RubyHash)(((JRubyConstructor)ctor).constructRubyMapping(node));
                return RuntimeHelpers.invoke(runtime.getCurrentContext(), objClass, "new", runtime.newString(tag), vars);
            }
        }
        final RubyClass theCls = (RubyClass)objClass;
        return constructRuby(ctor,theCls,node);
    }

    public static Object constructRubyRegexp(final Constructor ctor, final Node node) {
        final Ruby runtime = ((JRubyConstructor)ctor).runtime;
        String s1 = ctor.constructScalar(node).toString();
        // This should be fixed in some way
        return runtime.evalScriptlet(s1);
    }

    public static Object constructRubyRange(final Constructor ctor, final Node node) {
        final Ruby runtime = ((JRubyConstructor)ctor).runtime;
        ThreadContext context = runtime.getCurrentContext();
        if (node instanceof org.jvyamlb.nodes.ScalarNode) {
            String s1 = ctor.constructScalar(node).toString();
            String first;
            String second;
            boolean exc = false;
            int ix = -1;
            if((ix = s1.indexOf("...")) != -1) {
                first = s1.substring(0,ix);
                second = s1.substring(ix+3);
                exc = true;
            } else {
                ix = s1.indexOf("..");
                first = s1.substring(0,ix);
                second = s1.substring(ix+2);
            }
            IRubyObject fist = runtime.fastGetModule("YAML").callMethod(context,"load",runtime.newString(first));
            IRubyObject sic = runtime.fastGetModule("YAML").callMethod(context,"load",runtime.newString(second));
            return RubyRange.newRange(runtime, context, fist, sic, exc);
        } else {
            final Map vars = (Map)(ctor.constructMapping(node));
            IRubyObject beg = (IRubyObject)vars.get(runtime.newString("begin"));
            IRubyObject end = (IRubyObject)vars.get(runtime.newString("end"));
            boolean excl = ((IRubyObject)vars.get(runtime.newString("excl"))).isTrue();
            return RubyRange.newRange(runtime, context, beg, end, excl);
        }
    }

    public static Object findAndCreateFromCustomTagging(final Constructor ctor, final Node node) {
        String tag = node.getTag();
        Ruby runtime = ((JRubyConstructor)ctor).runtime;
        IRubyObject _cl = runtime.fastGetModule("YAML").callMethod(runtime.getCurrentContext(), "tagged_classes").callMethod(runtime.getCurrentContext(),"[]", runtime.newString(tag));
        if(!(_cl instanceof RubyClass)) {
            return null;
        }
        RubyClass clazz = (RubyClass)_cl;
        if(clazz != null && !clazz.isNil()) {
            return constructRuby(ctor, clazz, node);
        }
        return null;
    }

    public static Object constructRubyInt(final Constructor ctor, final String tag, final Node node) {
        final Ruby runtime = ((JRubyConstructor)ctor).runtime;
        RubyModule objClass = runtime.getObject();
        if(tag != null) {
            final String[] nms = tag.split("::");
            for(int i=0,j=nms.length;i<j;i++) {
                objClass = (RubyModule)objClass.getConstant(nms[i]);
            }
        }
        final RubyClass theCls = (RubyClass)objClass;
        final RubyObject oo = (RubyObject)theCls.getAllocator().allocate(runtime, theCls);
        final IRubyObject val = (IRubyObject)constructYamlInt(ctor, node);
        oo.callInit(new IRubyObject[]{val},org.jruby.runtime.Block.NULL_BLOCK);
        return oo;
    }

    public static Object constructRubyString(final Constructor ctor, final String tag, final Node node) {
        final Ruby runtime = ((JRubyConstructor)ctor).runtime;
        RubyModule objClass = runtime.getObject();
        if(tag != null) {
            final String[] nms = tag.split("::");
            for(int i=0,j=nms.length;i<j;i++) {
                objClass = (RubyModule)objClass.getConstant(nms[i]);
            }
        }
        final RubyClass theCls = (RubyClass)objClass;
        final RubyObject oo = (RubyObject)theCls.getAllocator().allocate(runtime, theCls);
        final IRubyObject val = (IRubyObject)constructYamlStr(ctor, node);
        oo.callInit(new IRubyObject[]{val},org.jruby.runtime.Block.NULL_BLOCK);
        return oo;
    }

    public static Object constructRubyMap(final Constructor ctor, final String tag, final Node node) {
        final Ruby runtime = ((JRubyConstructor)ctor).runtime;
        RubyModule objClass = runtime.getObject();
        if(tag != null) {
            final String[] nms = tag.split("::");
            for(int i=0,j=nms.length;i<j;i++) {
                objClass = (RubyModule)objClass.getConstant(nms[i]);
            }
        }
        final RubyClass theCls = (RubyClass)objClass;
        final RubyObject oo = (RubyObject)theCls.getAllocator().allocate(runtime, theCls);
        final Map vars = (Map)(ctor.constructMapping(node));
        for(final Iterator iter = vars.keySet().iterator();iter.hasNext();) {
            final IRubyObject key = (IRubyObject)iter.next();
            RuntimeHelpers.invoke(oo.getRuntime().getCurrentContext(), oo, "[]=", key, (IRubyObject)vars.get(key));
        }
        return oo;
    }

    public static Object constructRubySequence(final Constructor ctor, final String tag, final Node node) {
        final Ruby runtime = ((JRubyConstructor)ctor).runtime;
        RubyModule objClass = runtime.getObject();
        if(tag != null) {
            final String[] nms = tag.split("::");
            for(int i=0,j=nms.length;i<j;i++) {
                objClass = (RubyModule)objClass.getConstant(nms[i]);
            }
        }
        final RubyClass theCls = (RubyClass)objClass;
        final RubyObject oo = (RubyObject)theCls.getAllocator().allocate(runtime, theCls);
        final List vars = (List)(ctor.constructSequence(node));
        for(final Iterator iter = vars.iterator();iter.hasNext();) {
            RuntimeHelpers.invoke(oo.getRuntime().getCurrentContext(), oo, "<<", (IRubyObject)iter.next());
        }
        return oo;
    }

    static {
        addConstructor("tag:yaml.org,2002:null",new YamlConstructor() {
                public Object call(final Constructor self, final Node node) {
                    return constructYamlNull(self,node);
                }
            });
        addConstructor("tag:yaml.org,2002:bool",new YamlConstructor() {
                public Object call(final Constructor self, final Node node) {
                    return constructYamlBool(self,node);
                }
            });
        addConstructor("tag:yaml.org,2002:omap",new YamlConstructor() {
                public Object call(final Constructor self, final Node node) {
                    return constructYamlOmap(self,node);
                }
            });
        addConstructor("tag:yaml.org,2002:pairs",new YamlConstructor() {
                public Object call(final Constructor self, final Node node) {
                    return constructYamlPairs(self,node);
                }
            });
        addConstructor("tag:yaml.org,2002:set",new YamlConstructor() {
                public Object call(final Constructor self, final Node node) {
                    return constructYamlSet(self,node);
                }
            });
        addConstructor("tag:yaml.org,2002:int",new YamlConstructor() {
                public Object call(final Constructor self, final Node node) {
                    return constructYamlInt(self,node);
                }
            });
        addConstructor("tag:yaml.org,2002:float",new YamlConstructor() {
                public Object call(final Constructor self, final Node node) {
                    return constructYamlFloat(self,node);
                }
            });
        addConstructor("tag:yaml.org,2002:timestamp",new YamlConstructor() {
                public Object call(final Constructor self, final Node node) {
                    java.util.regex.Matcher match = SafeConstructorImpl.YMD_REGEXP.matcher(node.getValue().toString());
                    if(match.matches()) {
                        return constructYamlTimestampYMD(self,node);
                    } else {
                        return constructYamlTimestamp(self,node);
                    }
                }
            });
        addConstructor("tag:yaml.org,2002:timestamp#ymd",new YamlConstructor() {
                public Object call(final Constructor self, final Node node) {
                    return constructYamlTimestampYMD(self,node);
                }
            });
        addConstructor("tag:yaml.org,2002:str",new YamlConstructor() {
                public Object call(final Constructor self, final Node node) {
                    return constructYamlStr(self,node);
                }
            });
        addConstructor("tag:yaml.org,2002:binary",new YamlConstructor() {
                public Object call(final Constructor self, final Node node) {
                    return constructYamlBinary(self,node);
                }
            });
        addConstructor("tag:yaml.org,2002:seq",new YamlConstructor() {
                public Object call(final Constructor self, final Node node) {
                    return constructYamlSeq(self,node);
                }
            });
        addConstructor("tag:yaml.org,2002:map",new YamlConstructor() {
                public Object call(final Constructor self, final Node node) {
                    return constructYamlMap(self,node);
                }
            });
        addConstructor("tag:ruby.yaml.org,2002:range",new YamlConstructor() {
                public Object call(final Constructor self, final Node node) {
                    return constructRubyRange(self,node);
                }
            });
        addConstructor("tag:ruby.yaml.org,2002:regexp",new YamlConstructor() {
                public Object call(final Constructor self, final Node node) {
                    return constructRubyRegexp(self,node);
                }
            });
        addConstructor(null,new YamlConstructor() {
                public Object call(final Constructor self, final Node node) {
                    Object v1 = findAndCreateFromCustomTagging(self, node);
                    if(null != v1) {
                        return v1;
                    }
                    return self.constructPrivateType(node);
                }
            });
        addMultiConstructor("tag:yaml.org,2002:map:",new YamlMultiConstructor() {
                public Object call(final Constructor self, final String pref, final Node node) {
                    return constructRubyMap(self,pref,node);
                }
            });
        addMultiConstructor("tag:ruby.yaml.org,2002:hash:",new YamlMultiConstructor() {
                public Object call(final Constructor self, final String pref, final Node node) {
                    return constructRubyMap(self,pref,node);
                }
            });
        addMultiConstructor("tag:yaml.org,2002:int:",new YamlMultiConstructor() {
                public Object call(final Constructor self, final String pref, final Node node) {
                    return constructRubyInt(self,pref,node);
                }
            });
        addMultiConstructor("tag:yaml.org,2002:seq:",new YamlMultiConstructor() {
                public Object call(final Constructor self, final String pref, final Node node) {
                    return constructRubySequence(self,pref,node);
                }
            });
        addMultiConstructor("tag:ruby.yaml.org,2002:array:",new YamlMultiConstructor() {
                public Object call(final Constructor self, final String pref, final Node node) {
                    return constructRubySequence(self,pref,node);
                }
            });
        addMultiConstructor("tag:yaml.org,2002:str:",new YamlMultiConstructor() {
                public Object call(final Constructor self, final String pref, final Node node) {
                    return constructRubyString(self,pref,node);
                }
            });
        addMultiConstructor("tag:ruby.yaml.org,2002:string:",new YamlMultiConstructor() {
                public Object call(final Constructor self, final String pref, final Node node) {
                    return constructRubyString(self,pref,node);
                }
            });
        addMultiConstructor("tag:yaml.org,2002:ruby/object:",new YamlMultiConstructor() {
                public Object call(final Constructor self, final String pref, final Node node) {
                    return constructRuby(self,pref,node);
                }
            });
        addMultiConstructor("tag:ruby.yaml.org,2002:object:",new YamlMultiConstructor() {
                public Object call(final Constructor self, final String pref, final Node node) {
                    return constructRuby(self,pref,node);
                }
            });
        addMultiConstructor("tag:yaml.org,2002:java/object:",new YamlMultiConstructor() {
                public Object call(final Constructor self, final String pref, final Node node) {
                    return constructJava(self,pref,node);
                }
            });
        addMultiConstructor("tag:java.yaml.org,2002:object:",new YamlMultiConstructor() {
                public Object call(final Constructor self, final String pref, final Node node) {
                    return constructJava(self,pref,node);
                }
            });
        addMultiConstructor("tag:ruby.yaml.org,2002:struct:",new YamlMultiConstructor() {
                public Object call(final Constructor self, final String pref, final Node node) {
                    return constructRubyStruct(self,pref,node);
                }
            });
        addMultiConstructor("tag:ruby.yaml.org,2002:exception:",new YamlMultiConstructor() {
                public Object call(final Constructor self, final String pref, final Node node) {
                    return constructRuby(self,pref,node);
                }
            });
    }
}// JRubyConstructor
