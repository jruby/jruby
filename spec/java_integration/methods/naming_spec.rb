require File.dirname(__FILE__) + "/../spec_helper"

java_import "java_integration.fixtures.MethodNames"
java_import "java_integration.fixtures.ArrayReceiver"
java_import "java_integration.fixtures.BeanLikeInterface"

describe "Java static method names" do
  let(:class_methods) { MethodNames.methods }

  it "should present as both camel-case and ruby-case" do
    expect(class_methods).to have_strings_or_symbols("lowercase1",
                                      "camelCase1",
                                      "camel_case1",
                                      "camelWithUPPER1",
                                      "camel_with_upper1",
                                      "camelWITHUpper1",
                                      "CAMELWithUpper1")

    expect(class_methods).not_to have_strings_or_symbols("camel_withupper1",
                                          "camelwith_upper1")
  end

  it "keeps all caps in a name together as a single downcased word" do
    expect(class_methods).to have_strings_or_symbols("value_objs",
                                      "value_objs=",
                                      "value_objs_here",
                                      "value_objs_here=")
  end

  it "should present javabean properties as attribute readers and writers" do
    expect(class_methods).to have_strings_or_symbols("getValue1",
                                      "get_value1",
                                      "value1",
                                      "setValue1",
                                      "set_value1",
                                      "value1=",
                                      "getValues1",
                                      "get_values1",
                                      "setValues1",
                                      "set_values1")

    expect(class_methods).not_to have_strings_or_symbols("values1",
                                          "values1=")
  end

  it "should present boolean javabean property accessors as '?' method" do
    expect(class_methods).to have_strings_or_symbols("isFirst1",
                                      "first1",
                                      "first1?",
                                      "isSecond1",
                                      "second1",
                                      "second1?",
                                      "hasThird1",
                                      "has_third1",
                                      "has_third1?",
                                      "hasFourth1",
                                      "has_fourth1",
                                      "has_fourth1?")

    expect(class_methods).not_to have_strings_or_symbols("fourth1?")
  end

  it "should not overwrite critical core Ruby methods" do
    skip("need a better way to separate class and instance methods in the java code")
  end
end

describe "Java instance method names" do
  let(:members) { MethodNames.instance_methods }

  it "should present as both camel-case and ruby-case" do
    expect(members).to have_strings_or_symbols("lowercase2",
                                "camelCase2",
                                "camel_case2",
                                "camelWithUPPER2",
                                "camel_with_upper2",
                                "camelWITHUpper2",
                                "CAMELWithUpper2")

    expect(members).not_to have_strings_or_symbols("camel_withupper2",
                                    "camelwith_upper2")
  end

  it "should present javabean properties as attribute readers and writers" do
    expect(members).to have_strings_or_symbols("getValue2",
                                "get_value2",
                                "value2",
                                "setValue2",
                                "set_value2",
                                "value2=",
                                "getValues2",
                                "get_values2",
                                "setValues2",
                                "set_values2",
                                "get_my_value",
                                "my_value",
                                "set_my_value",
                                "my_value=",
                                "getX",
                                "get_x",
                                "x",
                                "bigO",
                                "big_o")

    expect(members).not_to have_strings_or_symbols("values2",
                                    "values2=",
                                    "get_myvalue",
                                    "set_myvalue",
                                    "myvalue=")
  end

  it "with colliding is/get property - ruby name does get" do
    expect( MethodNames.new.foo? ).to be false
    expect( MethodNames.new.foo ).to eql 42
  end

  it "keeps all caps in a name together as a single downcased word" do
    expect(members).to have_strings_or_symbols("value_obj",
                                "value_obj=",
                                "value_obj_here",
                                "value_obj_here=")
  end

  it "should treat consecutive caps as part of one property name" do
    expect(members).to have_strings_or_symbols("jconsecutive_caps",
                                "jconsecutive_caps=")
  end

  it "should present boolean javabean property accessors as '?' method" do
    expect(members).to have_strings_or_symbols("isFirst2",
                                "first2",
                                "first2?",
                                "isSecond2",
                                "second2",
                                "second2?",
                                "hasThird2",
                                "has_third2",
                                "has_third2?",
                                "hasFourth2",
                                "has_fourth2",
                                "has_fourth2?")

    expect(members).not_to have_strings_or_symbols("fourth2?")
  end

  it "should not overwrite critical core Ruby methods" do
    obj = MethodNames.new

    expect(obj.send(:initialize)).not_to eq("foo")
    expect(obj.object_id).not_to eq("foo")
    expect(obj.__id__).not_to eq("foo")
    expect {obj.__send__}.to raise_error(ArgumentError)
  end
end

describe "Needed implementation methods for concrete classes" do
  it "should have __id__ method" do
    expect(ArrayReceiver.new.methods).to have_strings_or_symbols("__id__")
  end
  it "should have __send__ method" do
    expect(ArrayReceiver.new.methods).to have_strings_or_symbols("__send__")
  end
  it "should have == method" do
    expect(ArrayReceiver.new.methods).to have_strings_or_symbols("==")
  end
  it "should have inspect method" do
    expect(ArrayReceiver.new.methods).to have_strings_or_symbols("inspect")
  end
  it "should have respond_to? method" do
    expect(ArrayReceiver.new.methods).to have_strings_or_symbols("respond_to?")
  end
  it "should have class method" do
    expect(ArrayReceiver.new.methods).to have_strings_or_symbols("class")
  end
  it "should have methods method" do
    expect(ArrayReceiver.new.methods).to have_strings_or_symbols("methods")
  end
  it "should have send method" do
    expect(ArrayReceiver.new.methods).to have_strings_or_symbols("send")
  end
  it "should have equal? method" do
    expect(ArrayReceiver.new.methods).to have_strings_or_symbols("equal?")
  end
  it "should have eql? method" do
    expect(ArrayReceiver.new.methods).to have_strings_or_symbols("eql?")
  end
  it "should have to_s method" do
    expect(ArrayReceiver.new.methods).to have_strings_or_symbols("to_s")
  end
end

describe "Needed implementation methods for interfaces" do
  it "should have __id__ method" do
    expect(BeanLikeInterface.impl {}.methods).to have_strings_or_symbols("__id__")
  end
  it "should have __send__ method" do
    expect(BeanLikeInterface.impl {}.methods).to have_strings_or_symbols("__send__")
  end
  it "should have == method" do
    expect(BeanLikeInterface.impl {}.methods).to have_strings_or_symbols("==")
  end
  it "should have inspect method" do
    expect(BeanLikeInterface.impl {}.methods).to have_strings_or_symbols("inspect")
  end
  it "should have respond_to? method" do
    expect(BeanLikeInterface.impl {}.methods).to have_strings_or_symbols("respond_to?")
  end
  it "should have class method" do
    expect(BeanLikeInterface.impl {}.methods).to have_strings_or_symbols("class")
  end
  it "should have methods method" do
    expect(BeanLikeInterface.impl {}.methods).to have_strings_or_symbols("methods")
  end
  it "should have send method" do
    expect(BeanLikeInterface.impl {}.methods).to have_strings_or_symbols("send")
  end
  it "should have equal? method" do
    expect(BeanLikeInterface.impl {}.methods).to have_strings_or_symbols("equal?")
  end
  it "should have eql? method" do
    expect(BeanLikeInterface.impl {}.methods).to have_strings_or_symbols("eql?")
  end
  it "should have to_s method" do
    expect(BeanLikeInterface.impl {}.methods).to have_strings_or_symbols("to_s")
  end

  it "should be able to access Java methods of core Ruby Methods via __method" do
    expect(MethodNames.new.initialize__method).to eq("foo")
    expect(MethodNames.__send____method).to eq("foo")
  end
end

