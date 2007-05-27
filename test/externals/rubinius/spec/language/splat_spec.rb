require File.dirname(__FILE__) + '/../spec_helper'

def reverse_foo(a,b);return b,a;end

describe "Multiple assignment" do
  it "should have the proper return value" do
    (a,b,*c = *[5,6,7,8,9,10]).should == [5,6,7,8,9,10]
    (d,e = reverse_foo(4,3)).should == [3,4]
    (f,g,h = reverse_foo(6,7)).should == [7,6]
    (i,*j = *[5,6,7]).should == [5,6,7]
    (k,*l = [5,6,7]).should == [5,6,7]
    a.should == 5
    b.should == 6
    c.should == [7,8,9,10]
    d.should == 3
    e.should == 4
    f.should == 7
    g.should == 6
    h.should == nil
    i.should == 5
    j.should == [6,7]
    k.should == 5
    l.should == [6,7]
  end
end

describe "Multiple assignment, array-style" do
  it "should have the proper return value" do
    (x,y = 5,6,7).should == [5,6,7]
    x.should == 5
    y.should == 6
  end
end

