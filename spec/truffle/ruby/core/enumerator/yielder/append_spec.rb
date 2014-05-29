require File.expand_path('../../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "Enumerator::Yielder#<<" do
    # TODO: There's some common behavior between yield and <<; move to a shared spec
    it "yields the value to the block" do
      ary = []
      y = Enumerator::Yielder.new {|x| ary << x}
      y << 1
      
      ary.should == [1]
    end
    
    it "returns the the yielder" do
      y = Enumerator::Yielder.new {|x| x + 1}
      (y << 1).should == y
    end
  end
end
