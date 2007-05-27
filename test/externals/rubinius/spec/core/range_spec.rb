require File.dirname(__FILE__) + '/../spec_helper'

# ==, ===, begin, each, end, eql?, exclude_end?, first, hash,
# include?, inspect, last, member?, step, to_s

context "Range" do
  specify "== should return true if other has same begin, end, and exclude_end?" do
    ((0..2) == (0..2)).should == true
    ((5..10) == Range.new(5,10)).should == true
    ((1482..1911) == (1482...1911)).should == false
  end

  specify "=== should return true if other is an element" do
    ((0..5) === 2).should == true
    ((-5..5) === 0).should == true
    ((-1...1) === 10.5).should == false
    ((-10..-2) === -2.5).should == true
  end
  
  specify "begin should return the first element" do
    (-1..1).begin.should == -1
    (0..1).begin.should == 0
    (0xffff...0xfffff).begin.should == 65535
  end
  
  specify "each should pass each element to the block" do
    a = []
    (-5..5).each { |i| a << i }
    a.should == [-5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5]
  end
  
  specify "end should return the last element" do
    (-1..1).end.should == 1
    (0..1).end.should == 1
    (0xffff...0xfffff).end.should == 1048575
  end
  
  specify "eql? should be a synonym for ==" do
    (0..2).eql?((0..2)).should == true
    (5..10).eql?(Range.new(5,10)).should == true
    (1482..1911).eql?((1482...1911)).should == false
  end
  
  specify "exclude_end? should return true if the range exludes the end value" do
    (-2..2).exclude_end?.should == false
    (0...5).exclude_end?.should == true
  end
  
  specify "first should be a synonym for begin" do
    (-1..1).first.should == -1
    (0..1).first.should == 0
    (0xffff...0xfffff).first.should == 65535
  end
  
  specify "should provide hash" do
    (0..1).respond_to?(:hash).should == true
  end
  
  specify "include? should be a synonym for ===" do
    ((0..5) === 2).should == true
    ((-5..5) === 0).should == true
    ((-1...1) === 10.5).should == false
    ((-10..-2) === -2.5).should == true
  end
  
  specify "inspect should provide a printable form" do
    (0..21).inspect.should == "0..21"
    (-8..0).inspect.should ==  "-8..0"
    (-411..959).inspect.should == "-411..959"
  end

  specify "last should be a synonym for end" do
    (-1..1).end.should == 1
    (0..1).end.should == 1
    (0xffff...0xfffff).end.should == 1048575
  end

  specify "member? should be a synonym for ===" do
    ((0..5) === 2).should == true
    ((-5..5) === 0).should == true
    ((-1...1) === 10.5).should == false
    ((-10..-2) === -2.5).should == true
  end

  specify "step should pass each nth element to the block" do
    a = []
    (-5..5).step(2) { |x| a << x }
    a.should == [-5, -3, -1, 1, 3, 5]
  end

  specify "to_s should provide a printable form" do
    (0..21).to_s.should == "0..21"
    (-8..0).to_s.should == "-8..0"
    (-411..959).to_s.should == "-411..959"
  end
end
