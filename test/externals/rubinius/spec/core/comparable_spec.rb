require File.dirname(__FILE__) + '/../spec_helper'

# <, <=, ==, >, >=, between?

context "A class with Comparable mixin, method" do
  class Weird
    include Comparable
    
    def initialize(int)
      @int = int
    end
    
    def negative?
      @int < 0
    end
    
    def <=>(other)
      return 0 if self.negative? == other.negative?
      return 1 if self.negative?
      -1
    end
  end
  
  specify "<=> should be provided" do
    Weird.new(0).respond_to?(:<=>).should == true
  end
  
  specify "<=> should return 0 if other is equal" do
    (Weird.new(-1) <=> Weird.new(-2)).should == 0
  end
  
  specify "<=> should return 1 if other is greater" do
    (Weird.new(-1) <=> Weird.new(0)).should == 1
  end
  
  specify "<=> should return -1 if other is lesser" do
    (Weird.new(1) <=> Weird.new(-1)).should == -1
  end
  
  specify "< should return true if other is greater" do
    (Weird.new(1) < Weird.new(-1)).should == true
  end
  
  specify "< should return false if other is lesser than or equal" do
    a = Weird.new(-1)
    b = Weird.new(0)

    (a < b).should == false
    (a < a).should == false
    (b < b).should == false
  end
  
  specify "<= should return true if other is greater than or equal" do
    a = Weird.new(0)
    b = Weird.new(-1)

    (a <= b).should == true
    (a <= a).should == true
    (b <= b).should == true
  end
  
  specify "<= should return false if other is lesser" do
    (Weird.new(-1) <= Weird.new(0)).should == false
  end

  specify "== should return true if other is equal" do
    a = Weird.new(0)
    b = Weird.new(-1)
    c = Weird.new(1)

    (a == c).should == true
    (a == a).should == true
    (b == b).should == true
    (c == c).should == true
  end

  specify "== should return false if other is not equal" do
    a = Weird.new(0)
    b = Weird.new(-1)
    c = Weird.new(1)

    (a == b).should == false
    (b == c).should == false
  end

  specify "> should return true if other is lesser" do
    (Weird.new(-1) > Weird.new(0)).should == true
  end

  specify "> should return false if other is greater than or equal" do
    a = Weird.new(-1)
    b = Weird.new(0)

    (b > a).should == false
    (a > a).should == false
    (b > b).should == false
  end

  specify ">= should return true if other is lesser than or equal" do
    a = Weird.new(-1)
    b = Weird.new(0)
    c = Weird.new(1)

    (b <= a).should == true
    (b <= c).should == true
  end

  specify ">= should return false if other is greater" do
    (Weird.new(1) >= Weird.new(-1)).should == false
  end

  specify "betweem? should return true if min <= self <= max" do
    a = Weird.new(-1)
    b = Weird.new(0)
    c = Weird.new(1)

    b.between?(c, a).should == true
    c.between?(b, a).should == true
  end
  
  specify "between? should return false if self < min or self > max" do
    a = Weird.new(-1)
    b = Weird.new(0)
    c = Weird.new(1)

    a.between?(b, c).should == false
    a.between?(c, b).should == false
  end
end
