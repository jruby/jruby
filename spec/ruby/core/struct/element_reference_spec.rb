require_relative '../../spec_helper'
require_relative 'fixtures/classes'

describe "Struct[]" do
  it "is a synonym for new" do
    StructClasses::Ruby['2.0', 'i686'].should.is_a?(StructClasses::Ruby)
  end
end

describe "Struct#[]" do
  it "returns the attribute referenced" do
    car = StructClasses::Car.new('Ford', 'Ranger', 1983)
    car['make'].should == 'Ford'
    car['model'].should == 'Ranger'
    car['year'].should == 1983
    car[:make].should == 'Ford'
    car[:model].should == 'Ranger'
    car[:year].should == 1983
    car[0].should == 'Ford'
    car[1].should == 'Ranger'
    car[2].should == 1983
    car[-3].should == 'Ford'
    car[-2].should == 'Ranger'
    car[-1].should == 1983
  end

  it "fails when it does not know about the requested attribute" do
    car = StructClasses::Car.new('Ford', 'Ranger')
    -> { car[3]        }.should.raise(IndexError)
    -> { car[-4]       }.should.raise(IndexError)
    -> { car[:body]    }.should.raise(NameError)
    -> { car['wheels'] }.should.raise(NameError)
  end

  ruby_version_is ""..."4.1" do
    it "raises a RangeError for an Integer index outside the int range" do
      car = StructClasses::Car.new('Ford', 'Ranger')
      -> { car[2**40]  }.should.raise(RangeError)
      -> { car[-2**40] }.should.raise(RangeError)
    end
  end

  ruby_version_is "4.1" do
    platform_is c_long_size: 64 do
      it "raises an IndexError for an Integer index outside the int range" do
        car = StructClasses::Car.new('Ford', 'Ranger')
        -> { car[2**40]  }.should.raise(IndexError, "offset 1099511627776 too large for struct(size:3)")
        -> { car[-2**40] }.should.raise(IndexError, "offset -1099511627776 too small for struct(size:3)")
      end
    end
  end

  it "fails if passed too many arguments" do
    car = StructClasses::Car.new('Ford', 'Ranger')
    -> { car[:make, :model] }.should.raise(ArgumentError)
  end

  it "fails if not passed a string, symbol, or integer" do
    car = StructClasses::Car.new('Ford', 'Ranger')
    -> { car[Object.new] }.should.raise(TypeError)
  end

  it "returns attribute names that contain hyphens" do
    klass = Struct.new(:'current-state')
    tuple = klass.new(0)
    tuple['current-state'].should == 0
    tuple[:'current-state'].should == 0
    tuple[0].should == 0
  end
end
