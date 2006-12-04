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
package org.jruby;

import java.io.IOException;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.regex.Pattern;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.javasupport.JavaEmbedUtils;

import org.jruby.internal.runtime.methods.MultiStub;
import org.jruby.internal.runtime.methods.MultiStubMethod;

import org.jruby.yaml.JRubyRepresenter;
import org.jruby.yaml.JRubyConstructor;
import org.jruby.yaml.JRubySerializer;
import org.jruby.util.IOReader;
import org.jruby.util.IOWriter;

import org.jvyaml.Representer;
import org.jvyaml.Constructor;
import org.jvyaml.ParserImpl;
import org.jvyaml.Scanner;
import org.jvyaml.ScannerImpl;
import org.jvyaml.ComposerImpl;
import org.jvyaml.Serializer;
import org.jvyaml.ResolverImpl;
import org.jvyaml.EmitterImpl;
import org.jvyaml.YAMLConfig;
import org.jvyaml.YAML;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 * @version $Revision: $
 */
public class RubyYAML {
    public static RubyModule createYAMLModule(IRuby runtime) {
        runtime.getModule("Kernel").callMethod(runtime.getCurrentContext(),"require", runtime.newString("date"));
        RubyModule result = runtime.defineModule("YAML");

        YAMLStub0 ystub = new YAMLStub0();
        ystub.yaml_dump = new MultiStubMethod(ystub,0,result,Arity.optional(),Visibility.PUBLIC);
        ystub.yaml_dump_all = new MultiStubMethod(ystub,3,result,Arity.optional(),Visibility.PUBLIC);
        ystub.yaml_load = new MultiStubMethod(ystub,1,result,Arity.singleArgument(),Visibility.PUBLIC);
        ystub.yaml_load_file = new MultiStubMethod(ystub,2,result,Arity.singleArgument(),Visibility.PUBLIC);
        ystub.yaml_each_document = new MultiStubMethod(ystub,4,result,Arity.singleArgument(),Visibility.PUBLIC);
        ystub.yaml_load_documents = new MultiStubMethod(ystub,5,result,Arity.singleArgument(),Visibility.PUBLIC);
        ystub.yaml_load_stream = new MultiStubMethod(ystub,6,result,Arity.singleArgument(),Visibility.PUBLIC);
        ystub.yaml_dump_stream = new MultiStubMethod(ystub,7,result,Arity.optional(),Visibility.PUBLIC);
        ystub.yaml_quick_emit_node = new MultiStubMethod(ystub,8,result,Arity.optional(),Visibility.PUBLIC);
        ystub.yaml_quick_emit = new MultiStubMethod(ystub,9,result,Arity.optional(),Visibility.PUBLIC);
        result.addModuleFunction("dump",ystub.yaml_dump);
        result.addModuleFunction("dump_all",ystub.yaml_dump_all);
        result.addModuleFunction("load",ystub.yaml_load);
        result.addModuleFunction("load_file",ystub.yaml_load_file);
        result.addModuleFunction("each_document",ystub.yaml_each_document);
        result.addModuleFunction("load_documents",ystub.yaml_load_documents);
        result.addModuleFunction("load_stream",ystub.yaml_load_stream);
        result.addModuleFunction("dump_stream",ystub.yaml_dump_stream);
        result.addModuleFunction("quick_emit_node",ystub.yaml_quick_emit_node);
        result.addModuleFunction("quick_emit",ystub.yaml_quick_emit);

        ToYAMLNodeStub0 stub0 = new ToYAMLNodeStub0();
        ToYAMLNodeStub1 stub1 = new ToYAMLNodeStub1();
        ToYAMLNodeStub2 stub2 = new ToYAMLNodeStub2();
        ToYAMLNodeStub3 stub3 = new ToYAMLNodeStub3();

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
        RubyClass numeric = runtime.getClass("Numeric"); 
        RubyClass fixnum = runtime.getClass("Fixnum"); 
        RubyClass flt = runtime.getClass("Float"); 
        RubyClass trueClass = runtime.getClass("TrueClass"); 
        RubyClass falseClass = runtime.getClass("FalseClass"); 
        RubyClass nilClass = runtime.getClass("NilClass"); 
       
        stub0.obj_to_yaml_properties = new MultiStubMethod(stub0,2,obj,Arity.noArguments(),Visibility.PUBLIC);
        stub0.obj_to_yaml_style = new MultiStubMethod(stub0,3,obj,Arity.noArguments(),Visibility.PUBLIC);
        stub0.obj_to_yaml_node = new MultiStubMethod(stub0,4,obj,Arity.optional(),Visibility.PUBLIC);
        stub0.obj_to_yaml = new MultiStubMethod(stub0,7,obj,Arity.optional(),Visibility.PUBLIC);
        stub0.obj_taguri = new MultiStubMethod(stub0,5,obj,Arity.noArguments(),Visibility.PUBLIC);
        stub0.class_to_yaml = new MultiStubMethod(stub0,6,clazz,Arity.optional(),Visibility.PUBLIC);
        stub0.hash_to_yaml_node = new MultiStubMethod(stub0,0,hash,Arity.singleArgument(),Visibility.PUBLIC);
        stub0.hash_taguri = new MultiStubMethod(stub0,1,hash,Arity.noArguments(),Visibility.PUBLIC);
        stub0.array_to_yaml_node = new MultiStubMethod(stub0,8,hash,Arity.singleArgument(),Visibility.PUBLIC);
        stub0.array_taguri = new MultiStubMethod(stub0,9,hash,Arity.noArguments(),Visibility.PUBLIC);
        stub1.struct_to_yaml_node = new MultiStubMethod(stub1,0,struct,Arity.singleArgument(),Visibility.PUBLIC);
        stub1.struct_taguri = new MultiStubMethod(stub1,1,struct,Arity.noArguments(),Visibility.PUBLIC);
        stub1.exception_to_yaml_node = new MultiStubMethod(stub1,2,exception,Arity.singleArgument(),Visibility.PUBLIC);
        stub1.exception_taguri = new MultiStubMethod(stub1,3,exception,Arity.noArguments(),Visibility.PUBLIC);
        stub1.string_is_complex = new MultiStubMethod(stub1,4,string,Arity.noArguments(),Visibility.PUBLIC);
        stub1.string_is_binary = new MultiStubMethod(stub1,5,string,Arity.noArguments(),Visibility.PUBLIC);
        stub1.string_to_yaml_node = new MultiStubMethod(stub1,6,string,Arity.singleArgument(),Visibility.PUBLIC);
        stub1.string_taguri = new MultiStubMethod(stub1,7,string,Arity.noArguments(),Visibility.PUBLIC);
        stub1.symbol_to_yaml_node = new MultiStubMethod(stub1,8,symbol,Arity.singleArgument(),Visibility.PUBLIC);
        stub2.range_to_yaml_node = new MultiStubMethod(stub2,0,range,Arity.singleArgument(),Visibility.PUBLIC);
        stub2.range_taguri = new MultiStubMethod(stub2,1,range,Arity.noArguments(),Visibility.PUBLIC);
        stub2.regexp_to_yaml_node = new MultiStubMethod(stub2,2,regexp,Arity.singleArgument(),Visibility.PUBLIC);
        stub2.regexp_taguri = new MultiStubMethod(stub2,3,regexp,Arity.noArguments(),Visibility.PUBLIC);
        stub2.time_to_yaml_node = new MultiStubMethod(stub2,4,time,Arity.singleArgument(),Visibility.PUBLIC);
        stub2.time_taguri = new MultiStubMethod(stub2,5,time,Arity.noArguments(),Visibility.PUBLIC);
        stub2.date_to_yaml_node = new MultiStubMethod(stub2,6,date,Arity.singleArgument(),Visibility.PUBLIC);
        stub2.date_taguri = new MultiStubMethod(stub2,7,date,Arity.noArguments(),Visibility.PUBLIC);
        stub1.numeric_to_yaml_node = new MultiStubMethod(stub1,9,numeric,Arity.singleArgument(),Visibility.PUBLIC);
        stub2.fixnum_taguri = new MultiStubMethod(stub2,8,fixnum,Arity.noArguments(),Visibility.PUBLIC);
        stub2.float_taguri = new MultiStubMethod(stub2,9,flt,Arity.noArguments(),Visibility.PUBLIC);
        stub3.true_to_yaml_node = new MultiStubMethod(stub3,0,trueClass,Arity.singleArgument(),Visibility.PUBLIC);
        stub3.true_taguri = new MultiStubMethod(stub3,1,trueClass,Arity.noArguments(),Visibility.PUBLIC);
        stub3.false_to_yaml_node = new MultiStubMethod(stub3,2,falseClass,Arity.singleArgument(),Visibility.PUBLIC);
        stub3.false_taguri = new MultiStubMethod(stub3,3,falseClass,Arity.noArguments(),Visibility.PUBLIC);
        stub3.nil_to_yaml_node = new MultiStubMethod(stub3,4,nilClass,Arity.singleArgument(),Visibility.PUBLIC);
        stub3.nil_taguri = new MultiStubMethod(stub3,5,nilClass,Arity.noArguments(),Visibility.PUBLIC);

        clazz.addMethod("to_yaml",stub0.class_to_yaml);
        
        obj.addMethod("to_yaml_properties",stub0.obj_to_yaml_properties);
        obj.addMethod("to_yaml_style",stub0.obj_to_yaml_style);
        obj.addMethod("to_yaml_node",stub0.obj_to_yaml_node);
        obj.addMethod("to_yaml",stub0.obj_to_yaml);
        obj.addMethod("taguri",stub0.obj_taguri);
        
        hash.addMethod("to_yaml_node",stub0.hash_to_yaml_node);
        hash.addMethod("taguri",stub0.hash_taguri);

        array.addMethod("to_yaml_node",stub0.array_to_yaml_node);
        array.addMethod("taguri",stub0.array_taguri);

        struct.addMethod("to_yaml_node",stub1.struct_to_yaml_node);
        struct.addMethod("taguri",stub1.struct_taguri);

        exception.addMethod("to_yaml_node",stub1.exception_to_yaml_node);
        exception.addMethod("taguri",stub1.exception_taguri);

        string.addMethod("is_complex_yaml?",stub1.string_is_complex);
        string.addMethod("is_binary_data?",stub1.string_is_binary);
        string.addMethod("to_yaml_node",stub1.string_to_yaml_node);
        string.addMethod("taguri",stub1.string_taguri);

        symbol.addMethod("to_yaml_node",stub1.symbol_to_yaml_node);
        symbol.addMethod("taguri",stub1.string_taguri);

        range.addMethod("to_yaml_node",stub2.range_to_yaml_node);
        range.addMethod("taguri",stub2.range_taguri);

        regexp.addMethod("to_yaml_node",stub2.regexp_to_yaml_node);
        regexp.addMethod("taguri",stub2.regexp_taguri);

        time.addMethod("to_yaml_node",stub2.time_to_yaml_node);
        time.addMethod("taguri",stub2.time_taguri);

        date.addMethod("to_yaml_node",stub2.date_to_yaml_node);
        date.addMethod("taguri",stub2.date_taguri);

        numeric.addMethod("to_yaml_node",stub1.numeric_to_yaml_node);

        fixnum.addMethod("taguri",stub2.fixnum_taguri);
        flt.addMethod("taguri",stub2.float_taguri);

        trueClass.addMethod("to_yaml_node",stub3.true_to_yaml_node);
        trueClass.addMethod("taguri",stub3.true_taguri);

        falseClass.addMethod("to_yaml_node",stub3.false_to_yaml_node);
        falseClass.addMethod("taguri",stub3.false_taguri);

        nilClass.addMethod("to_yaml_node",stub3.nil_to_yaml_node);
        nilClass.addMethod("taguri",stub3.nil_taguri);

        return result;
    }

    public static class YAMLStub0 implements MultiStub {
        public MultiStubMethod yaml_dump;
        public MultiStubMethod yaml_dump_all;
        public MultiStubMethod yaml_load;
        public MultiStubMethod yaml_load_file;
        public MultiStubMethod yaml_each_document;
        public MultiStubMethod yaml_load_documents;
        public MultiStubMethod yaml_load_stream;
        public MultiStubMethod yaml_dump_stream;
        public MultiStubMethod yaml_quick_emit_node;
        public MultiStubMethod yaml_quick_emit;

        public IRubyObject method0(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            //yaml_dump
            IRubyObject obj = args[0];
            IRubyObject val = self.getRuntime().newArray(obj);
            if(args.length>1) {
                return self.callMethod(context,"dump_all", new IRubyObject[]{val,args[1]});
            } else {
                return self.callMethod(context,"dump_all", val);
            }
        }
        public IRubyObject method1(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            //yaml_load
            IRubyObject io = args[0];
            Scanner scn = null;
            if(io instanceof RubyString) {
                scn = new ScannerImpl(io.toString());
            } else {
                scn = new ScannerImpl(new IOReader(io));
            }
            Constructor ctor = new JRubyConstructor(self,new ComposerImpl(new ParserImpl(scn,YAML.config().version("1.0")),new ResolverImpl()));
            if(ctor.checkData()) {
                return JavaEmbedUtils.javaToRuby(self.getRuntime(),ctor.getData());
            }
            return self.getRuntime().getNil();
        }
        public IRubyObject method2(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            //yaml_load_file
            IRubyObject io = self.getRuntime().getClass("File").callMethod(context,"open", new IRubyObject[]{args[0],self.getRuntime().newString("r")});
            IRubyObject val = self.callMethod(context,"load", io);
            io.callMethod(context, "close");
            return val;

        }
        public IRubyObject method3(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            //yaml_dump_all
            RubyArray objs = (RubyArray)args[0];
            IRubyObject io = null;
            IRubyObject io2 = null;
            if(args.length == 2 && args[1] != null && !args[1].isNil()) {
                io = args[1];
            }
            YAMLConfig cfg = YAML.config().version("1.0");
            IOWriter iox = null;
            if(null == io) {
                self.getRuntime().getModule("Kernel").callMethod(context,"require", self.getRuntime().newString("stringio"));
                io2 = self.getRuntime().getClass("StringIO").callMethod(context, "new");
                iox = new IOWriter(io2);
            } else {
                iox = new IOWriter(io);
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
        public IRubyObject method4(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            //yaml_each_document
            IRubyObject io = args[0];
            Scanner scn = null;
            if(io instanceof RubyString) {
                scn = new ScannerImpl(io.toString());
            } else {
                scn = new ScannerImpl(new IOReader(io));
            }
            Constructor ctor = new JRubyConstructor(self,new ComposerImpl(new ParserImpl(scn,YAML.config().version("1.0")),new ResolverImpl()));
            while(ctor.checkData()) {
                context.yield(JavaEmbedUtils.javaToRuby(self.getRuntime(),ctor.getData()));
            }
            return self.getRuntime().getNil();
        }
        public IRubyObject method5(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            //yaml_load_documents
            IRubyObject io = args[0];
            Scanner scn = null;
            if(io instanceof RubyString) {
                scn = new ScannerImpl(io.toString());
            } else {
                scn = new ScannerImpl(new IOReader(io));
            }
            Constructor ctor = new JRubyConstructor(self,new ComposerImpl(new ParserImpl(scn,YAML.config().version("1.0")),new ResolverImpl()));
            while(ctor.checkData()) {
                context.yield(JavaEmbedUtils.javaToRuby(self.getRuntime(),ctor.getData()));
            }
            return self.getRuntime().getNil();
        }
        public IRubyObject method6(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            //yaml_load_stream
            IRubyObject d = self.getRuntime().getNil();
            IRubyObject io = args[0];
            Scanner scn = null;
            if(io instanceof RubyString) {
                scn = new ScannerImpl(io.toString());
            } else {
                scn = new ScannerImpl(new IOReader(io));
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
        public IRubyObject method7(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            //yaml_dump_stream
            IRubyObject stream = self.getRuntime().getModule("YAML").getClass("Stream").callMethod(context, "new");
            for(int i=0,j=args.length;i<j;) {
                stream.callMethod(context,"add", args[i]);
            }
            return stream.callMethod(context, "emit");
        }
        public IRubyObject method8(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            //yaml_quick_emit_node
            return context.yield(args[0]);
        }
        public IRubyObject method9(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            //yaml_quick_emit
            return self.getRuntime().getNil();
        }
    }

    public static class ToYAMLNodeStub0 implements MultiStub {
        public MultiStubMethod obj_to_yaml_properties;
        public MultiStubMethod obj_to_yaml_style;
        public MultiStubMethod obj_to_yaml_node;
        public MultiStubMethod obj_to_yaml;
        public MultiStubMethod obj_taguri;
        public MultiStubMethod class_to_yaml;
        public MultiStubMethod hash_to_yaml_node;
        public MultiStubMethod hash_taguri;
        public MultiStubMethod array_to_yaml_node;
        public MultiStubMethod array_taguri;

        public IRubyObject method0(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return args[0].callMethod(context,"map", new IRubyObject[]{self.callMethod(context, "taguri"),self,self.callMethod(context, "to_yaml_style")});
        }
        public IRubyObject method1(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            String className = self.getType().getName();
            if("Hash".equals(className)) {
                return self.getRuntime().newString("tag:yaml.org,2002:map");
            } else {
                return self.getRuntime().newString("tag:yaml.org,2002:map:" + className);
            }
        }
        public IRubyObject method2(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return self.callMethod(context, "instance_variables").callMethod(context, "sort");
        }
        public IRubyObject method3(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return self.getRuntime().getNil();
        }
        public IRubyObject method4(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            Map mep = (Map)(new RubyHash(self.getRuntime()));
            RubyArray props = (RubyArray)self.callMethod(context, "to_yaml_properties");
            for(Iterator iter = props.getList().iterator(); iter.hasNext();) {
                String m = iter.next().toString();
                mep.put(self.getRuntime().newString(m.substring(1)), self.callMethod(context,"instance_variable_get", self.getRuntime().newString(m)));
            }
            return args[0].callMethod(context,"map", new IRubyObject[]{self.callMethod(context, "taguri"),(IRubyObject)mep,self.callMethod(context, "to_yaml_style")});
        }
        public IRubyObject method5(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return self.getRuntime().newString("!ruby/object:" + self.getType().getName());
        }
        public IRubyObject method6(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            throw self.getRuntime().newTypeError("can't dump anonymous class " + self.getType().getName());
        }
        public IRubyObject method7(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return self.getRuntime().getModule("YAML").callMethod(context,"dump", self);
        }
        public IRubyObject method8(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return args[0].callMethod(context,"seq", new IRubyObject[]{self.callMethod(context, "taguri"),self,self.callMethod(context, "to_yaml_style")});
        }
        public IRubyObject method9(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            String className = self.getType().getName();
            if("Array".equals(className)) {
                return self.getRuntime().newString("tag:yaml.org,2002:seq");
            } else {
                return self.getRuntime().newString("tag:yaml.org,2002:seq:" + className);
            }
        }
    }

    public static class ToYAMLNodeStub1 implements MultiStub {
        public MultiStubMethod struct_taguri;
        public MultiStubMethod struct_to_yaml_node;
        public MultiStubMethod exception_taguri;
        public MultiStubMethod exception_to_yaml_node;
        public MultiStubMethod string_is_complex;
        public MultiStubMethod string_is_binary;
        public MultiStubMethod string_to_yaml_node;
        public MultiStubMethod string_taguri;
        public MultiStubMethod symbol_to_yaml_node;

        public MultiStubMethod numeric_to_yaml_node;

        public IRubyObject method0(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            //struct_to_yaml_node
            Map mep = (Map)(new RubyHash(self.getRuntime()));
            for(Iterator iter = ((RubyArray)self.callMethod(context, "members")).getList().iterator();iter.hasNext();) {
                IRubyObject key = self.getRuntime().newString(iter.next().toString());
                mep.put(key,self.callMethod(context,"[]", key));
            }            
            for(Iterator iter = ((RubyArray)self.callMethod(context, "to_yaml_properties")).getList().iterator(); iter.hasNext();) {
                String m = iter.next().toString();
                mep.put(self.getRuntime().newString(m.substring(1)), self.callMethod(context,"instance_variable_get", self.getRuntime().newString(m)));
            }
            return args[0].callMethod(context,"map", new IRubyObject[]{self.callMethod(context, "taguri"),(IRubyObject)mep,self.callMethod(context, "to_yaml_style")});
        }
        public IRubyObject method1(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return self.getRuntime().newString("!ruby/struct:" + self.getType().getName());
        }
        public IRubyObject method2(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            //exception_to_yaml_node
            Map mep = (Map)(new RubyHash(self.getRuntime()));
            mep.put(self.getRuntime().newString("message"),self.callMethod(context, "message"));
            for(Iterator iter = ((RubyArray)self.callMethod(context, "to_yaml_properties")).getList().iterator(); iter.hasNext();) {
                String m = iter.next().toString();
                mep.put(self.getRuntime().newString(m.substring(1)), self.callMethod(context,"instance_variable_get", self.getRuntime().newString(m)));
            }
            return args[0].callMethod(context,"map", new IRubyObject[]{self.callMethod(context, "taguri"),(IRubyObject)mep,self.callMethod(context, "to_yaml_style")});
        }
        public IRubyObject method3(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return self.getRuntime().newString("!ruby/exception:" + self.getType().getName());
        }
        private static final Pattern AFTER_NEWLINE = Pattern.compile("\n.+", Pattern.DOTALL);
        public IRubyObject method4(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return (self.callMethod(context, "to_yaml_style").isTrue() ||
                    ((List)self.callMethod(context, "to_yaml_properties")).isEmpty() ||
                    AFTER_NEWLINE.matcher(self.toString()).find()) ? self.getRuntime().getTrue() : self.getRuntime().getFalse();
        }
        public IRubyObject method5(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            if(self.callMethod(context, "empty?").isTrue()) {
                return self.getRuntime().getNil();
            }
            return self.toString().indexOf('\0') != -1 ? self.getRuntime().getTrue() : self.getRuntime().getFalse();
        }
        public IRubyObject method6(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            IRuby rt = self.getRuntime();
            if(self.callMethod(context, "is_binary_data?").isTrue()) {
                return args[0].callMethod(context,"scalar", new IRubyObject[]{rt.newString("tag:yaml.org,2002:binary"),rt.newArray(self).callMethod(context,"pack", rt.newString("m")),rt.newString("|")});
            }
            if(((List)self.callMethod(context, "to_yaml_properties")).isEmpty()) {
                return args[0].callMethod(context,"scalar", new IRubyObject[]{self.callMethod(context, "taguri"),self,self.toString().startsWith(":") ? rt.newString("\"") : self.callMethod(context, "to_yaml_style")});
            }
            
            Map mep = (Map)(new RubyHash(self.getRuntime()));
            mep.put(self.getRuntime().newString("str"),rt.newString(self.toString()));
            for(Iterator iter = ((RubyArray)self.callMethod(context, "to_yaml_properties")).getList().iterator(); iter.hasNext();) {
                String m = iter.next().toString();
                mep.put(self.getRuntime().newString(m.substring(1)), self.callMethod(context,"instance_variable_get", self.getRuntime().newString(m)));
            }
            return args[0].callMethod(context,"map", new IRubyObject[]{self.callMethod(context, "taguri"),(IRubyObject)mep,self.callMethod(context, "to_yaml_style")});
        }
        public IRubyObject method7(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return self.getRuntime().newString("tag:yaml.org,2002:str");
        }
        public IRubyObject method8(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return args[0].callMethod(context,"scalar", new IRubyObject[]{self.callMethod(context, "taguri"),self.callMethod(context, "inspect"),self.callMethod(context, "to_yaml_style")});
        }
        public IRubyObject method9(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            String val = self.toString();
            if("Infinity".equals(val)) {
                val = ".Inf";
            } else if("-Infinity".equals(val)) {
                val = "-.Inf";
            } else if("NaN".equals(val)) {
                val = ".NaN";
            }
            return args[0].callMethod(context,"scalar", new IRubyObject[]{self.callMethod(context, "taguri"),self.getRuntime().newString(val),self.callMethod(context, "to_yaml_style")});
        }
    }

    public static class ToYAMLNodeStub2 implements MultiStub {
        public MultiStubMethod range_taguri;
        public MultiStubMethod range_to_yaml_node;

        public MultiStubMethod regexp_taguri;
        public MultiStubMethod regexp_to_yaml_node;

        public MultiStubMethod time_taguri;
        public MultiStubMethod time_to_yaml_node;

        public MultiStubMethod date_taguri;
        public MultiStubMethod date_to_yaml_node;

        public MultiStubMethod fixnum_taguri;
        public MultiStubMethod float_taguri;

        public IRubyObject method0(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            Map mep = (Map)(new RubyHash(self.getRuntime()));
            mep.put(self.getRuntime().newString("begin"),self.callMethod(context, "begin"));
            mep.put(self.getRuntime().newString("end"),self.callMethod(context, "end"));
            mep.put(self.getRuntime().newString("excl"),self.callMethod(context, "exclude_end?"));
            for(Iterator iter = ((RubyArray)self.callMethod(context, "to_yaml_properties")).getList().iterator(); iter.hasNext();) {
                String m = iter.next().toString();
                mep.put(self.getRuntime().newString(m.substring(1)), self.callMethod(context,"instance_variable_get", self.getRuntime().newString(m)));
            }
            return args[0].callMethod(context,"map", new IRubyObject[]{self.callMethod(context, "taguri"),(IRubyObject)mep,self.callMethod(context, "to_yaml_style")});
        }
        public IRubyObject method1(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return self.getRuntime().newString("!ruby/range");
        }
        public IRubyObject method2(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return args[0].callMethod(context,"scalar", new IRubyObject[]{self.callMethod(context, "taguri"),self.callMethod(context, "inspect"),self.callMethod(context, "to_yaml_style")});
        }
        public IRubyObject method3(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return self.getRuntime().newString("!ruby/regexp");
        }
        public IRubyObject method4(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            IRubyObject tz = self.getRuntime().newString("Z");
            IRubyObject difference_sign = self.getRuntime().newString("-");
            if(!self.callMethod(context, "utc?").isTrue()) {
                IRubyObject utc_same_instant = self.callMethod(context, "dup").callMethod(context, "utc");
                IRubyObject utc_same_writing = self.getRuntime().getClass("Time").callMethod(context,"utc", new IRubyObject[]{
                        self.callMethod(context, "year"),self.callMethod(context, "month"),self.callMethod(context, "day"),self.callMethod(context, "hour"),
                        self.callMethod(context, "min"),self.callMethod(context, "sec"),self.callMethod(context, "usec")});
                IRubyObject difference_to_utc = utc_same_writing.callMethod(context,"-", utc_same_instant);
                IRubyObject absolute_difference;
                if(difference_to_utc.callMethod(context,"<", RubyFixnum.zero(self.getRuntime())).isTrue()) {
                    difference_sign = self.getRuntime().newString("-");
                    absolute_difference = RubyFixnum.zero(self.getRuntime()).callMethod(context,"-", difference_to_utc);
                } else {
                    difference_sign = self.getRuntime().newString("+");
                    absolute_difference = difference_to_utc;
                }
                IRubyObject difference_minutes = absolute_difference.callMethod(context,"/", self.getRuntime().newFixnum(60)).callMethod(context, "round");
                tz = self.getRuntime().newString("%s%02d:%02d").callMethod(context,"%", self.getRuntime().newArray(new IRubyObject[]{difference_sign,difference_minutes.callMethod(context,"/", self.getRuntime().newFixnum(60)),difference_minutes.callMethod(context,"%", self.getRuntime().newFixnum(60))}));
            }
            IRubyObject standard = self.callMethod(context,"strftime", self.getRuntime().newString("%Y-%m-%d %H:%M:%S"));
            if(self.callMethod(context, "usec").callMethod(context, "nonzero?").isTrue()) {
                standard = standard.callMethod(context, "+", self.getRuntime().newString(".%06d").callMethod(context,"%", self.getRuntime().newArray(self.callMethod(context, "usec"))));
            }
            standard = standard.callMethod(context,"+", self.getRuntime().newString(" %s").callMethod(context,"%", self.getRuntime().newArray(tz)));
            return args[0].callMethod(context,"scalar", new IRubyObject[]{self.callMethod(context, "taguri"),standard,self.callMethod(context, "to_yaml_style")});
        }
        public IRubyObject method5(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return self.getRuntime().newString("tag:yaml.org,2002:timestamp");
        }
        public IRubyObject method6(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return args[0].callMethod(context,"scalar", new IRubyObject[]{self.callMethod(context, "taguri"),self.callMethod(context, "to_s"),self.callMethod(context, "to_yaml_style")});
        }
        public IRubyObject method7(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return self.getRuntime().newString("tag:yaml.org,2002:timestamp#ymd");
        }
        public IRubyObject method8(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return self.getRuntime().newString("tag:yaml.org,2002:int");
        }
        public IRubyObject method9(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return self.getRuntime().newString("tag:yaml.org,2002:float");
        }
    }

    public static class ToYAMLNodeStub3 implements MultiStub {
        public MultiStubMethod true_taguri;
        public MultiStubMethod true_to_yaml_node;
        public MultiStubMethod false_taguri;
        public MultiStubMethod false_to_yaml_node;
        public MultiStubMethod nil_taguri;
        public MultiStubMethod nil_to_yaml_node;

        public IRubyObject method0(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return args[0].callMethod(context,"scalar", new IRubyObject[]{self.callMethod(context, "taguri"),self.callMethod(context, "to_s"),self.callMethod(context, "to_yaml_style")});
        }
        public IRubyObject method1(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return self.getRuntime().newString("tag:yaml.org,2002:bool");
        }
        public IRubyObject method2(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return args[0].callMethod(context,"scalar", new IRubyObject[]{self.callMethod(context, "taguri"),self.callMethod(context, "to_s"),self.callMethod(context, "to_yaml_style")});
        }
        public IRubyObject method3(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return self.getRuntime().newString("tag:yaml.org,2002:bool");
        }
        public IRubyObject method4(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return args[0].callMethod(context,"scalar", new IRubyObject[]{self.callMethod(context, "taguri"),self.getRuntime().newString(""),self.callMethod(context, "to_yaml_style")});
        }
        public IRubyObject method5(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return self.getRuntime().newString("tag:yaml.org,2002:null");
        }
        public IRubyObject method6(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return null;
        }
        public IRubyObject method7(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return null;
        }
        public IRubyObject method8(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return null;
        }
        public IRubyObject method9(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return null;
        }
    }
}// RubyYAML
