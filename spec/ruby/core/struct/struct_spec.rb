require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Struct anonymous class instance methods" do
  it "Enumerable methods should work" do
    car = StructClasses::Car.new('Ford', 'Ranger', '2001')
    car.detect { |value| value.include? 'F' }.should == 'Ford'
    car.reject { |value| value.include? 'F' }.should == ['Ranger', '2001']
  end

  it "reader method should be a synonym for []" do
    klass = Struct.new(:clock, :radio)
    alarm = klass.new(true)
    alarm.clock.should == alarm[:clock]
    alarm.radio.should == alarm['radio']
  end

  it "reader method should not interfere with undefined methods" do
    car = StructClasses::Car.new('Ford', 'Ranger')
    lambda { car.something_weird }.should raise_error(NoMethodError)
  end

  it "writer method be a synonym for []=" do
    car = StructClasses::Car.new('Ford', 'Ranger')
    car.model.should == 'Ranger'
    car.model = 'F150'
    car.model.should == 'F150'
    car[:model].should == 'F150'
    car['model'].should == 'F150'
    car[1].should == 'F150'
  end
end

describe "Struct subclasses" do
  it "can be subclassed" do
    compact = Class.new StructClasses::Car
    compact.new.class.should == compact
  end
end

