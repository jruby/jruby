require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../shared/accessor', __FILE__)

describe "Struct#each_pair" do
  it "passes each key value pair to the given block" do
    car = StructClasses::Car.new('Ford', 'Ranger', 2001)
    car.each_pair do |key, value|
      value.should == car[key]
    end
  end

  it "returns self if passed a block" do
    car = StructClasses::Car.new('Ford', 'Ranger')
    car.each_pair {}.should == car
  end

  ruby_version_is "" ... "1.8.7" do
    it "fails if not passed a block" do
      car = StructClasses::Car.new('Ford', 'Ranger')
      lambda { car.each_pair }.should raise_error(LocalJumpError)
    end
  end

  ruby_version_is "1.8.7" do
    it "returns an Enumerator if not passed a block" do
      car = StructClasses::Car.new('Ford', 'Ranger')
      car.each_pair.should be_an_instance_of(enumerator_class)
    end
  end

  it_behaves_like :struct_accessor, :length
end
