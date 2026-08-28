require File.dirname(__FILE__) + "/../spec_helper"

java_import "java_integration.fixtures.ManyArityMethodClass"

describe "Java constructors" do
  it "should fail correctly when called with wrong parameters" do 
    expect do 
      java.util.HashMap.new "str"
    end.to raise_error(NameError)
  end
end

describe "Java instance methods" do
  it "should fail correctly when called with wrong parameters" do 
    expect do 
      java.util.ArrayList.new.add_all "str"
    end.to raise_error(NameError)

    expect do 
      java.util.System.set_property 1, 2
    end.to raise_error(NameError)
  end
  
  describe "with fixed arity" do
    it "raises ArgumentError on arity mismatch" do
      # arity zero
      expect do
        ManyArityMethodClass.foo0('foo')
      end.to raise_error(ArgumentError)
      expect do
        ManyArityMethodClass.foo0('foo', 'foo')
      end.to raise_error(ArgumentError)
      expect do
        ManyArityMethodClass.foo0('foo', 'foo', 'foo')
      end.to raise_error(ArgumentError)
      expect do
        ManyArityMethodClass.foo0('foo', 'foo', 'foo', 'foo')
      end.to raise_error(ArgumentError)
      
      # arity one
      expect do
        ManyArityMethodClass.foo1()
      end.to raise_error(ArgumentError)
      expect do
        ManyArityMethodClass.foo1('foo', 'foo')
      end.to raise_error(ArgumentError)
      expect do
        ManyArityMethodClass.foo1('foo', 'foo', 'foo')
      end.to raise_error(ArgumentError)
      expect do
        ManyArityMethodClass.foo1('foo', 'foo', 'foo', 'foo')
      end.to raise_error(ArgumentError)
      
      # arity two
      expect do
        ManyArityMethodClass.foo2()
      end.to raise_error(ArgumentError)
      expect do
        ManyArityMethodClass.foo2('foo')
      end.to raise_error(ArgumentError)
      expect do
        ManyArityMethodClass.foo2('foo', 'foo', 'foo')
      end.to raise_error(ArgumentError)
      expect do
        ManyArityMethodClass.foo2('foo', 'foo', 'foo', 'foo')
      end.to raise_error(ArgumentError)
      
      # arity three
      expect do
        ManyArityMethodClass.foo3()
      end.to raise_error(ArgumentError)
      expect do
        ManyArityMethodClass.foo3('foo')
      end.to raise_error(ArgumentError)
      expect do
        ManyArityMethodClass.foo3('foo', 'foo')
      end.to raise_error(ArgumentError)
      expect do
        ManyArityMethodClass.foo3('foo', 'foo', 'foo', 'foo')
      end.to raise_error(ArgumentError)
      
      # arity four
      expect do
        ManyArityMethodClass.foo4()
      end.to raise_error(ArgumentError)
      expect do
        ManyArityMethodClass.foo4('foo')
      end.to raise_error(ArgumentError)
      expect do
        ManyArityMethodClass.foo4('foo', 'foo')
      end.to raise_error(ArgumentError)
      expect do
        ManyArityMethodClass.foo4('foo', 'foo', 'foo')
      end.to raise_error(ArgumentError)
    end
  end
  
  describe "with variable arity" do
    it "raises ArgumentError on arity mismatch" do
      # arity 0 or 1
      expect do
        ManyArityMethodClass.foo0_1('foo', 'foo')
      end.to raise_error(ArgumentError)
      expect do
        ManyArityMethodClass.foo0_1('foo', 'foo', 'foo')
      end.to raise_error(ArgumentError)
      expect do
        ManyArityMethodClass.foo0_1('foo', 'foo', 'foo', 'foo')
      end.to raise_error(ArgumentError)
      
      # arity 1 or 2
      expect do
        ManyArityMethodClass.foo1_2()
      end.to raise_error(ArgumentError)
      expect do
        ManyArityMethodClass.foo1_2('foo', 'foo', 'foo')
      end.to raise_error(ArgumentError)
      expect do
        ManyArityMethodClass.foo1_2('foo', 'foo', 'foo', 'foo')
      end.to raise_error(ArgumentError)
      
      # arity 2 or 3
      expect do
        ManyArityMethodClass.foo2_3()
      end.to raise_error(ArgumentError)
      expect do
        ManyArityMethodClass.foo2_3('foo')
      end.to raise_error(ArgumentError)
      expect do
        ManyArityMethodClass.foo2_3('foo', 'foo', 'foo', 'foo')
      end.to raise_error(ArgumentError)
      
      # arity 3 or 4
      expect do
        ManyArityMethodClass.foo3_4()
      end.to raise_error(ArgumentError)
      expect do
        ManyArityMethodClass.foo3_4('foo')
      end.to raise_error(ArgumentError)
      expect do
        ManyArityMethodClass.foo3_4('foo', 'foo')
      end.to raise_error(ArgumentError)
    end
  end
end
