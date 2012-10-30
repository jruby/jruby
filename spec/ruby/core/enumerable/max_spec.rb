require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Enumerable#max" do
  before :each do
    @a = EnumerableSpecs::EachDefiner.new( 2, 4, 6, 8, 10 )

    @e_strs = EnumerableSpecs::EachDefiner.new("333", "22", "666666", "1", "55555", "1010101010")
    @e_ints = EnumerableSpecs::EachDefiner.new( 333,   22,   666666,   55555, 1010101010)
  end

  it "max should return the maximum element" do
    EnumerableSpecs::Numerous.new.max.should == 6
  end

  it "return the maximum element (basics cases)" do
    EnumerableSpecs::EachDefiner.new(55).max.should == 55

    EnumerableSpecs::EachDefiner.new(11,99).max.should == 99
    EnumerableSpecs::EachDefiner.new(99,11).max.should == 99
    EnumerableSpecs::EachDefiner.new(2, 33, 4, 11).max.should == 33

    EnumerableSpecs::EachDefiner.new(1,2,3,4,5).max.should == 5
    EnumerableSpecs::EachDefiner.new(5,4,3,2,1).max.should == 5
    EnumerableSpecs::EachDefiner.new(1,4,3,5,2).max.should == 5
    EnumerableSpecs::EachDefiner.new(5,5,5,5,5).max.should == 5

    EnumerableSpecs::EachDefiner.new("aa","tt").max.should == "tt"
    EnumerableSpecs::EachDefiner.new("tt","aa").max.should == "tt"
    EnumerableSpecs::EachDefiner.new("2","33","4","11").max.should == "4"

    @e_strs.max.should == "666666"
    @e_ints.max.should == 1010101010
  end

  it "returns nil for an empty Enumerable " do
    EnumerableSpecs::EachDefiner.new.max.should == nil
  end

  ruby_version_is ""..."1.9" do
    it "raises a NoMethodError for elements without #<=>" do
      lambda do
        EnumerableSpecs::EachDefiner.new(Object.new, Object.new).max
      end.should raise_error(NoMethodError)
    end
  end

  ruby_version_is "1.9" do
    it "raises a NoMethodError for elements without #<=>" do
      lambda do
        EnumerableSpecs::EachDefiner.new(BasicObject.new, BasicObject.new).max
      end.should raise_error(NoMethodError)
    end
  end

  it "raises an ArgumentError for incomparable elements" do
    lambda do
      EnumerableSpecs::EachDefiner.new(11,"22").max
    end.should raise_error(ArgumentError)
    lambda do
      EnumerableSpecs::EachDefiner.new(11,12,22,33).max{|a, b| nil}
    end.should raise_error(ArgumentError)
  end

  it "return the maximum element (with block" do
    # with a block
    EnumerableSpecs::EachDefiner.new("2","33","4","11").max {|a,b| a <=> b }.should == "4"
    EnumerableSpecs::EachDefiner.new( 2 , 33 , 4 , 11 ).max {|a,b| a <=> b }.should == 33

    EnumerableSpecs::EachDefiner.new("2","33","4","11").max {|a,b| b <=> a }.should == "11"
    EnumerableSpecs::EachDefiner.new( 2 , 33 , 4 , 11 ).max {|a,b| b <=> a }.should == 2

    @e_strs.max {|a,b| a.length <=> b.length }.should == "1010101010"

    @e_strs.max {|a,b| a <=> b }.should == "666666"
    @e_strs.max {|a,b| a.to_i <=> b.to_i }.should == "1010101010"

    @e_ints.max {|a,b| a <=> b }.should == 1010101010
    @e_ints.max {|a,b| a.to_s <=> b.to_s }.should == 666666
  end

  it "returns the minimum for enumerables that contain nils" do
    arr = EnumerableSpecs::Numerous.new(nil, nil, true)
    arr.max { |a, b|
      x = a.nil? ? 1 : a ? 0 : -1
      y = b.nil? ? 1 : b ? 0 : -1
      x <=> y
    }.should == nil
  end

  it "gathers whole arrays as elements when each yields multiple" do
    multi = EnumerableSpecs::YieldsMulti.new
    multi.max.should == [6, 7, 8, 9]
  end

end
