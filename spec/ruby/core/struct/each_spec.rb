require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../shared/accessor', __FILE__)

describe "Struct#each" do
  it "passes each value to the given block" do
    car = StructClasses::Car.new('Ford', 'Ranger')
    i = -1
    car.each do |value|
      value.should == car[i += 1]
    end
  end

  it "returns self if passed a block" do
    car = StructClasses::Car.new('Ford', 'Ranger')
    car.each {}.should == car
  end

  ruby_version_is "" ... "1.8.7" do
    it "fails if not passed a block" do
      car = StructClasses::Car.new('Ford', 'Ranger')
      lambda { car.each }.should raise_error(LocalJumpError)
    end
  end

  ruby_version_is "1.8.7" do
    it "returns an Enumerator if not passed a block" do
      car = StructClasses::Car.new('Ford', 'Ranger')
      car.each.should be_an_instance_of(enumerator_class)
    end
  end

  it_behaves_like :struct_accessor, :length
end
