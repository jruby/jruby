require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Struct#initialize" do

  it "is private" do
    StructClasses::Car.should have_private_instance_method(:initialize)
  end

  it "does nothing when passed a set of fields equal to self" do
    car = same_car = StructClasses::Car.new("Honda", "Accord", "1998")
    car.instance_eval { initialize("Honda", "Accord", "1998") }
    car.should == same_car
  end

  it "explicitly sets instance variables to nil when args not provided to initialize" do
    car = StructClasses::Honda.new
    car.make.should == nil # still nil despite override in Honda#initialize b/c of super order
  end

  it "can be overriden" do
    StructClasses::SubclassX.new(:y).new.key.should == :value
  end
end
