require File.expand_path('../../spec_helper', __FILE__)

class UndefSpecClass
  def meth(other);other;end
end

class UndefMultipleAtOnce
  def method1; end
  def method2; :nope; end

  undef :method1, :method2
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

  it "allows undefining multiple methods at a time" do
    obj = UndefMultipleAtOnce.new
    obj.respond_to?(:method1).should == false
    obj.respond_to?(:method2).should == false
  end

  it "raises a NameError when passed a missing name" do
    lambda { class ::UndefSpecClass; undef not_exist; end }.should raise_error(NameError) { |e|
      # a NameError and not a NoMethodError
      e.class.should == NameError
    }
  end
end
