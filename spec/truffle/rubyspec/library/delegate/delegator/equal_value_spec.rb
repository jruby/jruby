require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

describe "Delegator#==" do
  before :all do
    @base = mock('base')
    @delegator = DelegateSpecs::Delegator.new(@base)
  end

  ruby_version_is ""..."1.9" do
    it "is delegated when passed self" do
      @base.should_receive(:==).and_return(false)
      (@delegator == @delegator).should be_false
    end
  end

  ruby_version_is "1.9" do
    it "is not delegated when passed self" do
      @base.should_not_receive(:==)
      (@delegator == @delegator).should be_true
    end
  end
  it "is delegated when passed the delegated object" do
    @base.should_receive(:==).and_return(false)
    (@delegator == @base).should be_false
  end

  it "is delegated in general" do
    @base.should_receive(:==).and_return(true)
    (@delegator == 42).should be_true
  end
end
