require File.dirname(__FILE__) + '/../spec_helper'

# &, ^, inspect, nil?, to_a, to_f, to_i, to_s, |

context "NilClass" do
  def nil_and(other)
    nil & other
  end
  
  def nil_or(other)
    nil | other
  end
  
  def nil_xor(other)
    nil ^ other
  end
  
  specify "& should return false" do
    nil_and(nil).should == false
    nil_and(true).should == false
    nil_and(false).should == false
    nil_and("").should == false
    nil_and(Object.new).should == false
  end

  specify "^ should return false if other is nil or false, otherwise true" do
    nil_xor(nil).should == false
    nil_xor(true).should == true
    nil_xor(false).should == false
    nil_xor("").should == true
    nil_xor(Object.new).should == true
  end

  specify "inspect should return the string 'nil'" do
    nil.inspect.should == "nil"
  end

  specify "nil? should return true" do
    nil.nil?.should == true
  end

  specify "to_a should return an empty array" do
    nil.to_a.should == []
  end

  specify "to_f should return 0.0" do
    nil.to_f.should == 0.0
  end

  specify "to_f should not cause NilClass to be coerced into Float" do
    (0.0 == nil).should == false
  end

  specify "to_i should return 0" do
    nil.to_i.should == 0
  end

  specify "to_i should not cause NilClass to be coerced into Fixnum" do
    (0 == nil).should == false
  end

  specify "to_s should return ''" do
    nil.to_s.should == ""
  end

  specify "| should return false if other is nil or false, otherwise true" do
    nil_or(nil).should == false
    nil_or(true).should == true
    nil_or(false).should == false
    nil_or("").should == true
    nil_or(Object.new).should == true
  end
end
