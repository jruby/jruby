require File.expand_path('../../spec_helper', __FILE__)

class UndefSpecClass
  def meth(other);other;end
end

describe "The undef keyword" do
  it "undefines a method" do
    obj = ::UndefSpecClass.new
    (obj.meth 5).should == 5
    class ::UndefSpecClass
      undef meth
    end
    lambda { obj.meth 5 }.should raise_error(NoMethodError)
  end

  it "raises a NameError when passed a missing name" do
    lambda { class ::UndefSpecClass; undef not_exist; end }.should raise_error(NameError)
    # a NameError and not a NoMethodError
    lambda { class ::UndefSpecClass; undef not_exist; end }.should_not raise_error(NoMethodError)
  end
end
