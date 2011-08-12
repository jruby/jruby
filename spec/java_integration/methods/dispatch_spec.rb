require File.dirname(__FILE__) + "/../spec_helper"

java_import "java_integration.fixtures.ClassWithVarargs"
java_import "java_integration.fixtures.CoreTypeMethods"
java_import "java_integration.fixtures.StaticMethodSelection"

describe "Non-overloaded static Java methods" do
  it "should raise ArgumentError when called with incorrect arity" do
    lambda do
      java.util.Collections.empty_list('foo')
    end.should raise_error(ArgumentError)
  end
end

describe "An overloaded Java static method" do
  it "should be called with the most exact overload" do
    obj = java.lang.Integer.new(1)
    CoreTypeMethods.getType(1).should == "long"
    CoreTypeMethods.getType(1, obj).should == "long,object"
    CoreTypeMethods.getType(1, obj, obj).should == "long,object,object"
    CoreTypeMethods.getType(1, obj, obj, obj).should == "long,object,object,object"
    CoreTypeMethods.getType(1.0).should == "double"
    CoreTypeMethods.getType(1.0, obj).should == "double,object"
    CoreTypeMethods.getType(1.0, obj, obj).should == "double,object,object"
    CoreTypeMethods.getType(1.0, obj, obj, obj).should == "double,object,object,object"
    
    obj = "foo"
    CoreTypeMethods.getType(1).should == "long"
    CoreTypeMethods.getType(1, obj).should == "long,string"
    CoreTypeMethods.getType(1, obj, obj).should == "long,string,string"
    CoreTypeMethods.getType(1, obj, obj, obj).should == "long,string,string,string"
    CoreTypeMethods.getType(1.0).should == "double"
    CoreTypeMethods.getType(1.0, obj).should == "double,string"
    CoreTypeMethods.getType(1.0, obj, obj).should == "double,string,string"
    CoreTypeMethods.getType(1.0, obj, obj, obj).should == "double,string,string,string"
  end

  it "should raise error when called with too many args" do
    lambda do
      obj = java.lang.Integer.new(1)
      CoreTypeMethods.getType(1, obj, obj, obj, obj)
    end.should raise_error(ArgumentError)
      
    lambda do
      obj = "foo"
      CoreTypeMethods.getType(1, obj, obj, obj, obj)
    end.should raise_error(ArgumentError)
  end

  it "should raise error when called with too few args" do
    lambda do
      CoreTypeMethods.getType()
    end.should raise_error(ArgumentError)

    lambda do
      CoreTypeMethods.getType()
    end.should raise_error(ArgumentError)
  end
end

describe "The return value of an overridden Java static method" do
  before(:each) do
    @return_value = StaticMethodSelection.produce
  end
  it "should not be nil" do
    @return_value.should_not be_nil
  end
  it "should be of the correct type" do
    @return_value.should be_an_instance_of(StaticMethodSelection)
  end 
end

describe "An overloaded Java instance method" do
  it "should be called with the most exact overload" do
    obj = java.lang.Integer.new(1)
    ctm = CoreTypeMethods.new
    ctm.getTypeInstance(1).should == "long"
    ctm.getTypeInstance(1, obj).should == "long,object"
    ctm.getTypeInstance(1, obj, obj).should == "long,object,object"
    ctm.getTypeInstance(1, obj, obj, obj).should == "long,object,object,object"
    ctm.getTypeInstance(1.0).should == "double"
    ctm.getTypeInstance(1.0, obj).should == "double,object"
    ctm.getTypeInstance(1.0, obj, obj).should == "double,object,object"
    ctm.getTypeInstance(1.0, obj, obj, obj).should == "double,object,object,object"
    
    obj = "foo"
    ctm = CoreTypeMethods.new
    ctm.getTypeInstance(1).should == "long"
    ctm.getTypeInstance(1, obj).should == "long,string"
    ctm.getTypeInstance(1, obj, obj).should == "long,string,string"
    ctm.getTypeInstance(1, obj, obj, obj).should == "long,string,string,string"
    ctm.getTypeInstance(1.0).should == "double"
    ctm.getTypeInstance(1.0, obj).should == "double,string"
    ctm.getTypeInstance(1.0, obj, obj).should == "double,string,string"
    ctm.getTypeInstance(1.0, obj, obj, obj).should == "double,string,string,string"
  end

  it "should raise error when called with too many args" do
    lambda do
      obj = java.lang.Integer.new(1)
      CoreTypeMethods.new.getTypeInstance(1, obj, obj, obj, obj)
    end.should raise_error(ArgumentError)
      
    lambda do
      obj = "foo"
      CoreTypeMethods.new.getTypeInstance(1, obj, obj, obj, obj)
    end.should raise_error(ArgumentError)
  end

  it "should raise error when called with too few args" do
    lambda do
      CoreTypeMethods.new.getTypeInstance()
    end.should raise_error(ArgumentError)

    lambda do
      CoreTypeMethods.new.getTypeInstance()
    end.should raise_error(ArgumentError)
  end
end

describe "A class with varargs constructors" do
  it "should be called with the most exact overload" do
    obj = ClassWithVarargs.new()
    obj.constructor.should == "0: []"
    obj = ClassWithVarargs.new(1)
    obj.constructor.should == "0: [1]"
    obj = ClassWithVarargs.new(1,2)
    obj.constructor.should == "0: [1, 2]"
    obj = ClassWithVarargs.new(1,2,3)
    obj.constructor.should == "0: [1, 2, 3]"
    obj = ClassWithVarargs.new(1,2,3,4)
    obj.constructor.should == "0: [1, 2, 3, 4]"

    pending("needs better type-driven ranking of overloads") do
      obj = ClassWithVarargs.new('foo')
      obj.constructor.should == "1: []"
    end
    obj = ClassWithVarargs.new('foo', 1)
    obj.constructor.should == "1: [1]"
    obj = ClassWithVarargs.new('foo', 1, 2)
    obj.constructor.should == "1: [1, 2]"
    obj = ClassWithVarargs.new('foo', 1, 2, 3)
    obj.constructor.should == "1: [1, 2, 3]"
    obj = ClassWithVarargs.new('foo', 1, 2, 3, 4)
    obj.constructor.should == "1: [1, 2, 3, 4]"

    pending("needs better type-driven ranking of overloads") do
      obj = ClassWithVarargs.new('foo', 'bar')
      obj.constructor.should == "2: []"
    end
    obj = ClassWithVarargs.new('foo', 'bar', 1)
    obj.constructor.should == "2: [1]"
    obj = ClassWithVarargs.new('foo', 'bar', 1, 2)
    obj.constructor.should == "2: [1, 2]"
    obj = ClassWithVarargs.new('foo', 'bar', 1, 2, 3)
    obj.constructor.should == "2: [1, 2, 3]"
    obj = ClassWithVarargs.new('foo', 'bar', 1, 2, 3, 4)
    obj.constructor.should == "2: [1, 2, 3, 4]"

    pending("needs better type-driven ranking of overloads") do
      obj = ClassWithVarargs.new('foo', 'bar', 'baz')
      obj.constructor.should == "3: []"
    end
    obj = ClassWithVarargs.new('foo', 'bar', 'baz', 1)
    obj.constructor.should == "3: [1]"
    obj = ClassWithVarargs.new('foo', 'bar', 'baz', 1, 2)
    obj.constructor.should == "3: [1, 2]"
    obj = ClassWithVarargs.new('foo', 'bar', 'baz', 1, 2, 3)
    obj.constructor.should == "3: [1, 2, 3]"
    obj = ClassWithVarargs.new('foo', 'bar', 'baz', 1, 2, 3, 4)
    obj.constructor.should == "3: [1, 2, 3, 4]"
  end

  it "should be callable with an array" do
    ClassWithVarargs.new([1,2,3].to_java).constructor.should == "0: [1, 2, 3]"
    ClassWithVarargs.new('foo', [1,2,3].to_java).constructor.should == "1: [1, 2, 3]"
    ClassWithVarargs.new('foo', 'bar', [1,2,3].to_java).constructor.should == "2: [1, 2, 3]"
    ClassWithVarargs.new('foo', 'bar', 'baz', [1,2,3].to_java).constructor.should == "3: [1, 2, 3]"
  end
end

describe "A class with varargs instance methods" do
  it "should be called with the most exact overload" do
    obj = ClassWithVarargs.new(1)

    obj.varargs().should == "0: []";
    obj.varargs(1).should == "0: [1]";
    obj.varargs(1,2).should == "0: [1, 2]";
    obj.varargs(1,2,3).should == "0: [1, 2, 3]";
    obj.varargs(1,2,3,4).should == "0: [1, 2, 3, 4]";

    pending("needs better type-driven ranking of overloads") do
      obj.varargs('foo').should == "1: []";
    end
    obj.varargs('foo', 1).should == "1: [1]";
    obj.varargs('foo', 1, 2).should == "1: [1, 2]";
    obj.varargs('foo', 1, 2, 3).should == "1: [1, 2, 3]";
    obj.varargs('foo', 1, 2, 3, 4).should == "1: [1, 2, 3, 4]";

    pending("needs better type-driven ranking of overloads") do
      obj.varargs('foo', 'bar').should == "2: []";
    end
    obj.varargs('foo', 'bar', 1).should == "2: [1]";
    obj.varargs('foo', 'bar', 1, 2).should == "2: [1, 2]";
    obj.varargs('foo', 'bar', 1, 2, 3).should == "2: [1, 2, 3]";
    obj.varargs('foo', 'bar', 1, 2, 3, 4).should == "2: [1, 2, 3, 4]";

    pending("needs better type-driven ranking of overloads") do
      obj.varargs('foo', 'bar', 'baz').should == "3: []";
    end
    obj.varargs('foo', 'bar', 'baz', 1).should == "3: [1]";
    obj.varargs('foo', 'bar', 'baz', 1, 2).should == "3: [1, 2]";
    obj.varargs('foo', 'bar', 'baz', 1, 2, 3).should == "3: [1, 2, 3]";
    obj.varargs('foo', 'bar', 'baz', 1, 2, 3, 4).should == "3: [1, 2, 3, 4]";
  end

  it "should be callable with an array" do
    obj = ClassWithVarargs.new(1)
    obj.varargs([1,2,3].to_java).should == "0: [1, 2, 3]"
    obj.varargs('foo', [1,2,3].to_java).should == "1: [1, 2, 3]"
    obj.varargs('foo', 'bar', [1,2,3].to_java).should == "2: [1, 2, 3]"
    obj.varargs('foo', 'bar', 'baz', [1,2,3].to_java).should == "3: [1, 2, 3]"
  end
end

describe "A class with varargs static methods" do
  it "should be called with the most exact overload" do
    ClassWithVarargs.varargs_static().should == "0: []";
    ClassWithVarargs.varargs_static(1).should == "0: [1]";
    ClassWithVarargs.varargs_static(1,2).should == "0: [1, 2]";
    ClassWithVarargs.varargs_static(1,2,3).should == "0: [1, 2, 3]";
    ClassWithVarargs.varargs_static(1,2,3,4).should == "0: [1, 2, 3, 4]";

    pending("needs better type-driven ranking of overloads") do
      ClassWithVarargs.varargs_static('foo').should == "1: []";
    end
    ClassWithVarargs.varargs_static('foo', 1).should == "1: [1]";
    ClassWithVarargs.varargs_static('foo', 1, 2).should == "1: [1, 2]";
    ClassWithVarargs.varargs_static('foo', 1, 2, 3).should == "1: [1, 2, 3]";
    ClassWithVarargs.varargs_static('foo', 1, 2, 3, 4).should == "1: [1, 2, 3, 4]";

    pending("needs better type-driven ranking of overloads") do
      ClassWithVarargs.varargs_static('foo', 'bar').should == "2: []";
    end
    ClassWithVarargs.varargs_static('foo', 'bar', 1).should == "2: [1]";
    ClassWithVarargs.varargs_static('foo', 'bar', 1, 2).should == "2: [1, 2]";
    ClassWithVarargs.varargs_static('foo', 'bar', 1, 2, 3).should == "2: [1, 2, 3]";
    ClassWithVarargs.varargs_static('foo', 'bar', 1, 2, 3, 4).should == "2: [1, 2, 3, 4]";

    pending("needs better type-driven ranking of overloads") do
      ClassWithVarargs.varargs_static('foo', 'bar').should == "3: []";
    end
    ClassWithVarargs.varargs_static('foo', 'bar', 'baz', 1).should == "3: [1]";
    ClassWithVarargs.varargs_static('foo', 'bar', 'baz', 1, 2).should == "3: [1, 2]";
    ClassWithVarargs.varargs_static('foo', 'bar', 'baz', 1, 2, 3).should == "3: [1, 2, 3]";
    ClassWithVarargs.varargs_static('foo', 'bar', 'baz', 1, 2, 3, 4).should == "3: [1, 2, 3, 4]";
  end

  it "should be callable with an array" do
    ClassWithVarargs.varargs_static([1,2,3].to_java).should == "0: [1, 2, 3]"
    ClassWithVarargs.varargs_static('foo', [1,2,3].to_java).should == "1: [1, 2, 3]"
    ClassWithVarargs.varargs_static('foo', 'bar', [1,2,3].to_java).should == "2: [1, 2, 3]"
    ClassWithVarargs.varargs_static('foo', 'bar', 'baz', [1,2,3].to_java).should == "3: [1, 2, 3]"
  end
end

# JRUBY-5418
describe "A Java method dispatch downstream from a Kernel#catch block" do
  it "should propagate rather than wrap the 'throw' exception" do
    lambda do
      catch(:foo) do
        UsesSingleMethodInterface.new { throw :foo }
      end
    end.should_not raise_error
    lambda do
      catch(:foo) do
        UsesSingleMethodInterface.new(nil) { throw :foo }
      end
    end.should_not raise_error
    lambda do
      catch(:foo) do
        UsesSingleMethodInterface.new(nil, nil) { throw :foo }
      end
    end.should_not raise_error
    lambda do
      catch(:foo) do
        UsesSingleMethodInterface.new(nil, nil, nil) { throw :foo }
      end
    end.should_not raise_error
    # 3 normal args is our cutoff for specific-arity optz, so test four
    lambda do
      catch(:foo) do
        UsesSingleMethodInterface.new(nil, nil, nil, nil) { throw :foo }
      end
    end.should_not raise_error
  end
end
