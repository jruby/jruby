require 'bitescript'
require File.dirname(__FILE__) + '/signature'

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

JAVA_CLASSNAME = ARGV[0]
RUBY_CLASSNAME = ARGV[1]
RUBY_FILENAME = ARGV[2]

unless JAVA_CLASSNAME && RUBY_CLASSNAME && RUBY_FILENAME
  puts "usage: compiler2 <java class name> <ruby class name> <ruby library>"
  exit 1
end

require RUBY_FILENAME
RUBY_CLASS = eval(RUBY_CLASSNAME)

BiteScript.bytecode_version = BiteScript::JAVA1_5

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

file = BiteScript::FileBuilder.build("#{JAVA_CLASSNAME}.java.rb") do
  public_class JAVA_CLASSNAME, RubyObject do
    static_init do
      invokestatic Ruby, "getGlobalRuntime", [Ruby]
      invokevirtual Ruby, "getLoadService", [LoadService]
      ldc RUBY_FILENAME
      invokevirtual LoadService, "require", [boolean, string]
      returnvoid
    end
    public_constructor do
      aload 0
      invokestatic Ruby, "getGlobalRuntime", [Ruby]
      dup
      ldc RUBY_CLASSNAME
      invokevirtual Ruby, "getClass", [RubyClass, string]
      invokespecial RubyObject, "<init>", [void, Ruby, RubyClass]
      returnvoid
    end

    for method_name in RUBY_CLASS.public_instance_methods(false) do
      method = RUBY_CLASS.instance_method(method_name)
      signature = RUBY_CLASS.signatures[method_name]
      
      if signature
        raise "signatures only supported for exact arities: #{RUBY_CLASS.to_s + '#' + method_name}" if method.arity < 0
        params = signature.keys[0]
        retval = signature[params]
        use_ji = true
      else
        params = (method.arity < 0) ? [IRubyObject[]] : [IRubyObject] * method.arity
        retval = IRubyObject
      end
      
      public_method method_name, retval, *params do
        aload 0
        dup
        invokeinterface IRubyObject, "getRuntime", [Ruby]
        ruby_index = first_local(params)
        dup; astore ruby_index
        invokevirtual Ruby, "getCurrentContext", [ThreadContext]
        ldc method_name
        # TODO: arity-specific call
        if use_ji
          # We have a signature and need to use java integration logic
          ldc method.arity
          anewarray IRubyObject
          index = 1
          1.upto(method.arity) do |i|
            dup
            ldc i - 1
            aload ruby_index
            param_type = params[i - 1]
            if [boolean, byte, short, char, int].include? param_type
              iload index
              invokestatic JavaUtil, "convertJavaToRuby", [IRubyObject, Ruby, int]
            elsif long == param_type
              lload index
              invokestatic JavaUtil, "convertJavaToRuby", [IRubyObject, Ruby, long]
              index += 1
            elsif float == param_type
              fload index
              invokestatic JavaUtil, "convertJavaToRuby", [IRubyObject, Ruby, float]
            elsif double == param_type
              dload index
              invokestatic JavaUtil, "convertJavaToRuby", [IRubyObject, Ruby, double]
              index += 1
            else
              aload i
              invokestatic JavaUtil, "convertJavaToUsableRubyObject", [IRubyObject, Ruby, object]
            end
            aastore
            index += 1
          end
        else
          if method.arity < 0
            # restarg or optarg, just pass array through
            aload 1
          else
            # all normal args, box them up
            ldc method.arity
            anewarray IRubyObject
            i = 1;
            1.upto(method.arity) do |i|
              dup
              ldc i - 1
              aload i
              aastore
            end
          end
        end
        invokevirtual RubyBasicObject, "callMethod", [IRubyObject, ThreadContext, string, IRubyObject[]]
        if use_ji
          if boolean == retval
            invokestatic JavaUtil, "convertRubyToJavaBoolean", [boolean, IRubyObject]
            ireturn
          elsif byte == retval
            invokestatic JavaUtil, "convertRubyToJavaByte", [byte, IRubyObject]
            ireturn
          elsif short == retval
            invokestatic JavaUtil, "convertRubyToJavaShort", [short, IRubyObject]
            ireturn
          elsif char == retval
            invokestatic JavaUtil, "convertRubyToJavaChar", [char, IRubyObject]
            ireturn
          elsif int == retval
            invokestatic JavaUtil, "convertRubyToJavaInt", [int, IRubyObject]
            ireturn
          elsif long == retval
            invokestatic JavaUtil, "convertRubyToJavaLong", [long, IRubyObject]
            lreturn
          elsif float == retval
            invokestatic JavaUtil, "convertRubyToJavaFloat", [float, IRubyObject]
            freturn
          elsif double == retval
            invokestatic JavaUtil, "convertRubyToJavaDouble", [double, IRubyObject]
            dreturn
          elsif retval == void
            pop
            returnvoid
          else
            ldc retval
            invokestatic JavaUtil, "convertRubyToJava", [JObject, IRubyObject, JClass]
            areturn
          end
        else
          areturn
        end
      end
    end
  end
end

file.generate do |name, builder|
  File.open(name, 'w') do |f|
    f.write(builder.generate)
  end
end