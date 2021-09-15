require File.dirname(__FILE__) + "/../spec_helper"

java_import "java_integration.fixtures.ClassWithVarargs"
java_import "java_integration.fixtures.ClassWithPrimitiveVarargs"
java_import "java_integration.fixtures.CoreTypeMethods"
java_import "java_integration.fixtures.StaticMethodSelection"
java_import "java_integration.fixtures.UsesSingleMethodInterface"

describe "Non-overloaded static Java methods" do
  it "should raise ArgumentError when called with incorrect arity" do
    expect do
      java.util.Collections.empty_list('foo')
    end.to raise_error(ArgumentError)
  end
end

describe "An overloaded Java static method" do
  it "should be called with the most exact overload" do
    obj = java.lang.Integer.new(1)
    expect(CoreTypeMethods.getType(1)).to eq("long")
    expect(CoreTypeMethods.getType(1, obj)).to eq("long,object")
    expect(CoreTypeMethods.getType(1, obj, obj)).to eq("long,object,object")
    expect(CoreTypeMethods.getType(1, obj, obj, obj)).to eq("long,object,object,object")
    expect(CoreTypeMethods.getType(1.0)).to eq("double")
    expect(CoreTypeMethods.getType(1.0, obj)).to eq("double,object")
    expect(CoreTypeMethods.getType(1.0, obj, obj)).to eq("double,object,object")
    expect(CoreTypeMethods.getType(1.0, obj, obj, obj)).to eq("double,object,object,object")

    obj = "foo"
    expect(CoreTypeMethods.getType(1)).to eq("long")
    expect(CoreTypeMethods.getType(1, obj)).to eq("long,string")
    expect(CoreTypeMethods.getType(1, obj, obj)).to eq("long,string,string")
    expect(CoreTypeMethods.getType(1, obj, obj, obj)).to eq("long,string,string,string")
    expect(CoreTypeMethods.getType(1.0)).to eq("double")
    expect(CoreTypeMethods.getType(1.0, obj)).to eq("double,string")
    expect(CoreTypeMethods.getType(1.0, obj, obj)).to eq("double,string,string")
    expect(CoreTypeMethods.getType(1.0, obj, obj, obj)).to eq("double,string,string,string")
    expect(CoreTypeMethods.getType(1.0, obj.to_java)).to eq("double,string")
  end

  it "should raise error when called with too many args" do
    expect do
      obj = java.lang.Integer.new(1)
      CoreTypeMethods.getType(1, obj, obj, obj, obj)
    end.to raise_error(ArgumentError)

    expect do
      obj = "foo"
      CoreTypeMethods.getType(1, obj, obj, obj, obj)
    end.to raise_error(ArgumentError)
  end

  it "should raise error when called with too few args" do
    expect do
      CoreTypeMethods.getType()
    end.to raise_error(ArgumentError)

    expect do
      CoreTypeMethods.getType()
    end.to raise_error(ArgumentError)
  end
end

describe "The return value of an overridden Java static method" do
  before(:each) do
    @return_value = StaticMethodSelection.produce
  end
  it "should not be nil" do
    expect(@return_value).not_to be_nil
  end
  it "should be of the correct type" do
    expect(@return_value).to be_an_instance_of(StaticMethodSelection)
  end
end

describe "An overloaded Java instance method" do
  it "should be called with the most exact overload" do
    obj = java.lang.Integer.new(1)
    ctm = CoreTypeMethods.new
    expect(ctm.getTypeInstance(1)).to eq("long")
    expect(ctm.getTypeInstance(1, obj)).to eq("long,object")
    expect(ctm.getTypeInstance(1, obj, obj)).to eq("long,object,object")
    expect(ctm.getTypeInstance(1, obj, obj, obj)).to eq("long,object,object,object")
    expect(ctm.getTypeInstance(1.0)).to eq("double")
    expect(ctm.getTypeInstance(1.0, obj)).to eq("double,object")
    expect(ctm.getTypeInstance(1.0, obj, obj)).to eq("double,object,object")
    expect(ctm.getTypeInstance(1.0, obj, obj, obj)).to eq("double,object,object,object")

    obj = "foo"
    ctm = CoreTypeMethods.new
    expect(ctm.getTypeInstance(1)).to eq("long")
    expect(ctm.getTypeInstance(1, obj)).to eq("long,string")
    expect(ctm.getTypeInstance(1, obj, obj)).to eq("long,string,string")
    expect(ctm.getTypeInstance(1, obj, obj, obj)).to eq("long,string,string,string")
    expect(ctm.getTypeInstance(1.0)).to eq("double")
    expect(ctm.getTypeInstance(1.0, obj)).to eq("double,string")
    expect(ctm.getTypeInstance(1.0, obj, obj)).to eq("double,string,string")
    expect(ctm.getTypeInstance(1.0, obj, obj, obj)).to eq("double,string,string,string")
  end

  it "should raise error when called with too many args" do
    expect do
      obj = java.lang.Integer.new(1)
      CoreTypeMethods.new.getTypeInstance(1, obj, obj, obj, obj)
    end.to raise_error(ArgumentError)

    expect do
      obj = "foo"
      CoreTypeMethods.new.getTypeInstance(1, obj, obj, obj, obj)
    end.to raise_error(ArgumentError)
  end

  it "should raise error when called with too few args" do
    expect do
      CoreTypeMethods.new.getTypeInstance()
    end.to raise_error(ArgumentError)

    expect do
      CoreTypeMethods.new.getTypeInstance()
    end.to raise_error(ArgumentError)
  end
end

describe "A class with varargs constructors" do
  it "should be called with the most exact overload" do
    obj = ClassWithVarargs.new()
    expect(obj.constructor).to eq("0: []")
    obj = ClassWithVarargs.new(1)
    expect(obj.constructor).to eq("0: [1]")
    obj = ClassWithVarargs.new(1,2)
    expect(obj.constructor).to eq("0: [1, 2]")
    obj = ClassWithVarargs.new(1,2,3)
    expect(obj.constructor).to eq("0: [1, 2, 3]")
    obj = ClassWithVarargs.new(1,2,3,4)
    expect(obj.constructor).to eq("0: [1, 2, 3, 4]")

    obj = ClassWithVarargs.new('foo', 1)
    expect(obj.constructor).to eq("1: [1]")
    obj = ClassWithVarargs.new('foo', 1, 2)
    expect(obj.constructor).to eq("1: [1, 2]")
    obj = ClassWithVarargs.new('foo', 1, 2, 3)
    expect(obj.constructor).to eq("1: [1, 2, 3]")
    obj = ClassWithVarargs.new('foo', 1, 2, 3, 4)
    expect(obj.constructor).to eq("1: [1, 2, 3, 4]")

    obj = ClassWithVarargs.new('foo', 'bar', 1)
    expect(obj.constructor).to eq("2: [1]")
    obj = ClassWithVarargs.new('foo', 'bar', 1, 2)
    expect(obj.constructor).to eq("2: [1, 2]")
    obj = ClassWithVarargs.new('foo', 'bar', 1, 2, 3)
    expect(obj.constructor).to eq("2: [1, 2, 3]")
    obj = ClassWithVarargs.new('foo', 'bar', 1, 2, 3, 4)
    expect(obj.constructor).to eq("2: [1, 2, 3, 4]")

    obj = ClassWithVarargs.new('foo', 'bar', 'baz', 1)
    expect(obj.constructor).to eq("3: [1]")
    obj = ClassWithVarargs.new('foo', 'bar', 'baz', 1, 2)
    expect(obj.constructor).to eq("3: [1, 2]")
    obj = ClassWithVarargs.new('foo', 'bar', 'baz', 1, 2, 3)
    expect(obj.constructor).to eq("3: [1, 2, 3]")
    obj = ClassWithVarargs.new('foo', 'bar', 'baz', 1, 2, 3, 4)
    expect(obj.constructor).to eq("3: [1, 2, 3, 4]")

    #skip("needs better type-driven ranking of overloads") do
    obj = ClassWithVarargs.new('foo')
    expect(obj.constructor).to eq("1: []")

    obj = ClassWithVarargs.new('foo', 'bar')
    expect(obj.constructor).to eq("2: []")

    obj = ClassWithVarargs.new('foo', 'bar', 'baz')
    expect(obj.constructor).to eq("3: []")
    #end
  end

  it "should be callable with an array" do
    expect(ClassWithVarargs.new([1,2,3].to_java).constructor).to eq("0: [1, 2, 3]")
    expect(ClassWithVarargs.new('foo', [1,2,3].to_java).constructor).to eq("1: [1, 2, 3]")
    expect(ClassWithVarargs.new('foo', 'bar', [1,2,3].to_java).constructor).to eq("2: [1, 2, 3]")
    expect(ClassWithVarargs.new('foo', 'bar', 'baz', [1,2,3].to_java).constructor).to eq("3: [1, 2, 3]")
  end
end

describe "A class with varargs instance methods" do
  it "should be called with the most exact overload" do
    obj = ClassWithVarargs.new(1)

    expect(obj.varargs()).to eq("0: []");
    expect(obj.varargs(1)).to eq("0: [1]");
    expect(obj.varargs(1,2)).to eq("0: [1, 2]");
    expect(obj.varargs(1,2,3)).to eq("0: [1, 2, 3]");
    expect(obj.varargs(1,2,3,4)).to eq("0: [1, 2, 3, 4]");

    expect(obj.varargs('foo', 1)).to eq("1: [1]");
    expect(obj.varargs('foo', 1, 2)).to eq("1: [1, 2]");
    expect(obj.varargs('foo', 1, 2, 3)).to eq("1: [1, 2, 3]");
    expect(obj.varargs('foo', 1, 2, 3, 4)).to eq("1: [1, 2, 3, 4]");

    expect(obj.varargs('foo', 'bar', 1)).to eq("2: [1]");
    expect(obj.varargs('foo', 'bar', 1, 2)).to eq("2: [1, 2]");
    expect(obj.varargs('foo', 'bar', 1, 2, 3)).to eq("2: [1, 2, 3]");
    expect(obj.varargs('foo', 'bar', 1, 2, 3, 4)).to eq("2: [1, 2, 3, 4]");

    expect(obj.varargs('foo', 'bar', 'baz', 1)).to eq("3: [1]");
    expect(obj.varargs('foo', 'bar', 'baz', 1, 2)).to eq("3: [1, 2]");
    expect(obj.varargs('foo', 'bar', 'baz', 1, 2, 3)).to eq("3: [1, 2, 3]");
    expect(obj.varargs('foo', 'bar', 'baz', 1, 2, 3, 4)).to eq("3: [1, 2, 3, 4]");

    #skip("needs better type-driven ranking of overloads") do
    expect(obj.varargs('foo')).to eq("1: []")
    expect(obj.varargs('foo', 'bar')).to eq("2: []")
    expect(obj.varargs('foo', 'bar', 'baz')).to eq("3: []")
    #end
  end

  it "should be callable with an array" do
    obj = ClassWithVarargs.new(1)
    expect(obj.varargs([1,2,3].to_java)).to eq("0: [1, 2, 3]")
    expect(obj.varargs('foo', [1,2,3].to_java)).to eq("1: [1, 2, 3]")
    expect(obj.varargs('foo', 'bar', [1,2,3].to_java)).to eq("2: [1, 2, 3]")
    expect(obj.varargs('foo', 'bar', 'baz', [1,2,3].to_java)).to eq("3: [1, 2, 3]")
  end
end

describe "A class with varargs static methods" do
  it "should be called with the most exact overload" do
    expect(ClassWithVarargs.varargs_static()).to eq("0: []");
    expect(ClassWithVarargs.varargs_static(1)).to eq("0: [1]");
    expect(ClassWithVarargs.varargs_static(1,2)).to eq("0: [1, 2]");
    expect(ClassWithVarargs.varargs_static(1,2,3)).to eq("0: [1, 2, 3]");
    expect(ClassWithVarargs.varargs_static(1,2,3,4)).to eq("0: [1, 2, 3, 4]");

    expect(ClassWithVarargs.varargs_static('foo', 1)).to eq("1: [1]");
    expect(ClassWithVarargs.varargs_static('foo', 1, 2)).to eq("1: [1, 2]");
    expect(ClassWithVarargs.varargs_static('foo', 1, 2, 3)).to eq("1: [1, 2, 3]");
    expect(ClassWithVarargs.varargs_static('foo', 1, 2, 3, 4)).to eq("1: [1, 2, 3, 4]");

    expect(ClassWithVarargs.varargs_static('foo', 'bar', 1)).to eq("2: [1]");
    expect(ClassWithVarargs.varargs_static('foo', 'bar', 1, 2)).to eq("2: [1, 2]");
    expect(ClassWithVarargs.varargs_static('foo', 'bar', 1, 2, 3)).to eq("2: [1, 2, 3]");
    expect(ClassWithVarargs.varargs_static('foo', 'bar', 1, 2, 3, 4)).to eq("2: [1, 2, 3, 4]");

    expect(ClassWithVarargs.varargs_static('foo', 'bar', 'baz', 1)).to eq("3: [1]");
    expect(ClassWithVarargs.varargs_static('foo', 'bar', 'baz', 1, 2)).to eq("3: [1, 2]");
    expect(ClassWithVarargs.varargs_static('foo', 'bar', 'baz', 1, 2, 3)).to eq("3: [1, 2, 3]");
    expect(ClassWithVarargs.varargs_static('foo', 'bar', 'baz', 1, 2, 3, 4)).to eq("3: [1, 2, 3, 4]");

    #skip("needs better type-driven ranking of overloads") do
    expect(ClassWithVarargs.varargs_static('foo')).to eq("1: []")
    expect(ClassWithVarargs.varargs_static('foo', 'bar')).to eq("2: []")
    expect(ClassWithVarargs.varargs_static('foo', 'bar', 'baz')).to eq("3: []")
    #end
    expect(ClassWithVarargs.varargs_static('foo'.to_java)).to eq("1: []")
    expect(ClassWithVarargs.varargs_static('foo'.to_java, 'bar')).to eq("2: []")
    expect(ClassWithVarargs.varargs_static('foo'.to_java, 'bar'.to_java)).to eq("2: []")
  end

  it "should be callable with an array" do
    expect(ClassWithVarargs.varargs_static([1,2,3].to_java)).to eq("0: [1, 2, 3]")
    expect(ClassWithVarargs.varargs_static('foo', [1,2,3].to_java)).to eq("1: [1, 2, 3]")
    expect(ClassWithVarargs.varargs_static('foo', 'bar', [1,2,3].to_java)).to eq("2: [1, 2, 3]")
    expect(ClassWithVarargs.varargs_static('foo', 'bar', 'baz', [1,2,3].to_java)).to eq("3: [1, 2, 3]")
  end
end

describe "A class with primitive varargs constructors" do
  it "should be called with the most exact overload" do
    obj = ClassWithPrimitiveVarargs.new()
    expect(obj.constructor).to eq("0: []")
    obj = ClassWithPrimitiveVarargs.new(1)
    expect(obj.constructor).to eq("0: [1]")
    obj = ClassWithPrimitiveVarargs.new(1,2)
    expect(obj.constructor).to eq("0: [1, 2]")
    obj = ClassWithPrimitiveVarargs.new(1,2,3)
    expect(obj.constructor).to eq("0: [1, 2, 3]")
    obj = ClassWithPrimitiveVarargs.new(1,2,3,4)
    expect(obj.constructor).to eq("0: [1, 2, 3, 4]")

    obj = ClassWithPrimitiveVarargs.new('foo', 1)
    expect(obj.constructor).to eq("1: [1]")
    obj = ClassWithPrimitiveVarargs.new('foo', 1, 2)
    expect(obj.constructor).to eq("1: [1, 2]")
    obj = ClassWithPrimitiveVarargs.new('foo', 1, 2, 3)
    expect(obj.constructor).to eq("1: [1, 2, 3]")
    obj = ClassWithPrimitiveVarargs.new('foo', 1, 2, 3, 4)
    expect(obj.constructor).to eq("1: [1, 2, 3, 4]")

    obj = ClassWithPrimitiveVarargs.new('foo', 'bar', 1)
    expect(obj.constructor).to eq("2: [1]")
    obj = ClassWithPrimitiveVarargs.new('foo', 'bar', 1, 2)
    expect(obj.constructor).to eq("2: [1, 2]")
    obj = ClassWithPrimitiveVarargs.new('foo', 'bar', 1, 2, 3)
    expect(obj.constructor).to eq("2: [1, 2, 3]")
    obj = ClassWithPrimitiveVarargs.new('foo', 'bar', 1, 2, 3, 4)
    expect(obj.constructor).to eq("2: [1, 2, 3, 4]")

    obj = ClassWithPrimitiveVarargs.new('foo', 'bar', 'baz', 1)
    expect(obj.constructor).to eq("3: [1]")
    obj = ClassWithPrimitiveVarargs.new('foo', 'bar', 'baz', 1, 2)
    expect(obj.constructor).to eq("3: [1, 2]")
    obj = ClassWithPrimitiveVarargs.new('foo', 'bar', 'baz', 1, 2, 3)
    expect(obj.constructor).to eq("3: [1, 2, 3]")
    obj = ClassWithPrimitiveVarargs.new('foo', 'bar', 'baz', 1, 2, 3, 4)
    expect(obj.constructor).to eq("3: [1, 2, 3, 4]")

    #skip("needs better type-driven ranking of overloads") do
    obj = ClassWithPrimitiveVarargs.new('foo')
    expect(obj.constructor).to eq("1: []")

    obj = ClassWithPrimitiveVarargs.new('foo', 'bar')
    expect(obj.constructor).to eq("2: []")

    obj = ClassWithPrimitiveVarargs.new('foo', 'bar', 'baz')
    expect(obj.constructor).to eq("3: []")
    #end
  end

  it "should be callable with an array" do
    expect(ClassWithPrimitiveVarargs.new([1,2,3].to_java(:int)).constructor).to eq("0: [1, 2, 3]")
    expect(ClassWithPrimitiveVarargs.new('foo', [1,2,3].to_java(:int)).constructor).to eq("1: [1, 2, 3]")
    expect(ClassWithPrimitiveVarargs.new('foo', 'bar', [1,2,3].to_java(:int)).constructor).to eq("2: [1, 2, 3]")
    expect(ClassWithPrimitiveVarargs.new('foo', 'bar', 'baz', [1,2,3].to_java(:int)).constructor).to eq("3: [1, 2, 3]")
  end
end

describe "A class with primitive varargs instance methods" do
  it "should be called with the most exact overload" do
    obj = ClassWithPrimitiveVarargs.new(1)

    expect(obj.primitive_varargs()).to eq("0: []");
    expect(obj.primitive_varargs(1)).to eq("0: [1]");
    expect(obj.primitive_varargs(1,2)).to eq("0: [1, 2]");
    expect(obj.primitive_varargs(1,2,3)).to eq("0: [1, 2, 3]");
    expect(obj.primitive_varargs(1,2,3,4)).to eq("0: [1, 2, 3, 4]");

    expect(obj.primitive_varargs('foo', 1)).to eq("1: [1]");
    expect(obj.primitive_varargs('foo', 1, 2)).to eq("1: [1, 2]");
    expect(obj.primitive_varargs('foo', 1, 2, 3)).to eq("1: [1, 2, 3]");
    expect(obj.primitive_varargs('foo', 1, 2, 3, 4)).to eq("1: [1, 2, 3, 4]");

    expect(obj.primitive_varargs('foo', 'bar', 1)).to eq("2: [1]");
    expect(obj.primitive_varargs('foo', 'bar', 1, 2)).to eq("2: [1, 2]");
    expect(obj.primitive_varargs('foo', 'bar', 1, 2, 3)).to eq("2: [1, 2, 3]");
    expect(obj.primitive_varargs('foo', 'bar', 1, 2, 3, 4)).to eq("2: [1, 2, 3, 4]");

    expect(obj.primitive_varargs('foo', 'bar', 'baz', 1)).to eq("3: [1]");
    expect(obj.primitive_varargs('foo', 'bar', 'baz', 1, 2)).to eq("3: [1, 2]");
    expect(obj.primitive_varargs('foo', 'bar', 'baz', 1, 2, 3)).to eq("3: [1, 2, 3]");
    expect(obj.primitive_varargs('foo', 'bar', 'baz', 1, 2, 3, 4)).to eq("3: [1, 2, 3, 4]");

    #skip("needs better type-driven ranking of overloads") do
    expect(obj.primitive_varargs('foo')).to eq("1: []")
    expect(obj.primitive_varargs('foo', 'bar')).to eq("2: []")
    expect(obj.primitive_varargs('foo', 'bar', 'baz')).to eq("3: []")
    #end
  end

  it "should be callable with an array" do
    obj = ClassWithPrimitiveVarargs.new(1)
    expect(obj.primitive_varargs([1,2,3].to_java(:int))).to eq("0: [1, 2, 3]")
    expect(obj.primitive_varargs('foo', [1,2,3].to_java(:int))).to eq("1: [1, 2, 3]")
    expect(obj.primitive_varargs('foo', 'bar', [1,2,3].to_java(:int))).to eq("2: [1, 2, 3]")
    expect(obj.primitive_varargs('foo', 'bar', 'baz', [1,2,3].to_java(:int))).to eq("3: [1, 2, 3]")
  end
end

describe "A class with primitive varargs static methods" do
  it "should be called with the most exact overload" do
    expect(ClassWithPrimitiveVarargs.primitive_varargs_static()).to eq("0: []");
    expect(ClassWithPrimitiveVarargs.primitive_varargs_static(1)).to eq("0: [1]");
    expect(ClassWithPrimitiveVarargs.primitive_varargs_static(1,2)).to eq("0: [1, 2]");
    expect(ClassWithPrimitiveVarargs.primitive_varargs_static(1,2,3)).to eq("0: [1, 2, 3]");
    expect(ClassWithPrimitiveVarargs.primitive_varargs_static(1,2,3,4)).to eq("0: [1, 2, 3, 4]");

    expect(ClassWithPrimitiveVarargs.primitive_varargs_static('foo', 1)).to eq("1: [1]");
    expect(ClassWithPrimitiveVarargs.primitive_varargs_static('foo', 1, 2)).to eq("1: [1, 2]");
    expect(ClassWithPrimitiveVarargs.primitive_varargs_static('foo', 1, 2, 3)).to eq("1: [1, 2, 3]");
    expect(ClassWithPrimitiveVarargs.primitive_varargs_static('foo', 1, 2, 3, 4)).to eq("1: [1, 2, 3, 4]");

    expect(ClassWithPrimitiveVarargs.primitive_varargs_static('foo', 'bar', 1)).to eq("2: [1]");
    expect(ClassWithPrimitiveVarargs.primitive_varargs_static('foo', 'bar', 1, 2)).to eq("2: [1, 2]");
    expect(ClassWithPrimitiveVarargs.primitive_varargs_static('foo', 'bar', 1, 2, 3)).to eq("2: [1, 2, 3]");
    expect(ClassWithPrimitiveVarargs.primitive_varargs_static('foo', 'bar', 1, 2, 3, 4)).to eq("2: [1, 2, 3, 4]");

    expect(ClassWithPrimitiveVarargs.primitive_varargs_static('foo', 'bar', 'baz', 1)).to eq("3: [1]");
    expect(ClassWithPrimitiveVarargs.primitive_varargs_static('foo', 'bar', 'baz', 1, 2)).to eq("3: [1, 2]");
    expect(ClassWithPrimitiveVarargs.primitive_varargs_static('foo', 'bar', 'baz', 1, 2, 3)).to eq("3: [1, 2, 3]");
    expect(ClassWithPrimitiveVarargs.primitive_varargs_static('foo', 'bar', 'baz', 1, 2, 3, 4)).to eq("3: [1, 2, 3, 4]");

    #skip("needs better type-driven ranking of overloads") do
    expect(ClassWithPrimitiveVarargs.primitive_varargs_static('foo')).to eq("1: []")
    expect(ClassWithPrimitiveVarargs.primitive_varargs_static('foo', 'bar')).to eq("2: []")
    expect(ClassWithPrimitiveVarargs.primitive_varargs_static('foo', 'bar', 'baz')).to eq("3: []")
    #end
    expect(ClassWithPrimitiveVarargs.primitive_varargs_static('foo'.to_java)).to eq("1: []")
    expect(ClassWithPrimitiveVarargs.primitive_varargs_static('foo'.to_java, 'bar')).to eq("2: []")
    expect(ClassWithPrimitiveVarargs.primitive_varargs_static('foo'.to_java, 'bar'.to_java)).to eq("2: []")
  end

  it "should be callable with an array" do
    expect(ClassWithPrimitiveVarargs.primitive_varargs_static([1,2,3].to_java(:int))).to eq("0: [1, 2, 3]")
    expect(ClassWithPrimitiveVarargs.primitive_varargs_static('foo', [1,2,3].to_java(:int))).to eq("1: [1, 2, 3]")
    expect(ClassWithPrimitiveVarargs.primitive_varargs_static('foo', 'bar', [1,2,3].to_java(:int))).to eq("2: [1, 2, 3]")
    expect(ClassWithPrimitiveVarargs.primitive_varargs_static('foo', 'bar', 'baz', [1,2,3].to_java(:int))).to eq("3: [1, 2, 3]")
  end
end

# JRUBY-5418
describe "A Java method dispatch downstream from a Kernel#catch block" do
  it "should propagate rather than wrap the 'throw' exception" do
    expect do
      catch(:foo) do
        UsesSingleMethodInterface.new { throw :foo }
      end
    end.not_to raise_error
    expect do
      catch(:foo) do
        UsesSingleMethodInterface.new(nil) { throw :foo }
      end
    end.not_to raise_error
    expect do
      catch(:foo) do
        UsesSingleMethodInterface.new(nil, nil) { throw :foo }
      end
    end.not_to raise_error
    expect do
      catch(:foo) do
        UsesSingleMethodInterface.new(nil, nil, nil) { throw :foo }
      end
    end.not_to raise_error
    # 3 normal args is our cutoff for specific-arity optz, so test four
    expect do
      catch(:foo) do
        UsesSingleMethodInterface.new(nil, nil, nil, nil) { throw :foo }
      end
    end.not_to raise_error
  end
end

if TestHelper::JAVA_9
  describe "An overridden Java method" do
    describe "with a non-public base implementation" do
      it "is called using a public override" do
        expect do
          java.util.Properties.new.clone
        end.not_to raise_error
      end
    end
  end
end