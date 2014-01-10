require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

describe "Delegator#~" do
  ruby_bug "redmine:2224", "1.8" do
    it "is delegated" do
      base = mock('base')
      delegator = DelegateSpecs::Delegator.new(base)
      base.should_receive(:~).and_return(:foo)
      (~delegator).should == :foo
    end
  end
end
