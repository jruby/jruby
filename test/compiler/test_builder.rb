require 'test/unit'
require 'compiler/builder'
require 'compiler/signature'

class TestBuilder < Test::Unit::TestCase
  import java.lang.String
  import java.util.ArrayList
  import java.lang.Void
  import java.lang.Object
  import java.lang.Boolean
  
  include Compiler::Signature
  
  def test_class_builder
    class_bytes = Compiler::ClassBuilder.build("MyClass", "MyClass.java") do
      field :list, ArrayList
      
      constructor(String, ArrayList) do
        aload 0
        invokespecial Object, "<init>", Void::TYPE
        aload 0
        aload 1
        aload 2
        invokevirtual this, :bar, [ArrayList, String, ArrayList]
        aload 0
        swap
        putfield this, :list, ArrayList
        returnvoid
      end
      
      static_method(:foo, this, String) do
        new this
        dup
        aload 0
        new ArrayList
        dup
        invokespecial ArrayList, "<init>", Void::TYPE
        invokespecial this, "<init>", [Void::TYPE, String, ArrayList]
        areturn
      end
      
      method(:bar, ArrayList, String, ArrayList) do
        aload 1
        invokevirtual(String, :toLowerCase, String)
        aload 2
        swap
        invokevirtual(ArrayList, :add, [Boolean::TYPE, Object])
        aload 2
        areturn
      end
      
      method(:getList, ArrayList) do
        aload 0
        getfield this, :list, ArrayList
        areturn
      end
      
      static_method(:main, Void::TYPE, String[]) do
        aload 0
        ldc_int 0
        aaload
        invokestatic this, :foo, [this, String]
        invokevirtual this, :getList, ArrayList
        aprintln
        returnvoid
      end
    end
    
    File.open("MyClass.class", "w") {|file| file.write(class_bytes)}
  end
end