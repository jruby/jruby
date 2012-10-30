require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../shared/accessor', __FILE__)

describe "Struct#select" do
  it "raises an ArgumentError if given any non-block arguments" do
    lambda { StructClasses::Car.new.select(1) { } }.should raise_error(ArgumentError)
  end

  it "returns a new array of elements for which block is true" do
    struct = StructClasses::Car.new("Toyota", "Tercel", "2000")
    struct.select { |i| i == "2000" }.should == [ "2000" ]
  end

  it "returns an instance of Array" do
    struct = StructClasses::Car.new("Ford", "Escort", "1995")
    struct.select { true }.should be_kind_of(Array)
  end

  it_behaves_like :struct_accessor, :length
end
