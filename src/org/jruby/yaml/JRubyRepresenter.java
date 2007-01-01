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
 * $Id: $
 */
package org.jruby.yaml;

import java.io.IOException;

import org.jruby.RubyArray;
import org.jruby.RubyHash;

import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.javasupport.JavaEmbedUtils;

import org.jvyaml.SafeRepresenterImpl;
import org.jvyaml.Serializer;
import org.jvyaml.Representer;
import org.jvyaml.YAMLConfig;
import org.jvyaml.YAMLNodeCreator;
import org.jvyaml.nodes.Node;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 * @version $Revision: $
 */
public class JRubyRepresenter extends SafeRepresenterImpl {
    public JRubyRepresenter(final Serializer serializer, final YAMLConfig opts) {
        super(serializer,opts);
    }

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
        if(mapping instanceof RubyHash) {
            mapping = ((RubyHash)mapping).getValueMap();
        }
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
        if(null == style || style.length() == 0) {
            return scalar(tag,val,(char)0);
        } else {
            return scalar(tag,val,style.charAt(0));
        }
    }

    protected boolean ignoreAliases(final Object data) {
        return (data instanceof IRubyObject && ((IRubyObject)data).isNil()) || super.ignoreAliases(data);
    }

    public static class IRubyObjectYAMLNodeCreator implements YAMLNodeCreator {
        private final IRubyObject data;

        public IRubyObjectYAMLNodeCreator(final Object data) {
            this.data = (IRubyObject)data;
        }

        public String taguri() {
            return data.callMethod(data.getRuntime().getCurrentContext(), "taguri").toString();
        }

        public Node toYamlNode(final Representer representer) throws IOException {
            Object val = data.callMethod(data.getRuntime().getCurrentContext(), "to_yaml_node", JavaEmbedUtils.javaToRuby(data.getRuntime(),representer));
            if(val instanceof Node) {
                return (Node)val;
            } else if(val instanceof IRubyObject) {
                return (Node)JavaEmbedUtils.rubyToJava(data.getRuntime(),(IRubyObject)val,Node.class);
            }
            return null;
        }
    }
}// JRubyRepresenter
