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
/**
 * $Id$
 */
package org.jruby.yaml;

import java.util.Date;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.regex.Pattern;

import org.jvyaml.Composer;
import org.jvyaml.Constructor;
import org.jvyaml.ConstructorException;
import org.jvyaml.ConstructorImpl;
import org.jvyaml.SafeConstructorImpl;

import org.jvyaml.nodes.Node;

import org.jruby.IRuby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyHash;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 * @version $Revision$
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

    private final IRuby runtime;

    public JRubyConstructor(final IRubyObject receiver, final Composer composer) {
        super(composer);
        this.runtime = receiver.getRuntime();
    }

    public Object constructRubyScalar(final Node node) {
        return runtime.newString((String)super.constructScalar(node));
    }

    public Object constructRubySequence(final Node node) {
        return runtime.newArray((List)super.constructSequence(node));
    }

    public Object constructRubyMapping(final Node node) {
        return RubyHash.newHash(runtime,(Map)super.constructMapping(node),runtime.getNil());
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
        return ((JRubyConstructor)ctor).constructRubyPairs(node);
    }

    public static Object constructYamlPairs(final Constructor ctor, final Node node) {
        return constructYamlOmap(ctor,node);
    }

    public static Object constructYamlSet(final Constructor ctor, final Node node) {
        return SafeConstructorImpl.constructYamlSet(ctor,node);
    }

    public static Object constructYamlStr(final Constructor ctor, final Node node) {
        final org.jruby.RubyString str = (org.jruby.RubyString)((JRubyConstructor)ctor).constructRubyScalar(node);
        return (str.getValue().length() == 0 && ((org.jvyaml.nodes.ScalarNode)node).getStyle() == 0) ? str.getRuntime().getNil() : str;
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
        return ((JRubyConstructor)ctor).runtime.newTime(((Date)SafeConstructorImpl.constructYamlTimestamp(ctor,node)).getTime()).callMethod(((JRubyConstructor)ctor).runtime.getCurrentContext(),"utc");
    }

    public static Object constructYamlTimestampYMD(final Constructor ctor, final Node node) {
        Date d = (Date)SafeConstructorImpl.constructYamlTimestamp(ctor,node);
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        IRuby runtime = ((JRubyConstructor)ctor).runtime;
        return runtime.getClass("Date").callMethod(runtime.getCurrentContext(),"new",new IRubyObject[]{runtime.newFixnum(c.get(Calendar.YEAR)),runtime.newFixnum(c.get(Calendar.MONTH)+1),runtime.newFixnum(c.get(Calendar.DAY_OF_MONTH)),});
    }

    public static Object constructYamlInt(final Constructor ctor, final Node node) {
        return ((JRubyConstructor)ctor).runtime.newFixnum(((Long)SafeConstructorImpl.constructYamlInt(ctor,node)).longValue());
    }
    public static Object constructYamlFloat(final Constructor ctor, final Node node) {
        return ((JRubyConstructor)ctor).runtime.newFloat(((Double)SafeConstructorImpl.constructYamlFloat(ctor,node)).doubleValue());
    }
    public static Object constructYamlBinary(final Constructor ctor, final Node node) {
        return ((JRubyConstructor)ctor).runtime.newString(((String)SafeConstructorImpl.constructYamlBinary(ctor,node)));
    }
    public static Object constructJava(final Constructor ctor, final String pref, final Node node) {
        return SafeConstructorImpl.constructJava(ctor,pref,node);
    }
    public static Object constructRuby(final Constructor ctor, final String tag, final Node node) {
        final IRuby runtime = ((JRubyConstructor)ctor).runtime;
        RubyModule objClass = runtime.getModule("Object");
        if(tag != null) {
            final String[] nms = tag.split("::");
            for(int i=0,j=nms.length;i<j;i++) {
                objClass = (RubyModule)objClass.getConstant(nms[i]);
            }
        }
        final RubyClass theCls = (RubyClass)objClass;
        final RubyObject oo = (RubyObject)theCls.allocate();
        final Map vars = (Map)(ctor.constructMapping(node));
        for(final Iterator iter = vars.keySet().iterator();iter.hasNext();) {
            final IRubyObject key = (IRubyObject)iter.next();
            oo.setInstanceVariable("@" + key.toString(),(IRubyObject)vars.get(key));
        }
        return oo;
    }

    public static Object constructRubyMap(final Constructor ctor, final String tag, final Node node) {
        final IRuby runtime = ((JRubyConstructor)ctor).runtime;
        RubyModule objClass = runtime.getModule("Object");
        if(tag != null) {
            final String[] nms = tag.split("::");
            for(int i=0,j=nms.length;i<j;i++) {
                objClass = (RubyModule)objClass.getConstant(nms[i]);
            }
        }
        final RubyClass theCls = (RubyClass)objClass;
        final RubyObject oo = (RubyObject)theCls.allocate();
        final Map vars = (Map)(ctor.constructMapping(node));
        for(final Iterator iter = vars.keySet().iterator();iter.hasNext();) {
            final IRubyObject key = (IRubyObject)iter.next();
            oo.callMethod(oo.getRuntime().getCurrentContext(),"[]=", new IRubyObject[]{key,(IRubyObject)vars.get(key)});
        }
        return oo;
    }

    public static Object constructRubySequence(final Constructor ctor, final String tag, final Node node) {
        final IRuby runtime = ((JRubyConstructor)ctor).runtime;
        RubyModule objClass = runtime.getModule("Object");
        if(tag != null) {
            final String[] nms = tag.split("::");
            for(int i=0,j=nms.length;i<j;i++) {
                objClass = (RubyModule)objClass.getConstant(nms[i]);
            }
        }
        final RubyClass theCls = (RubyClass)objClass;
        final RubyObject oo = (RubyObject)theCls.allocate();
        final List vars = (List)(ctor.constructSequence(node));
        for(final Iterator iter = vars.iterator();iter.hasNext();) {
            oo.callMethod(oo.getRuntime().getCurrentContext(),"<<", new IRubyObject[]{(IRubyObject)iter.next()});;
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
                    return constructYamlTimestamp(self,node);
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
        addConstructor(null,new YamlConstructor() {
                public Object call(final Constructor self, final Node node) {
                    return self.constructPrivateType(node);
                }
            });
        addMultiConstructor("tag:yaml.org,2002:map:",new YamlMultiConstructor() {
                public Object call(final Constructor self, final String pref, final Node node) {
                    return constructRubyMap(self,pref,node);
                }
            });
        addMultiConstructor("tag:yaml.org,2002:seq:",new YamlMultiConstructor() {
                public Object call(final Constructor self, final String pref, final Node node) {
                    return constructRubySequence(self,pref,node);
                }
            });
        addMultiConstructor("tag:yaml.org,2002:ruby/object:",new YamlMultiConstructor() {
                public Object call(final Constructor self, final String pref, final Node node) {
                    return constructRuby(self,pref,node);
                }
            });
        addMultiConstructor("tag:yaml.org,2002:java/object:",new YamlMultiConstructor() {
                public Object call(final Constructor self, final String pref, final Node node) {
                    return constructJava(self,pref,node);
                }
            });
    }
}// JRubyConstructor

