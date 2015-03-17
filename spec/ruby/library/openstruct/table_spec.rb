require File.expand_path('../../../spec_helper', __FILE__)
require 'ostruct'

describe "OpenStruct#table" do
  before(:each) do
    @os = OpenStruct.new("age" => 20, "name" => "John")
  end

  it "is protected" do
    OpenStruct.should have_protected_instance_method(:table)
  end

  it "returns self's method/value table" do
    @os.send(:table).should == { :age => 20, :name => "John" }
    @os.send(:table)[:age] = 30
    @os.age.should == 30
  end
end
