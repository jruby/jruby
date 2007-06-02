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
 * Copyright (C) 2007 Ola Bini <ola.bini@gmail.com>
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
package org.jruby;

import java.io.IOException;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.regex.Pattern;

import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.javasupport.JavaEmbedUtils;

import org.jruby.runtime.MethodIndex;

import org.jruby.yaml.JRubyRepresenter;
import org.jruby.yaml.JRubyConstructor;
import org.jruby.yaml.JRubySerializer;
import org.jruby.util.IOInputStream;
import org.jruby.util.IOOutputStream;

import org.jvyamlb.Representer;
import org.jvyamlb.Constructor;
import org.jvyamlb.ParserImpl;
import org.jvyamlb.Scanner;
import org.jvyamlb.ScannerImpl;
import org.jvyamlb.ComposerImpl;
import org.jvyamlb.Serializer;
import org.jvyamlb.ResolverImpl;
import org.jvyamlb.EmitterImpl;
import org.jvyamlb.YAMLConfig;
import org.jvyamlb.YAML;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class RubyYAML {
    public static RubyModule createYAMLModule(Ruby runtime) {
        runtime.getModule("Kernel").callMethod(runtime.getCurrentContext(),"require", runtime.newString("date"));
        RubyModule result = runtime.defineModule("YAML");
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyYAML.class);

        result.defineFastModuleFunction("dump",callbackFactory.getFastOptSingletonMethod("dump"));
        result.defineFastModuleFunction("dump_all",callbackFactory.getFastOptSingletonMethod("dump_all"));
        result.defineFastModuleFunction("load",callbackFactory.getFastSingletonMethod("load", IRubyObject.class));
        result.defineFastModuleFunction("load_file",callbackFactory.getFastSingletonMethod("load_file", IRubyObject.class));
        result.defineModuleFunction("each_document",callbackFactory.getSingletonMethod("each_document", IRubyObject.class));
        result.defineModuleFunction("load_documents",callbackFactory.getSingletonMethod("load_documents", IRubyObject.class));
        result.defineFastModuleFunction("load_stream",callbackFactory.getFastSingletonMethod("load_stream", IRubyObject.class));
        result.defineFastModuleFunction("dump_stream",callbackFactory.getFastOptSingletonMethod("dump_stream"));
        result.defineModuleFunction("quick_emit_node",callbackFactory.getOptSingletonMethod("quick_emit_node"));
        result.defineFastModuleFunction("quick_emit",callbackFactory.getFastOptSingletonMethod("quick_emit"));

        RubyClass obj = runtime.getClass("Object");
        RubyClass clazz = runtime.getClass("Class");
        RubyClass hash = runtime.getClass("Hash");
        RubyClass array = runtime.getClass("Array");
        RubyClass struct = runtime.getClass("Struct");
        RubyClass exception = runtime.getClass("Exception");
        RubyClass string = runtime.getClass("String");
        RubyClass symbol = runtime.getClass("Symbol");
        RubyClass range = runtime.getClass("Range");
        RubyClass regexp = runtime.getClass("Regexp");
        RubyClass time = runtime.getClass("Time");
        RubyClass date = runtime.getClass("Date"); 
        RubyClass fixnum = runtime.getClass("Fixnum"); 
        RubyClass bignum = runtime.getClass("Bignum"); 
        RubyClass flt = runtime.getClass("Float"); 
        RubyClass trueClass = runtime.getClass("TrueClass"); 
        RubyClass falseClass = runtime.getClass("FalseClass"); 
        RubyClass nilClass = runtime.getClass("NilClass"); 

        clazz.defineFastMethod("to_yaml",callbackFactory.getFastOptSingletonMethod("class_to_yaml"));
        
        obj.defineFastMethod("to_yaml_properties",callbackFactory.getFastSingletonMethod("obj_to_yaml_properties"));
        obj.defineFastMethod("to_yaml_style",callbackFactory.getFastSingletonMethod("obj_to_yaml_style"));
        obj.defineFastMethod("to_yaml_node",callbackFactory.getFastSingletonMethod("obj_to_yaml_node",IRubyObject.class));
        obj.defineFastMethod("to_yaml",callbackFactory.getFastOptSingletonMethod("obj_to_yaml"));
        obj.defineFastMethod("taguri",callbackFactory.getFastSingletonMethod("obj_taguri"));
        
        hash.defineFastMethod("to_yaml_node",callbackFactory.getFastSingletonMethod("hash_to_yaml_node",IRubyObject.class));
        hash.defineFastMethod("taguri",callbackFactory.getFastSingletonMethod("hash_taguri"));

        array.defineFastMethod("to_yaml_node",callbackFactory.getFastSingletonMethod("array_to_yaml_node",IRubyObject.class));
        array.defineFastMethod("taguri",callbackFactory.getFastSingletonMethod("array_taguri"));

        struct.defineFastMethod("to_yaml_node",callbackFactory.getFastSingletonMethod("struct_to_yaml_node",IRubyObject.class));
        struct.defineFastMethod("taguri",callbackFactory.getFastSingletonMethod("struct_taguri"));

        exception.defineFastMethod("to_yaml_node",callbackFactory.getFastSingletonMethod("exception_to_yaml_node",IRubyObject.class));
        exception.defineFastMethod("taguri",callbackFactory.getFastSingletonMethod("exception_taguri"));

        string.defineFastMethod("is_complex_yaml?",callbackFactory.getFastSingletonMethod("string_is_complex"));
        string.defineFastMethod("is_binary_data?",callbackFactory.getFastSingletonMethod("string_is_binary"));
        string.defineFastMethod("to_yaml_node",callbackFactory.getFastSingletonMethod("string_to_yaml_node",IRubyObject.class));
        string.defineFastMethod("taguri",callbackFactory.getFastSingletonMethod("string_taguri"));

        symbol.defineFastMethod("to_yaml_node",callbackFactory.getFastSingletonMethod("symbol_to_yaml_node",IRubyObject.class));
        symbol.defineFastMethod("taguri",callbackFactory.getFastSingletonMethod("string_taguri"));

        range.defineFastMethod("to_yaml_node",callbackFactory.getFastSingletonMethod("range_to_yaml_node",IRubyObject.class));
        range.defineFastMethod("taguri",callbackFactory.getFastSingletonMethod("range_taguri"));

        regexp.defineFastMethod("to_yaml_node",callbackFactory.getFastSingletonMethod("regexp_to_yaml_node",IRubyObject.class));
        regexp.defineFastMethod("taguri",callbackFactory.getFastSingletonMethod("regexp_taguri"));

        time.defineFastMethod("to_yaml_node",callbackFactory.getFastSingletonMethod("time_to_yaml_node",IRubyObject.class));
        time.defineFastMethod("taguri",callbackFactory.getFastSingletonMethod("time_taguri"));

        date.defineFastMethod("to_yaml_node",callbackFactory.getFastSingletonMethod("date_to_yaml_node",IRubyObject.class));
        date.defineFastMethod("taguri",callbackFactory.getFastSingletonMethod("date_taguri"));

        bignum.defineFastMethod("to_yaml_node",callbackFactory.getFastSingletonMethod("numeric_to_yaml_node",IRubyObject.class));
        bignum.defineFastMethod("taguri",callbackFactory.getFastSingletonMethod("fixnum_taguri"));

        fixnum.defineFastMethod("to_yaml_node",callbackFactory.getFastSingletonMethod("numeric_to_yaml_node",IRubyObject.class));
        fixnum.defineFastMethod("taguri",callbackFactory.getFastSingletonMethod("fixnum_taguri"));

        flt.defineFastMethod("to_yaml_node",callbackFactory.getFastSingletonMethod("numeric_to_yaml_node",IRubyObject.class));
        flt.defineFastMethod("taguri",callbackFactory.getFastSingletonMethod("float_taguri"));

        trueClass.defineFastMethod("to_yaml_node",callbackFactory.getFastSingletonMethod("true_to_yaml_node",IRubyObject.class));
        trueClass.defineFastMethod("taguri",callbackFactory.getFastSingletonMethod("true_taguri"));

        falseClass.defineFastMethod("to_yaml_node",callbackFactory.getFastSingletonMethod("false_to_yaml_node",IRubyObject.class));
        falseClass.defineFastMethod("taguri",callbackFactory.getFastSingletonMethod("false_taguri"));

        nilClass.defineFastMethod("to_yaml_node",callbackFactory.getFastSingletonMethod("nil_to_yaml_node",IRubyObject.class));
        nilClass.defineFastMethod("taguri",callbackFactory.getFastSingletonMethod("nil_taguri"));

        return result;
    }

    public static IRubyObject dump(IRubyObject self, IRubyObject[] args) {
        IRubyObject obj = args[0];
        IRubyObject val = self.getRuntime().newArray(obj);
        if(args.length>1) {
            return self.callMethod(self.getRuntime().getCurrentContext(),"dump_all", new IRubyObject[]{val,args[1]});
        } else {
            return self.callMethod(self.getRuntime().getCurrentContext(),"dump_all", val);
        }
    }

    public static IRubyObject dump_all(IRubyObject self, IRubyObject[] args) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        RubyArray objs = (RubyArray)args[0];
        IRubyObject io = null;
        IRubyObject io2 = null;
        if(args.length == 2 && args[1] != null && !args[1].isNil()) {
            io = args[1];
        }
        YAMLConfig cfg = YAML.config().version("1.0");
        IOOutputStream iox = null;
        if(null == io) {
            self.getRuntime().getModule("Kernel").callMethod(context,"require", self.getRuntime().newString("stringio"));
            io2 = self.getRuntime().getClass("StringIO").callMethod(context, "new");
            iox = new IOOutputStream(io2);
        } else {
            iox = new IOOutputStream(io);
        }
        Serializer ser = new JRubySerializer(new EmitterImpl(iox,cfg),new ResolverImpl(),cfg);
        try {
            ser.open();
            Representer r = new JRubyRepresenter(ser, cfg);
            for(Iterator iter = objs.getList().iterator();iter.hasNext();) {
                r.represent(iter.next());
            }
            ser.close();
        } catch(IOException e) {
            throw self.getRuntime().newIOErrorFromException(e);
        }
        if(null == io) {
            io2.callMethod(context, "rewind");
            return io2.callMethod(context, "read");
        } else {
            return io;
        }
    }

    public static IRubyObject load(IRubyObject self, IRubyObject arg) {
        IRubyObject io = arg;
        Scanner scn = null;
        if(io instanceof RubyString) {
            scn = new ScannerImpl(((RubyString)io).getByteList());
        } else {
            scn = new ScannerImpl(new IOInputStream(io));
        }
        Constructor ctor = new JRubyConstructor(self,new ComposerImpl(new ParserImpl(scn,YAML.config().version("1.0")),new ResolverImpl()));
        if(ctor.checkData()) {
            return JavaEmbedUtils.javaToRuby(self.getRuntime(),ctor.getData());
        }
        return self.getRuntime().getNil();
    }

    public static IRubyObject load_file(IRubyObject self, IRubyObject arg) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        IRubyObject io = self.getRuntime().getClass("File").callMethod(context,"open", new IRubyObject[]{arg,self.getRuntime().newString("r")});
        IRubyObject val = self.callMethod(context,"load", io);
        io.callMethod(context, "close");
        return val;
    }

    public static IRubyObject each_document(IRubyObject self, IRubyObject arg, Block block) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        IRubyObject io = arg;
        Scanner scn = null;
        if(io instanceof RubyString) {
            scn = new ScannerImpl(((RubyString)io).getByteList());
        } else {
            scn = new ScannerImpl(new IOInputStream(io));
        }
        Constructor ctor = new JRubyConstructor(self,new ComposerImpl(new ParserImpl(scn,YAML.config().version("1.0")),new ResolverImpl()));
        while(ctor.checkData()) {
            block.yield(context, JavaEmbedUtils.javaToRuby(self.getRuntime(),ctor.getData()));
        }
        return self.getRuntime().getNil();
    }

    public static IRubyObject load_documents(IRubyObject self, IRubyObject arg, Block block) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        IRubyObject io = arg;
        Scanner scn = null;
        if(io instanceof RubyString) {
            scn = new ScannerImpl(((RubyString)io).getByteList());
        } else {
            scn = new ScannerImpl(new IOInputStream(io));
        }
        Constructor ctor = new JRubyConstructor(self,new ComposerImpl(new ParserImpl(scn,YAML.config().version("1.0")),new ResolverImpl()));
        while(ctor.checkData()) {
            block.yield(context, JavaEmbedUtils.javaToRuby(self.getRuntime(),ctor.getData()));
        }
        return self.getRuntime().getNil();
    }

    public static IRubyObject load_stream(IRubyObject self, IRubyObject arg) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        IRubyObject d = self.getRuntime().getNil();
        IRubyObject io = arg;
        Scanner scn = null;
        if(io instanceof RubyString) {
            scn = new ScannerImpl(((RubyString)io).getByteList());
        } else {
            scn = new ScannerImpl(new IOInputStream(io));
        }
        Constructor ctor = new JRubyConstructor(self,new ComposerImpl(new ParserImpl(scn,YAML.config().version("1.0")),new ResolverImpl()));
        while(ctor.checkData()) {
            if(d.isNil()) {
                d = self.getRuntime().getModule("YAML").getClass("Stream").callMethod(context,"new", d);
            }
            d.callMethod(context,"add", JavaEmbedUtils.javaToRuby(self.getRuntime(),ctor.getData()));
        }
        return d;
    }

    public static IRubyObject dump_stream(IRubyObject self, IRubyObject[] args) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        IRubyObject stream = self.getRuntime().getModule("YAML").getClass("Stream").callMethod(context, "new");
        for(int i=0,j=args.length;i<j;i++) {
            stream.callMethod(context,"add", args[i]);
        }
        return stream.callMethod(context, "emit");
    }

    public static IRubyObject quick_emit_node(IRubyObject self, IRubyObject[] args, Block block) {
        return block.yield(self.getRuntime().getCurrentContext(), args[0]);
    }

    public static IRubyObject quick_emit(IRubyObject self, IRubyObject[] args) {
        return self.getRuntime().getNil();
    }

    public static IRubyObject hash_to_yaml_node(IRubyObject self, IRubyObject arg) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        return arg.callMethod(context,"map", new IRubyObject[]{self.callMethod(context, "taguri"),self,self.callMethod(context, "to_yaml_style")});
    }
    public static IRubyObject hash_taguri(IRubyObject self) {
        String className = self.getType().getName();
        if("Hash".equals(className)) {
            return self.getRuntime().newString("tag:yaml.org,2002:map");
        } else {
            return self.getRuntime().newString("tag:yaml.org,2002:map:" + className);
        }
    }
    public static IRubyObject obj_to_yaml_properties(IRubyObject self) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        return self.callMethod(context, "instance_variables").callMethod(context, "sort");
    }
    public static IRubyObject obj_to_yaml_style(IRubyObject self) {
        return self.getRuntime().getNil();
    }
    public static IRubyObject obj_to_yaml_node(IRubyObject self, IRubyObject arg) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        Map mep = (Map)(new RubyHash(self.getRuntime()));
        RubyArray props = (RubyArray)self.callMethod(context, "to_yaml_properties");
        for(Iterator iter = props.getList().iterator(); iter.hasNext();) {
            String m = iter.next().toString();
            mep.put(self.getRuntime().newString(m.substring(1)), self.callMethod(context,"instance_variable_get", self.getRuntime().newString(m)));
        }
        return arg.callMethod(context,"map", new IRubyObject[]{self.callMethod(context, "taguri"),(IRubyObject)mep,self.callMethod(context, "to_yaml_style")});
    }
    public static IRubyObject obj_taguri(IRubyObject self) {
        return self.getRuntime().newString("!ruby/object:" + self.getType().getName());
    }
    public static IRubyObject class_to_yaml(IRubyObject self, IRubyObject[] args) {
        throw self.getRuntime().newTypeError("can't dump anonymous class " + self.getType().getName());
    }
    public static IRubyObject obj_to_yaml(IRubyObject self, IRubyObject[] args) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        return self.getRuntime().getModule("YAML").callMethod(context,"dump", self);
    }
    public static IRubyObject array_to_yaml_node(IRubyObject self, IRubyObject arg) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        return arg.callMethod(context,"seq", new IRubyObject[]{self.callMethod(context, "taguri"),self,self.callMethod(context, "to_yaml_style")});
    }
    public static IRubyObject array_taguri(IRubyObject self) {
        String className = self.getType().getName();
        if("Array".equals(className)) {
            return self.getRuntime().newString("tag:yaml.org,2002:seq");
        } else {
            return self.getRuntime().newString("tag:yaml.org,2002:seq:" + className);
        }
    }

    public static IRubyObject struct_to_yaml_node(IRubyObject self, IRubyObject arg) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        Map mep = (Map)(new RubyHash(self.getRuntime()));
        for(Iterator iter = ((RubyArray)self.callMethod(context, "members")).getList().iterator();iter.hasNext();) {
            IRubyObject key = self.getRuntime().newString(iter.next().toString());
            mep.put(key,self.callMethod(context,MethodIndex.AREF, "[]", key));
        }            
        for(Iterator iter = ((RubyArray)self.callMethod(context, "to_yaml_properties")).getList().iterator(); iter.hasNext();) {
            String m = iter.next().toString();
            mep.put(self.getRuntime().newString(m.substring(1)), self.callMethod(context,"instance_variable_get", self.getRuntime().newString(m)));
        }
        return arg.callMethod(context,"map", new IRubyObject[]{self.callMethod(context, "taguri"),(IRubyObject)mep,self.callMethod(context, "to_yaml_style")});
    }
    public static IRubyObject struct_taguri(IRubyObject self) {
        return self.getRuntime().newString("!ruby/struct:" + self.getType().getName());
    }
    public static IRubyObject exception_to_yaml_node(IRubyObject self, IRubyObject arg) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        Map mep = (Map)(new RubyHash(self.getRuntime()));
        mep.put(self.getRuntime().newString("message"),self.callMethod(context, "message"));
        for(Iterator iter = ((RubyArray)self.callMethod(context, "to_yaml_properties")).getList().iterator(); iter.hasNext();) {
            String m = iter.next().toString();
            mep.put(self.getRuntime().newString(m.substring(1)), self.callMethod(context,"instance_variable_get", self.getRuntime().newString(m)));
        }
        return arg.callMethod(context,"map", new IRubyObject[]{self.callMethod(context, "taguri"),(IRubyObject)mep,self.callMethod(context, "to_yaml_style")});
    }
    public static IRubyObject exception_taguri(IRubyObject self) {
        return self.getRuntime().newString("!ruby/exception:" + self.getType().getName());
    }
    private static final Pattern AFTER_NEWLINE = Pattern.compile("\n.+", Pattern.DOTALL);
    public static IRubyObject string_is_complex(IRubyObject self) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        return (self.callMethod(context, "to_yaml_style").isTrue() ||
                ((List)self.callMethod(context, "to_yaml_properties")).isEmpty() ||
                AFTER_NEWLINE.matcher(self.toString()).find()) ? self.getRuntime().getTrue() : self.getRuntime().getFalse();
    }
    public static IRubyObject string_is_binary(IRubyObject self) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        if(self.callMethod(context, MethodIndex.EMPTY_P, "empty?").isTrue()) {
            return self.getRuntime().getNil();
        }
        return self.toString().indexOf('\0') != -1 ? self.getRuntime().getTrue() : self.getRuntime().getFalse();
    }
    private static org.jruby.yaml.JRubyRepresenter into(IRubyObject arg) {
        IRubyObject jobj = arg.getInstanceVariable("@java_object");
        if(jobj != null) {
            return (org.jruby.yaml.JRubyRepresenter)(((org.jruby.javasupport.JavaObject)jobj).getValue());
        }
        return null;
    }
    public static IRubyObject string_to_yaml_node(IRubyObject self, IRubyObject arg) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        Ruby rt = self.getRuntime();
        if(self.callMethod(context, "is_binary_data?").isTrue()) {
            return arg.callMethod(context,"scalar", new IRubyObject[]{rt.newString("tag:yaml.org,2002:binary"),rt.newArray(self).callMethod(context,"pack", rt.newString("m")),rt.newString("|")});
        }
        if(((List)self.callMethod(context, "to_yaml_properties")).isEmpty()) {
            org.jruby.yaml.JRubyRepresenter rep = into(arg);
            if(rep != null) {
                try {
                    return org.jruby.javasupport.JavaUtil.convertJavaToRuby(rt,rep.scalar(self.callMethod(context, "taguri").toString(),self.convertToString().getByteList(),self.toString().startsWith(":") ? "\"" : self.callMethod(context, "to_yaml_style").toString()));
                } catch(IOException e) {
                    throw rt.newIOErrorFromException(e);
                }
            } else {
                return arg.callMethod(context,"scalar", new IRubyObject[]{self.callMethod(context, "taguri"),self,self.toString().startsWith(":") ? rt.newString("\"") : self.callMethod(context, "to_yaml_style")});
            }
        }
            
        Map mep = (Map)(new RubyHash(self.getRuntime()));
        mep.put(self.getRuntime().newString("str"),rt.newString(self.toString()));
        for(Iterator iter = ((RubyArray)self.callMethod(context, "to_yaml_properties")).getList().iterator(); iter.hasNext();) {
            String m = iter.next().toString();
            mep.put(self.getRuntime().newString(m.substring(1)), self.callMethod(context,"instance_variable_get", self.getRuntime().newString(m)));
        }
        return arg.callMethod(context,"map", new IRubyObject[]{self.callMethod(context, "taguri"),(IRubyObject)mep,self.callMethod(context, "to_yaml_style")});
    }
    public static IRubyObject string_taguri(IRubyObject self) {
        return self.getRuntime().newString("tag:yaml.org,2002:str");
    }
    public static IRubyObject symbol_to_yaml_node(IRubyObject self, IRubyObject arg) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        return arg.callMethod(context,"scalar", new IRubyObject[]{self.callMethod(context, "taguri"),self.callMethod(context, "inspect"),self.callMethod(context, "to_yaml_style")});
    }
    public static IRubyObject numeric_to_yaml_node(IRubyObject self, IRubyObject arg) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        String val = self.toString();
        if("Infinity".equals(val)) {
            val = ".Inf";
        } else if("-Infinity".equals(val)) {
            val = "-.Inf";
        } else if("NaN".equals(val)) {
            val = ".NaN";
        }
        return arg.callMethod(context,"scalar", new IRubyObject[]{self.callMethod(context, "taguri"),self.getRuntime().newString(val),self.callMethod(context, "to_yaml_style")});
    }

    public static IRubyObject range_to_yaml_node(IRubyObject self, IRubyObject arg) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        Map mep = (Map)(new RubyHash(self.getRuntime()));
        mep.put(self.getRuntime().newString("begin"),self.callMethod(context, "begin"));
        mep.put(self.getRuntime().newString("end"),self.callMethod(context, "end"));
        mep.put(self.getRuntime().newString("excl"),self.callMethod(context, "exclude_end?"));
        for(Iterator iter = ((RubyArray)self.callMethod(context, "to_yaml_properties")).getList().iterator(); iter.hasNext();) {
            String m = iter.next().toString();
            mep.put(self.getRuntime().newString(m.substring(1)), self.callMethod(context,"instance_variable_get", self.getRuntime().newString(m)));
        }
        return arg.callMethod(context,"map", new IRubyObject[]{self.callMethod(context, "taguri"),(IRubyObject)mep,self.callMethod(context, "to_yaml_style")});
    }
    public static IRubyObject range_taguri(IRubyObject self) {
        return self.getRuntime().newString("!ruby/range");
    }
    public static IRubyObject regexp_to_yaml_node(IRubyObject self, IRubyObject arg) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        return arg.callMethod(context,"scalar", new IRubyObject[]{self.callMethod(context, "taguri"),self.callMethod(context, "inspect"),self.callMethod(context, "to_yaml_style")});
    }
    public static IRubyObject regexp_taguri(IRubyObject self) {
        return self.getRuntime().newString("!ruby/regexp");
    }
    public static IRubyObject time_to_yaml_node(IRubyObject self, IRubyObject arg) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        IRubyObject tz = self.getRuntime().newString("Z");
        IRubyObject difference_sign = self.getRuntime().newString("-");
        self = self.dup();
        if(!self.callMethod(context, "utc?").isTrue()) {
            IRubyObject utc_same_instant = self.callMethod(context, "utc");
            IRubyObject utc_same_writing = self.getRuntime().getClass("Time").callMethod(context,"utc", new IRubyObject[]{
                    self.callMethod(context, "year"),self.callMethod(context, "month"),self.callMethod(context, "day"),self.callMethod(context, "hour"),
                    self.callMethod(context, "min"),self.callMethod(context, "sec"),self.callMethod(context, "usec")});
            IRubyObject difference_to_utc = utc_same_writing.callMethod(context,MethodIndex.OP_MINUS, "-", utc_same_instant);
            IRubyObject absolute_difference;
            if(difference_to_utc.callMethod(context,MethodIndex.OP_LT, "<", RubyFixnum.zero(self.getRuntime())).isTrue()) {
                difference_sign = self.getRuntime().newString("-");
                absolute_difference = RubyFixnum.zero(self.getRuntime()).callMethod(context,MethodIndex.OP_MINUS, "-", difference_to_utc);
            } else {
                difference_sign = self.getRuntime().newString("+");
                absolute_difference = difference_to_utc;
            }
            IRubyObject difference_minutes = absolute_difference.callMethod(context,"/", self.getRuntime().newFixnum(60)).callMethod(context, "round");
            tz = self.getRuntime().newString("%s%02d:%02d").callMethod(context,"%", self.getRuntime().newArrayNoCopy(new IRubyObject[]{difference_sign,difference_minutes.callMethod(context,"/", self.getRuntime().newFixnum(60)),difference_minutes.callMethod(context,"%", self.getRuntime().newFixnum(60))}));
        }
        IRubyObject standard = self.callMethod(context,"strftime", self.getRuntime().newString("%Y-%m-%d %H:%M:%S"));
        if(self.callMethod(context, "usec").callMethod(context, "nonzero?").isTrue()) {
            standard = standard.callMethod(context, MethodIndex.OP_PLUS, "+", self.getRuntime().newString(".%06d").callMethod(context,"%", self.getRuntime().newArray(self.callMethod(context, "usec"))));
        }
        standard = standard.callMethod(context,MethodIndex.OP_PLUS, "+", self.getRuntime().newString(" %s").callMethod(context,"%", self.getRuntime().newArray(tz)));
        return arg.callMethod(context,"scalar", new IRubyObject[]{self.callMethod(context, "taguri"),standard,self.callMethod(context, "to_yaml_style")});
    }
    public static IRubyObject time_taguri(IRubyObject self) {
        return self.getRuntime().newString("tag:yaml.org,2002:timestamp");
    }
    public static IRubyObject date_to_yaml_node(IRubyObject self, IRubyObject arg) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        return arg.callMethod(context,"scalar", new IRubyObject[]{self.callMethod(context, "taguri"),self.callMethod(context, MethodIndex.TO_S, "to_s"),self.callMethod(context, "to_yaml_style")});
    }
    public static IRubyObject date_taguri(IRubyObject self) {
        return self.getRuntime().newString("tag:yaml.org,2002:timestamp#ymd");
    }
    public static IRubyObject fixnum_taguri(IRubyObject self) {
        return self.getRuntime().newString("tag:yaml.org,2002:int");
    }
    public static IRubyObject float_taguri(IRubyObject self) {
        return self.getRuntime().newString("tag:yaml.org,2002:float");
    }

    public static IRubyObject true_to_yaml_node(IRubyObject self, IRubyObject arg) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        return arg.callMethod(context,"scalar", new IRubyObject[]{self.callMethod(context, "taguri"),self.callMethod(context, MethodIndex.TO_S, "to_s"),self.callMethod(context, "to_yaml_style")});
    }
    public static IRubyObject true_taguri(IRubyObject self) {
        return self.getRuntime().newString("tag:yaml.org,2002:bool");
    }
    public static IRubyObject false_to_yaml_node(IRubyObject self, IRubyObject arg) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        return arg.callMethod(context,"scalar", new IRubyObject[]{self.callMethod(context, "taguri"),self.callMethod(context, MethodIndex.TO_S, "to_s"),self.callMethod(context, "to_yaml_style")});
    }
    public static IRubyObject false_taguri(IRubyObject self) {
        return self.getRuntime().newString("tag:yaml.org,2002:bool");
    }
    public static IRubyObject nil_to_yaml_node(IRubyObject self, IRubyObject arg) {
        ThreadContext context = self.getRuntime().getCurrentContext();
        return arg.callMethod(context,"scalar", new IRubyObject[]{self.callMethod(context, "taguri"),self.getRuntime().newString(""),self.callMethod(context, "to_yaml_style")});
    }
    public static IRubyObject nil_taguri(IRubyObject self) {
        return self.getRuntime().newString("tag:yaml.org,2002:null");
    }
}// RubyYAML
