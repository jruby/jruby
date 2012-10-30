require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../shared/take', __FILE__)

ruby_version_is "1.8.7" do
  describe "Enumerable#first" do
    it "returns the first element" do
      EnumerableSpecs::Numerous.new.first.should == 2
      EnumerableSpecs::Empty.new.first.should == nil
    end

    it "returns nil if self is empty" do
      EnumerableSpecs::Empty.new.first.should == nil
    end

    describe "when passed an argument" do
      it_behaves_like :enumerable_take, :first
    end
  end
end
