require 'bitescript'
require File.dirname(__FILE__) + '/signature'

module JRuby
  class JavaCompiler
    RubyObject = org.jruby.RubyObject
    RubyBasicObject = org.jruby.RubyBasicObject
    Ruby = org.jruby.Ruby
    RubyClass = org.jruby.RubyClass
    IRubyObject = org.jruby.runtime.builtin.IRubyObject
    ThreadContext = org.jruby.runtime.ThreadContext
    LoadService = org.jruby.runtime.load.LoadService
    JClass = java.lang.Class
    JavaUtil = org.jruby.javasupport.JavaUtil
    JObject = java.lang.Object

    def initialize(source_name)
      @file_builder = BiteScript::FileBuilder.new(source_name)
    end

    def write_files
      @file_builder.generate do |name, builder|
        File.open(name, 'w') do |f|
          f.write(builder.generate)
        end
      end
    end

    def process_class(ruby_class_path, ruby_class, java_name, require_file = nil, java_package = nil)
      if java_package
        cb = @file_builder.package(java_package).public_class(java_name, RubyObject);
      else
        cb = @file_builder.public_class(java_name, RubyObject);
      end

      # If a require file is specified, load it in static initializer
      if require_file
        cb.static_init do
          invokestatic Ruby, "getGlobalRuntime", [Ruby]
          invokevirtual Ruby, "getLoadService", [LoadService]
          ldc require_file
          invokevirtual LoadService, "require", [cb.boolean, cb.string]
          returnvoid
        end
      end

      cb.public_constructor do
        aload 0
        invokestatic Ruby, "getGlobalRuntime", [Ruby]
        dup
        ldc ruby_class_path
        invokevirtual Ruby, "getClass", [RubyClass, cb.string]
        invokespecial RubyObject, "<init>", [cb.void, Ruby, RubyClass]
        returnvoid
      end

      for method_name in ruby_class.public_instance_methods(false) do
        method = ruby_class.instance_method(method_name)
        signature = if ruby_class.respond_to? :signatures
          ruby_class.signatures[method_name]
        else
          nil
        end
        annotations = if ruby_class.respond_to? :annotations
          ruby_class.annotations[method_name]
        else
          nil
        end

        create_method(cb, method_name, method, signature, annotations)
      end
    end

    def create_method(cb, method_name, method, signature = nil, annotations = nil)
      if signature
        raise "signatures only supported for exact arities: #{RUBY_CLASS.to_s + '#' + method_name}" if method.arity < 0
        params = signature.keys[0]
        retval = signature[params]
      else
        params = (method.arity < 0) ? [IRubyObject[]] : [IRubyObject] * method.arity
        retval = IRubyObject
      end

      mb = cb.public_method method_name, retval, *params

      mb.start

      if annotations && annotations.size > 0
        # define annotations
        annotations.each do |anno_cls, anno_data|
          mb.annotate(anno_cls, true) do |anno|
            anno_data.each {|k,v| anno.send(k + "=", v)} if anno_data
          end
        end
      end

      # prepare receiver, context, and method name for callMethod later
      mb.aload 0
      mb.dup
      mb.invokeinterface IRubyObject, "getRuntime", [Ruby]
      ruby_index = first_local(params)
      mb.dup; mb.astore ruby_index
      mb.invokevirtual Ruby, "getCurrentContext", [ThreadContext]
      mb.ldc method_name

      # TODO: arity-specific calling
      if signature
        java_args(mb, method, params, ruby_index)
      else
        ruby_args(mb, method)
      end

      # invoke the method dynamically
      # TODO: this could have a simple inline cache
      mb.invokevirtual RubyBasicObject, "callMethod", [IRubyObject, ThreadContext, mb.string, IRubyObject[]]

      # return the result
      if signature
        java_return(mb, retval)
      else
        ruby_return(mb, retval)
      end

      mb.stop
    end

    def first_local(params, instance = true)
      i = instance ? 1 : 0
      params.each do |param|
        if param == Java::long || param == Java::double
          i += 2
        else
          i += 1
        end
      end
      i
    end

    def java_args(mb, method, params, ruby_index)
      # We have a signature and need to use java integration logic
      mb.ldc method.arity
      mb.anewarray IRubyObject
      index = 1
      1.upto(method.arity) do |i|
        mb.dup
        mb.ldc i - 1
        mb.aload ruby_index
        param_type = params[i - 1]
        if [mb.boolean, mb.byte, mb.short, mb.char, mb.int].include? param_type
          mb.iload index
          mb.invokestatic JavaUtil, "convertJavaToRuby", [IRubyObject, Ruby, mb.int]
        elsif mb.long == param_type
          mb.lload index
          mb.invokestatic JavaUtil, "convertJavaToRuby", [IRubyObject, Ruby, mb.long]
          index += 1
        elsif mb.float == param_type
          mb.fload index
          mb.invokestatic JavaUtil, "convertJavaToRuby", [IRubyObject, Ruby, mb.float]
        elsif mb.double == param_type
          mb.dload index
          mb.invokestatic JavaUtil, "convertJavaToRuby", [IRubyObject, Ruby, mb.double]
          index += 1
        else
          mb.aload i
          mb.invokestatic JavaUtil, "convertJavaToUsableRubyObject", [IRubyObject, Ruby, mb.object]
        end
        mb.aastore
        index += 1
      end
    end

    def ruby_args(mb, method)
      if method.arity < 0
        # restarg or optarg, just pass array through
        mb.aload 1
      else
        # all normal args, box them up
        mb.ldc method.arity
        mb.anewarray IRubyObject
        i = 1;
        1.upto(method.arity) do |i|
          mb.dup
          mb.ldc i - 1
          mb.aload i
          mb.aastore
        end
      end
    end

    def ruby_return(mb, retval)
      mb.areturn
    end

    def java_return(mb, retval)
      if mb.boolean == retval
        mb.invokestatic JavaUtil, "convertRubyToJavaBoolean", [mb.boolean, IRubyObject]
        mb.ireturn
      elsif mb.byte == retval
        mb.invokestatic JavaUtil, "convertRubyToJavaByte", [mb.byte, IRubyObject]
        mb.ireturn
      elsif mb.short == retval
        mb.invokestatic JavaUtil, "convertRubyToJavaShort", [mb.short, IRubyObject]
        mb.ireturn
      elsif mb.char == retval
        mb.invokestatic JavaUtil, "convertRubyToJavaChar", [mb.char, IRubyObject]
        mb.ireturn
      elsif mb.int == retval
        mb.invokestatic JavaUtil, "convertRubyToJavaInt", [mb.int, IRubyObject]
        mb.ireturn
      elsif mb.long == retval
        mb.invokestatic JavaUtil, "convertRubyToJavaLong", [mb.long, IRubyObject]
        mb.lreturn
      elsif mb.float == retval
        mb.invokestatic JavaUtil, "convertRubyToJavaFloat", [mb.float, IRubyObject]
        mb.freturn
      elsif mb.double == retval
        mb.invokestatic JavaUtil, "convertRubyToJavaDouble", [mb.double, IRubyObject]
        mb.dreturn
      elsif retval == mb.void || retval == nil
        mb.pop
        mb.returnvoid
      else
        mb.ldc retval
        mb.invokestatic JavaUtil, "convertRubyToJava", [JObject, IRubyObject, JClass]
        mb.areturn
      end
    end
  end
end

if $0 == __FILE__
  java_classname = ARGV[0]
  ruby_classname = ARGV[1]
  ruby_filename = ARGV[2]

  unless java_classname && ruby_classname && ruby_filename
    puts "usage: compiler2 <java class name> <ruby class name> <ruby library>"
    exit 1
  end

  require ruby_filename
  ruby_class = eval(ruby_classname)

  java_compiler = JRuby::JavaCompiler.new(ruby_filename)
  java_compiler.process_class(ruby_classname, ruby_class, java_classname, ruby_filename)

  java_compiler.write_files
end