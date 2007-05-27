require File.dirname(__FILE__) + '/../spec_helper'

context "CType instance method" do
  specify "isspace should return true if self is ASCII whitespace" do
    a = []
    "\tt\fi a\r\nt\v".each_byte { |b| a << b.isspace }
    a.should == [true, false, true, false, true, false, true, true, false, true]
  end
  
  specify "isupper should return true if self is between A..Z" do
    a = []
    "MenTaLguY".each_byte { |b| a << b.isupper }
    a.should == [true, false, false, true, false, true, false, false, true]
  end
  
  specify "islower should return true if self is between a..z" do
    a = []
    "MenTaLguY".each_byte { |b| a << b.islower }
    a.should == [false, true, true, false, true, false, true, true, false]
  end
  
  specify "isalnum should return true if self is between A..Z, a..z, 0..9" do
    a = []
    "aAbB9;2-\0005Zt!@#b{$}x9".each_byte { |b| a << b.isalnum }
    a.should == [ true, true, true, true, true, false, true, false, false, true, true, 
        true, false, false, false, true, false, false, false, true, true]
  end
  
  specify "isdigit should return true if self is between 0..9" do
    a = []
    "0a1b2C3;4:5d6=7+8p9".each_byte { |b| a << b.isdigit }
    a.should == [ true, false, true, false, true, false, true, false, true, false, true, 
        false, true, false, true, false, true, false, true]
  end
  
  specify "tolower should return self in range a..z if self is between A..Z, otherwise return self" do
    a = []
    "MenTaLguY".each_byte { |b| a << b.tolower }
    a.should == [109, 101, 110, 116, 97, 108, 103, 117, 121]
  end
  
  specify "toupper should return a value in the range A..Z if self is between a..z, otherwise return self" do
    a = []
    "MenTaLguY".each_byte { |b| a << b.toupper }
    a.should == [77, 69, 78, 84, 65, 76, 71, 85, 89]
  end
end
