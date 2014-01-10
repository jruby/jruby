require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Kernel#equal?" do
  it "returns true only if obj and other are the same object" do
    o1 = mock('o1')
    o2 = mock('o2')
    (o1.equal? o1).should == true
    (o2.equal? o2).should == true
    (o1.equal? o2).should== false
    (nil.equal? nil).should == true
    (o1.equal?  nil).should== false
    (nil.equal?  o2).should== false
    ('stuff'.equal? 'stuff').should == false
    (true.equal? true).should == true
    (false.equal? false).should == true
  end

  it "returns true if obj and anObject have the same value." do
    o1 = 1
    o2 = :hola
    (:hola.equal? o1).should == false
    (1.equal? o1).should == true
    (:hola.equal? o2).should == true
  end

  it "is unaffected by overriding object_id" do
    o1 = mock("object")
    o1.stub!(:object_id).and_return(10)
    o2 = mock("object")
    o2.stub!(:object_id).and_return(10)
    o1.equal?(o2).should be_false
  end

  it "is unaffected by overriding ==" do
    # different objects, overriding == to return true
    o1 = mock("object")
    o1.stub!(:==).and_return(true)
    o2 = mock("object")
    o1.equal?(o2).should be_false

    # same objects, overriding == to return false
    o3 = mock("object")
    o3.stub!(:==).and_return(false)
    o3.equal?(o3).should be_true
  end

  it "is unaffected by overriding eql?" do
    # different objects, overriding eql? to return true
    o1 = mock("object")
    o1.stub!(:eql?).and_return(true)
    o2 = mock("object")
    o1.equal?(o2).should be_false

    # same objects, overriding eql? to return false
    o3 = mock("object")
    o3.stub!(:eql?).and_return(false)
    o3.equal?(o3).should be_true
  end

  it "is unaffected by overriding __id__" do
    o1 = mock("object")
    o1.stub!(:__id__).and_return(10)
    o2 = mock("object")
    o2.stub!(:__id__).and_return(10)
    o1.equal?(o2).should be_false
  end
end
