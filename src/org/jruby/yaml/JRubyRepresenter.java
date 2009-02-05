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

import java.io.IOException;
import java.io.ByteArrayInputStream;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyHash;
import org.jruby.RubyArray;
import org.jruby.RubyString;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.javasupport.JavaEmbedUtils;

import org.jruby.runtime.ThreadContext;
import org.jvyamlb.SafeRepresenterImpl;
import org.jvyamlb.Serializer;
import org.jvyamlb.Representer;
import org.jvyamlb.YAMLConfig;
import org.jvyamlb.YAMLNodeCreator;
import org.jvyamlb.Representer;
import org.jvyamlb.Constructor;
import org.jvyamlb.ParserImpl;
import org.jvyamlb.Scanner;
import org.jvyamlb.ScannerImpl;
import org.jvyamlb.Composer;
import org.jvyamlb.ComposerImpl;
import org.jvyamlb.PositioningScannerImpl;
import org.jvyamlb.PositioningComposerImpl;
import org.jvyamlb.Serializer;
import org.jvyamlb.Resolver;
import org.jvyamlb.ResolverImpl;
import org.jvyamlb.EmitterImpl;
import org.jvyamlb.exceptions.YAMLException;
import org.jvyamlb.YAMLConfig;
import org.jvyamlb.YAML;
import org.jvyamlb.PositioningScanner;
import org.jvyamlb.Positionable;
import org.jvyamlb.Position;
import org.jvyamlb.nodes.Node;
import org.jvyamlb.nodes.ScalarNode;
import org.jvyamlb.nodes.MappingNode;

import org.jruby.util.ByteList;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class JRubyRepresenter extends SafeRepresenterImpl {
    public JRubyRepresenter(final Serializer serializer, final YAMLConfig opts) {
        super(serializer,opts);
    }

    @Override
    protected YAMLNodeCreator getNodeCreatorFor(final Object data) {
        if(data instanceof YAMLNodeCreator) {
            return (YAMLNodeCreator)data;
        } else if(data instanceof IRubyObject) {
            return new IRubyObjectYAMLNodeCreator(data);
        } else {
            return super.getNodeCreatorFor(data);
        }
    }

    public Node map(String tag, java.util.Map mapping, Object flowStyle) throws IOException {
        if(null == flowStyle) {
            return map(tag,mapping,false);
        } else {
            return map(tag,mapping,true);
        }
    }
    public Node seq(String tag, java.util.List sequence, Object flowStyle) throws IOException {
        if(sequence instanceof RubyArray) {
            sequence = ((RubyArray)sequence).getList();
        }

        if(null == flowStyle) {
            return seq(tag,sequence,false);
        } else {
            return seq(tag,sequence,true);
        }
    }

    public Node scalar(String tag, String val, String style) throws IOException {
        return scalar(tag, ByteList.create(val), style);
    }

    public Node scalar(String tag, ByteList val, String style) throws IOException {
        if(null == style || style.length() == 0) {
            return scalar(tag,val,(char)0);
        } else {
            return scalar(tag,val,style.charAt(0));
        }
    }

    @Override
    public Node representMapping(final String tag, final Map mapping, final boolean flowStyle) throws IOException {
        Map value = new HashMap();
        final Iterator iter = (mapping instanceof RubyHash) ? ((RubyHash)mapping).directEntrySet().iterator() : mapping.entrySet().iterator();
        while(iter.hasNext()) {
            Map.Entry entry = (Map.Entry)iter.next();
            value.put(representData(entry.getKey()),representData(entry.getValue()));
        }
        return new MappingNode(tag,value,flowStyle);
    }

    @Override
    protected boolean ignoreAliases(final Object data) {
        return (data instanceof IRubyObject && ((IRubyObject)data).isNil()) || super.ignoreAliases(data);
    }

    public static class IRubyObjectYAMLNodeCreator implements YAMLNodeCreator {
        private final IRubyObject data;
        private final RubyClass outClass;

        public IRubyObjectYAMLNodeCreator(final Object data) {
            this.data = (IRubyObject)data;
            this.outClass = ((RubyClass)(((RubyModule)this.data.getRuntime().getModule("YAML").getConstant("JvYAML"))).getConstant("Node"));
        }

        public String taguri() {
            return data.callMethod(data.getRuntime().getCurrentContext(), "taguri").toString();
        }

        public Node toYamlNode(final Representer representer) throws IOException {
            Ruby runtime = data.getRuntime();
            ThreadContext context = runtime.getCurrentContext();

            if(data.getMetaClass().searchMethod("to_yaml") == runtime.getObjectToYamlMethod() ||
               data.getMetaClass().searchMethod("to_yaml").isUndefined() // In this case, hope that it works out correctly when calling to_yaml_node. Rails does this.
               ) {
                // to_yaml have not been overridden
                Object val = data.callMethod(context, "to_yaml_node", JavaEmbedUtils.javaToRuby(runtime, representer));
                if(val instanceof Node) {
                    return (Node)val;
                } else if(val instanceof IRubyObject) {
                    return (Node)JavaEmbedUtils.rubyToJava((IRubyObject) val);
                }
            } else {
                IRubyObject val = data.callMethod(context, "to_yaml", JavaEmbedUtils.javaToRuby(runtime, representer));

                if(!outClass.isInstance(val)) {
                    if(val instanceof RubyString && ((RubyString)val).getByteList().length() > 4) {
                        ByteList bl = ((RubyString)val).getByteList();
                        int subst = 4;
                        if(bl.get(4) == '\n') subst++;
                        int len = (bl.length()-subst)-1;
                        Resolver res = new ResolverImpl();
                        res.descendResolver(null, null);
                        String detectedTag = res.resolve(ScalarNode.class,bl.makeShared(subst, len),new boolean[]{true,false});
                        return ((JRubyRepresenter)representer).scalar(detectedTag, bl.makeShared(subst, len), null);
                    }

                    throw runtime.newTypeError("wrong argument type " + val.getMetaClass().getRealClass() + " (expected YAML::JvYAML::Node)");
                } else {
                    IRubyObject value = val.callMethod(context, "value");
                    IRubyObject style = val.callMethod(context, "style");
                    IRubyObject type_id = val.callMethod(context, "type_id");
                    String s = null;
                    if(!style.isNil()) {
                        s = style.toString();
                    }
                    String t = type_id.toString();
                    if(value instanceof RubyHash) {
                        return ((JRubyRepresenter)representer).map(t, (RubyHash)value, s);
                    } else if(value instanceof RubyArray) {
                        return ((JRubyRepresenter)representer).seq(t, (RubyArray)value, s);
                    } else {
                        return ((JRubyRepresenter)representer).scalar(t, ((RubyString)value).getByteList(), s);
                    }
                }
            }

            return null;
        }
    }
}// JRubyRepresenter
