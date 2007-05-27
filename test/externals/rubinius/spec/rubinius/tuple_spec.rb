require File.dirname(__FILE__) + '/../spec_helper'

describe "Tuple instance method" do
  specify "new should create a Tuple of specified size" do
    Tuple.new(2).fields.should == 2
    Tuple.new(2).size.should == 2
    Tuple.new(2).length.should == 2
  end
  
  specify "new(0) should create a Tuple of zero size" do
    Tuple.new(0).fields.should == 0
    Tuple.new(0).size.should == 0
    Tuple.new(0).length.should == 0
  end
  
  specify "put should insert an element at the specified index" do
    t = Tuple.new(1)
    t.put(0, "Whee")
    t.at(0).should == "Whee"
  end
  
  specify "at should retrieve the element at the specified index" do
    t = Tuple.new(3)
    t.put(2, 'three')
    t.at(2).should == 'three'
  end
  
  specify "put should raise InvalidIndex when index is greater than tuple size" do
    t = Tuple.new(1)
    should_raise(InvalidIndex) { t.put(1,'wrong') }
  end
  
  specify "put should raise InvalidIndex when index is less than zero" do
    t = Tuple.new(1)
    should_raise(InvalidIndex) { t.put(-1,'wrong') }
  end
  
  specify "at should raise InvalidIndex when index is greater than tuple size" do
    t = Tuple.new(1)
    should_raise(InvalidIndex) { t.at(1) }
  end
  
  specify "at should raise InvalidIndex when index is less than zero" do
    t = Tuple.new(1)
    should_raise(InvalidIndex) { t.at(-1) }
  end
end
