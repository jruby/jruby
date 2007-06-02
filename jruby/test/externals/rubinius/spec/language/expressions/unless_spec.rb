require File.dirname(__FILE__) + '/../../spec_helper'

describe "The unless expression" do
  it "should evaluate the unless body when the expression is false" do
    unless false
      a = true
    else
      a = false
    end
    
    a.should == true
  end
  
  it "should return the last statement in the body" do
    unless false
      'foo'
      'bar'
      'baz'
    end.should == 'baz'
  end
  
  it "should evaluate the else body when the expression is true" do
    unless true
      'foo'
    else
      'bar'
    end.should == 'bar'
  end
  
  it "should take an optional then after the expression" do
    unless false then
      'baz'
    end.should == 'baz'
  end
  
  it "should have no return value when the expression is true" do
    unless true; end.should == nil
  end

  it "should allow expression and body to be on one line (using ':')" do
    unless false: 'foo'; else 'bar'; end.should == 'foo'
  end
  
  it "should allow expression and body to be on one line (using 'then')" do
    unless false then 'foo'; else 'bar'; end.should == 'foo'
  end
end