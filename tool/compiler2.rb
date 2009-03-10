require 'bitescript'

RubyObject = org.jruby.RubyObject
RubyBasicObject = org.jruby.RubyBasicObject
Ruby = org.jruby.Ruby
RubyClass = org.jruby.RubyClass
IRubyObject = org.jruby.runtime.builtin.IRubyObject
ThreadContext = org.jruby.runtime.ThreadContext
LoadService = org.jruby.runtime.load.LoadService

JAVA_CLASSNAME = ARGV[0]
RUBY_CLASSNAME = ARGV[1]
RUBY_FILENAME = ARGV[2]

unless JAVA_CLASSNAME && RUBY_CLASSNAME && RUBY_FILENAME
  puts "usage: compiler2 <java class name> <ruby class name> <ruby library>"
  exit 1
end

require RUBY_FILENAME
RUBY_CLASS = eval(RUBY_CLASSNAME)

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
      params = (method.arity < 0) ? [IRubyObject[]] : [IRubyObject] * method.arity
      public_method method_name, IRubyObject, *params do
        aload 0
        dup
        invokeinterface IRubyObject, "getRuntime", [Ruby]
        invokevirtual Ruby, "getCurrentContext", [ThreadContext]
        ldc method_name
        # TODO: arity-specific call
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
        invokevirtual RubyBasicObject, "callMethod", [IRubyObject, ThreadContext, string, IRubyObject[]]
        areturn
      end
    end
  end
end

file.generate do |name, builder|
  File.open(name, 'w') do |f|
    f.write(builder.generate)
  end
end