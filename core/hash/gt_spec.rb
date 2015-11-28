require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "2.3" do
  describe "Hash#>" do
    before :each do
      @hash = {a:1, b:2}
      @bigger = {a:1, b:2, c:3}
      @unrelated = {c:3, d:4}
      @similar = {a:2, b:3}
    end

    it "returns false when receiver size is smaller than argument" do
      (@hash > @bigger).should == false
      (@unrelated > @bigger).should == false
    end

    it "returns false when receiver size is the same as argument" do
      (@hash > @hash).should == false
      (@hash > @unrelated).should == false
      (@unrelated > @hash).should == false
    end

    it "returns true when argument is a subset of receiver" do
      (@bigger > @hash).should == true
    end

    it "returns false when keys match but values don't" do
      (@hash > @similar).should == false
      (@similar > @hash).should == false
    end
  end
end
