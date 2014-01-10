require File.expand_path('../../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "Enumerator::Generator#each" do
    it "Supports enumeration with a block" do
      g = Enumerator::Generator.new { |y| y << 3 << 2 << 1 }
      r = []
      g.each { |v| r << v }

      r.should == [3, 2, 1]
    end

    it "Raises a LocalJumpError if no block given" do
      g = Enumerator::Generator.new { |y| y << 3 << 2 << 1 }
      lambda { g.each }.should raise_error(LocalJumpError)
    end
  end
end
