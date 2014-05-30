require File.expand_path('../../../spec_helper', __FILE__)
require "ostruct"

describe "OpenStruct#initialize_copy" do
  before :each do
    @os = OpenStruct.new("age" => 20, "name" => "John")
    @dup = @os.dup
  end

  it "is private" do
    OpenStruct.should have_private_instance_method(:initialize_copy)
  end

  it "creates an independent method/value table for self" do
    @dup.age = 30
    @dup.age.should eql(30)
    @os.age.should eql(20)
  end

  ruby_bug "bug 6028", "1.9.3" do
    it "generates the same methods" do
      @dup.methods.should == @os.methods
      @dup.respond_to?(:age).should == @os.respond_to?(:age)
    end
  end
end
