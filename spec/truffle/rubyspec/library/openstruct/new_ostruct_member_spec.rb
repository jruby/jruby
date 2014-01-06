require File.expand_path('../../../spec_helper', __FILE__)
require "ostruct"

describe "OpenStruct#new_ostruct_member when passed [method_name]" do
  before(:each) do
    @os = OpenStruct.new
    @os.instance_variable_set(:@table, :age => 20)
  end

  it "creates an attribute reader method for the passed method_name" do
    @os.respond_to?(:age).should be_false
    @os.send :new_ostruct_member, :age
    @os.method(:age).call.should == 20
  end

  it "creates an attribute writer method for the passed method_name" do
    @os.respond_to?(:age=).should be_false
    @os.send :new_ostruct_member, :age
    @os.method(:age=).call(42).should == 42
    @os.age.should == 42
  end

  it "does not allow overwriting existing methods" do
    def @os.age
      10
    end

    @os.send :new_ostruct_member, :age
    @os.age.should == 10
    @os.respond_to?(:age=).should be_false
  end
end
