# Specifications:
#
# Keywords:
#   class
#   def
#
# Behaviour:
#   Constants
#   Class variables
#   Class instance variables
#   Instance variables
#
require File.dirname(__FILE__) + '/../spec_helper'

describe "A new class definition" do
  it "should create a new class" do
    class A; end

    A.class.should == Class
    A.new.class.should == A
  end
  
  it "should have no class variables" do
    class B; end;
    
    B.class_variables.should == []
  end
  
  it "should have no class-level instance variables" do
    class C; end
    
    C.instance_variables.should == []
  end

  it "should allow the declaration of class variables in the body" do
    class D
      @@bar = 'foo'
    end
    
    D.class_variables.should == ["@@bar"]
  end
  
  it "should allow the declaration of class-level instance variables in the body" do
    class E
      @bar = 'foo'
    end
    
    E.instance_variables.should == ["@bar"]
  end

  it "should allow the declaration of class variables in a class method" do
    class F
      def self.setup_cv
        @@bar = 'foo'
      end
    end

    F.class_variables.should == []
    F.setup_cv
    F.class_variables.should == ["@@bar"]
  end
  
  it "should allow the declaration of class variables in an instance method" do
    class G
      def setup_cv
        @@bar = 'foo'
      end
    end

    G.class_variables.should == []
    G.new.setup_cv
    G.class_variables.should == ["@@bar"]
  end
  
  it "should allow the definition of methods" do
    class H
      def foo
        'foo'
      end
    end
    
    H.new.foo.should == 'foo'
  end
  
  it "should allow the definition of class methods" do
    class I
      def self.foo
        'foo'
      end
    end
    
    I.foo.should == 'foo'
  end
  
  it "should allow the definition of class methods using class << self" do
    class J
      class << self
        def foo
          'foo'
        end
      end
    end
    
    J.foo.should == 'foo'
  end
  
  it "should allow the definition of Constants" do
    class O; CONST = 'foo!'; end
    
    defined?(CONST).should == nil
    defined?(O::CONST).should == "constant"
    O::CONST.should == 'foo!'
  end
  
  it "should return the value of the last statement in the body" do
    value = class K; end
    value.should == nil
    
    value = class L; 20; end
    value.should == 20
    
    value = class M; 20 + 10; end
    value.should == 30
    
    value = class N; class << self; 'foo'; end; end
    value.should == 'foo'
  end
end

describe "Nested class definitions" do
  it "should make the outer class contain the inner classes" do
    class Z
      class A1; end
      class A2; end
    end
    
    Z.constants.include?('A1').should == true
    Z.constants.include?('A2').should == true
  end
end

describe "A Class Definitions extending an object" do
  it "should allow adding methods" do
    a = "a string"
    class << a
      def xyz
        self
      end
    end
    
    a.xyz.should == "a string"
  end
  
  it "should raise a TypeError when trying to extend numbers" do
    should_raise(TypeError) do
      eval <<-CODE
        class << 1
          def xyz
            self
          end
        end
      CODE
    end
  end
end

describe "Multiple Definitions of the same Class" do
  it "should extend previous definitions" do
    class X; def abc(); 'foo' end; end
    class X; def xyz(); 'bar' end; end
    
    x = X.new
    x.abc.should == 'foo'
    x.xyz.should == 'bar'
  end
  
  it "should overwrite existing methods" do
    class W; def abc() 'bar' end; end
    class W; def abc() 'foo' end; end
    
    W.new.abc.should == 'foo'
  end
  
  it "should raise a TypeError when superclasses mismatch" do
    should_raise(TypeError) do
      class V < Array; end
      class V < Fixnum; end
    end    
  end
end
