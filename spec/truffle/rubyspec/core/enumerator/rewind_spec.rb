require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.8.7" do
  require File.expand_path('../../../shared/enumerator/rewind', __FILE__)

  describe "Enumerator#rewind" do
    it_behaves_like(:enum_rewind, :rewind)

    ruby_version_is "1.8.8" do
      it "calls the enclosed object's rewind method if one exists" do
        obj = mock('rewinder')
        enum = enumerator_class.new(obj)
        obj.should_receive(:each).at_most(1)
        obj.should_receive(:rewind)
        enum.rewind
      end

      it "does nothing if the object doesn't have a #rewind method" do
        obj = mock('rewinder')
        enum = enumerator_class.new(obj)
        obj.should_receive(:each).at_most(1)
        lambda { enum.rewind.should == enum }.should_not raise_error
      end
    end
  end
end
