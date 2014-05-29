require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

ruby_version_is "1.9" do
  describe "Delegator#!" do
    it "is delegated" do
      base = mock('base')
      delegator = DelegateSpecs::Delegator.new(base)
      base.should_receive(:"!").and_return(:foo)
      (!delegator).should == :foo
    end
  end
end
