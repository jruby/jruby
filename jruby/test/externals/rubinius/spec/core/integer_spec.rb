require File.dirname(__FILE__) + '/../spec_helper'

# class methods
# induced_from

# ceil, chr, downto, floor, integer?,
# next, numerator, prime_division, round, succ, times, to_i,
# to_int, truncate, upto

context "Integer class method" do
  specify "induced_from should convert other to Integer" do
    Integer.induced_from(2.5).should == 2 
    Integer.induced_from(-3.14).should == -3 
    Integer.induced_from(1.233450999123389e+12).should == 1233450999123
  end
end

context "Integer instance method" do
  specify "ceil should be a synonym for to_i" do
    a = 1
    a.ceil.should == 1 
    a.ceil.eql?(a).should == true
  end
  
  specify "chr should return a string containing the ASCII character represented by self" do
    [82.chr, 117.chr, 98.chr, 105.chr, 110.chr, 105.chr, 117.chr, 115.chr, 
     32.chr, 
     114.chr, 111.chr, 99.chr, 107.chr, 115.chr].should == ["R", "u", "b", "i", "n", "i", "u", "s",
                                                            " ", 
                                                            "r", "o", "c", "k", "s"]
  end

  specify "chr should return a new string" do
     82.chr.equal?(82.chr).should == false
  end
  
  specify "downto should pass block decreasing values from self down to and including other Integer" do
    a = []
    3.downto(-1) { |i| a << i }
    -1.downto(3) { |i| a << i }
    a.should == [3, 2, 1, 0, -1]
  end
  
  specify "floor should return the largest integer less than or equal to" do
    0.floor.should == 0 
    -1.floor.should == -1 
    1.floor.should == 1 
    0xffffffff.floor.should == 4294967295
  end
  
  specify "integer? should return true" do
    0.integer?.should == true 
    0xffffffff.integer?.should == true
    -1.integer?.should == true
  end
  
  specify "next should return the Integer equal to self + 1" do
    0.next.should == 1 
    -1.next.should == 0
    0xffffffff.next.should == 4294967296 
    20.next.should == 21
  end
  
  specify "round should be a synonym for to_i" do
    a = 1
    a.round.should == a.to_i
    a.round.eql?(a).should == a.to_i.eql?(a)
  end
  
  specify "succ should be an alias for next" do
    0.succ.should == 0.next
    -1.succ.should == -1.next
    0xffffffff.succ.should == 0xffffffff.next
    20.succ.should == 20.next
  end

  specify "times should pass block values from 0 to self - 1" do
    a = []
    9.times { |i| a << i }
    -2.times { |i| a << i }
    a.should == [0, 1, 2, 3, 4, 5, 6, 7, 8]
  end
  
  specify "to_i should return self" do
    a = 1
    a.to_i.should == 1
    a.to_i.eql?(a).should == true
  end
  
  specify "to_int should be a synonym for to_i" do
    a = 1
    a.to_int.should == a.to_i
    a.to_int.eql?(a).should == a.to_i.eql?(a)
  end
  
  specify "truncate should be a synonym for to_i" do
    a = 1
    a.truncate.should == a.to_i
    a.truncate.eql?(a).should == a.to_i.eql?(a)
  end
  
  specify "upto should pass block values from self up to and including other Integer" do
    a = []
    9.upto(13) { |i| a << i }
    2.upto(-1) { |i| a << i }
    a.should == [9, 10, 11, 12, 13]
  end
end
