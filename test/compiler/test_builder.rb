require 'test/unit'
require 'compiler/builder'
require 'compiler/signature'

class TestBuilder < Test::Unit::TestCase
  import java.lang.String
  import java.util.ArrayList
  import java.lang.Void
  import java.lang.Object
  
  include Compiler::Signature
  
  def test_class_builder
    cb = Compiler::ClassBuilder.build("MyClass", "MyClass.java") do
      constructor(String[], ArrayList) do
        invokespecial Object, "<init>", Void::TYPE
        aload 1
        ldc 1
        aaload
        invokestatic "MyClass", :foo, [Void::TYPE, String]
      end
      
      static_method(:foo, Void::TYPE, String) do
        aload 1
        aprintln
      end
    end
    
    cb.write("MyClass.class")
  end
end