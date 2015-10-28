require File.expand_path('../../../spec_helper', __FILE__)
require "ostruct"

describe "OpenStruct#new_ostruct_member" do
  before :each do
    @os = OpenStruct.new
  end

  it "is protected" do
    OpenStruct.should have_protected_instance_method(:new_ostruct_member)
  end

  context "when passed [method_name]" do
    it "creates an attribute reader method for the passed method_name" do
      @os.respond_to?(:age).should be_false
      @os.send :new_ostruct_member, :age
      @os.respond_to?(:age).should be_true
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
end
