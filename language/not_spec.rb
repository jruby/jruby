require File.expand_path('../../spec_helper', __FILE__)

describe "The not keyword" do
  it "negates a `true' value" do
    (not true).should be_false
    (not 'true').should be_false
  end

  it "negates a `false' value" do
    (not false).should be_true
    (not nil).should be_true
  end

  it "accepts an argument" do
    lambda do
      not(true)
    end.should_not raise_error(SyntaxError)
  end

  it "returns false if the argument is true" do
    (not(true)).should be_false
  end

  it "returns true if the argument is false" do
    (not(false)).should be_true
  end

  it "returns true if the argument is nil" do
    (not(nil)).should be_true
  end
end

describe "not()" do
  # not(arg).method and method(not(arg)) raise SyntaxErrors on 1.8. Here we
  # use #inspect to test that the syntax works on 1.9

  it "can be used as a function" do
    lambda do
      not(true).inspect
    end.should_not raise_error(SyntaxError)
  end

  it "returns false if the argument is true" do
    not(true).inspect.should == "false"
  end

  it "returns true if the argument is false" do
    not(false).inspect.should == "true"
  end

  it "returns true if the argument is nil" do
    not(false).inspect.should == "true"
  end
end

describe "The `!' keyword" do
  it "negates a `true' value" do
    (!true).should be_false
    (!'true').should be_false
  end

  it "negates a `false' value" do
    (!false).should be_true
    (!nil).should be_true
  end

  it "turns a truthful object into `true'" do
    (!!true).should be_true
    (!!'true').should be_true
  end

  it "turns a not truthful object into `false'" do
    (!!false).should be_false
    (!!nil).should be_false
  end
end
