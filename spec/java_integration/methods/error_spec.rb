require File.dirname(__FILE__) + "/../spec_helper"

java_import "java_integration.fixtures.ManyArityMethodClass"

describe "Java constructors" do
  it "should fail correctly when called with wrong parameters" do 
    proc do 
      java.util.HashMap.new "str"
    end.should raise_error(NameError)
  end
end

describe "Java instance methods" do
  it "should fail correctly when called with wrong parameters" do 
    proc do 
      java.util.ArrayList.new.add_all "str"
    end.should raise_error(NameError)

    proc do 
      java.util.System.set_property 1, 2
    end.should raise_error(NameError)
  end
  
  describe "with fixed arity" do
    it "raises ArgumentError on arity mismatch" do
      # arity zero
      proc do
        ManyArityMethodClass.foo0('foo')
      end.should raise_error(ArgumentError)
      proc do
        ManyArityMethodClass.foo0('foo', 'foo')
      end.should raise_error(ArgumentError)
      proc do
        ManyArityMethodClass.foo0('foo', 'foo', 'foo')
      end.should raise_error(ArgumentError)
      proc do
        ManyArityMethodClass.foo0('foo', 'foo', 'foo', 'foo')
      end.should raise_error(ArgumentError)
      
      # arity one
      proc do
        ManyArityMethodClass.foo1()
      end.should raise_error(ArgumentError)
      proc do
        ManyArityMethodClass.foo1('foo', 'foo')
      end.should raise_error(ArgumentError)
      proc do
        ManyArityMethodClass.foo1('foo', 'foo', 'foo')
      end.should raise_error(ArgumentError)
      proc do
        ManyArityMethodClass.foo1('foo', 'foo', 'foo', 'foo')
      end.should raise_error(ArgumentError)
      
      # arity two
      proc do
        ManyArityMethodClass.foo2()
      end.should raise_error(ArgumentError)
      proc do
        ManyArityMethodClass.foo2('foo')
      end.should raise_error(ArgumentError)
      proc do
        ManyArityMethodClass.foo2('foo', 'foo', 'foo')
      end.should raise_error(ArgumentError)
      proc do
        ManyArityMethodClass.foo2('foo', 'foo', 'foo', 'foo')
      end.should raise_error(ArgumentError)
      
      # arity three
      proc do
        ManyArityMethodClass.foo3()
      end.should raise_error(ArgumentError)
      proc do
        ManyArityMethodClass.foo3('foo')
      end.should raise_error(ArgumentError)
      proc do
        ManyArityMethodClass.foo3('foo', 'foo')
      end.should raise_error(ArgumentError)
      proc do
        ManyArityMethodClass.foo3('foo', 'foo', 'foo', 'foo')
      end.should raise_error(ArgumentError)
      
      # arity four
      proc do
        ManyArityMethodClass.foo4()
      end.should raise_error(ArgumentError)
      proc do
        ManyArityMethodClass.foo4('foo')
      end.should raise_error(ArgumentError)
      proc do
        ManyArityMethodClass.foo4('foo', 'foo')
      end.should raise_error(ArgumentError)
      proc do
        ManyArityMethodClass.foo4('foo', 'foo', 'foo')
      end.should raise_error(ArgumentError)
    end
  end
  
  describe "with variable arity" do
    it "raises ArgumentError on arity mismatch" do
      # arity 0 or 1
      proc do
        ManyArityMethodClass.foo0_1('foo', 'foo')
      end.should raise_error(ArgumentError)
      proc do
        ManyArityMethodClass.foo0_1('foo', 'foo', 'foo')
      end.should raise_error(ArgumentError)
      proc do
        ManyArityMethodClass.foo0_1('foo', 'foo', 'foo', 'foo')
      end.should raise_error(ArgumentError)
      
      # arity 1 or 2
      proc do
        ManyArityMethodClass.foo1_2()
      end.should raise_error(ArgumentError)
      proc do
        ManyArityMethodClass.foo1_2('foo', 'foo', 'foo')
      end.should raise_error(ArgumentError)
      proc do
        ManyArityMethodClass.foo1_2('foo', 'foo', 'foo', 'foo')
      end.should raise_error(ArgumentError)
      
      # arity 2 or 3
      proc do
        ManyArityMethodClass.foo2_3()
      end.should raise_error(ArgumentError)
      proc do
        ManyArityMethodClass.foo2_3('foo')
      end.should raise_error(ArgumentError)
      proc do
        ManyArityMethodClass.foo2_3('foo', 'foo', 'foo', 'foo')
      end.should raise_error(ArgumentError)
      
      # arity 3 or 4
      proc do
        ManyArityMethodClass.foo3_4()
      end.should raise_error(ArgumentError)
      proc do
        ManyArityMethodClass.foo3_4('foo')
      end.should raise_error(ArgumentError)
      proc do
        ManyArityMethodClass.foo3_4('foo', 'foo')
      end.should raise_error(ArgumentError)
    end
  end
end
