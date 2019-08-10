require File.dirname(__FILE__) + "/../spec_helper"

java_import "java_integration.fixtures.SuperWithInterface"
java_import "java_integration.fixtures.PublicClassExtendsNonpublicClass"

describe "A private subclass of a public superclass with interfaces" do
  it "defines all methods from those interfaces" do
    sub_without = SuperWithInterface.sub_class_instance

    # sub_without_cls = sub_without.class
    # 
    # sub_without_cls.instance_methods.should include("size")
    # sub_without_cls.instance_methods.should include("empty?")
    # sub_without_cls.instance_methods.should include("contains?")
    # sub_without_cls.instance_methods.should include("iterator")
    # sub_without_cls.instance_methods.should include("to_array")
    # sub_without_cls.instance_methods.should include("add")
    # sub_without_cls.instance_methods.should include("remove")
    # sub_without_cls.instance_methods.should include("contains_all?")
    # sub_without_cls.instance_methods.should include("add_all")
    # sub_without_cls.instance_methods.should include("remove_all")
    # sub_without_cls.instance_methods.should include("retain_all")
    # sub_without_cls.instance_methods.should include("clear")
    
    # test that add can handle values other than string, since a
    # once-upon-a-time change caused child classes to only bind
    # unique methods, which in this case would be private boolean add(String)
    expect(sub_without.add(1)).to eq(true)
  end
end

describe "A private subclass with interfaces" do
  it "defines all methods from those interfaces" do
    sub_without = SuperWithInterface.sub_class_instance

    sub_without_cls = sub_without.class

    expect(sub_without_cls.instance_methods).to have_strings_or_symbols "run"
  end
end

describe "A public subclass with interfaces extending a superclass that duplicates some of those methods" do
  it "should still bind all methods from the child" do
    sub_with = SuperWithInterface::SubWithInterface.new
    
    # test that add can handle values other than string, since a
    # once-upon-a-time change caused child classes to only bind
    # unique methods, which in this case would be private boolean add(String)
    expect(sub_with.add(1)).to eq(true)
  end
end

describe "A non-public superclass of a public class" do
  it "should bind methods if the package is open" do
    pubclass = PublicClassExtendsNonpublicClass.new

    expect(pubclass.publicMethod).to be true
  end
end