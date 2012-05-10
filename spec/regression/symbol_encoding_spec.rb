require 'rspec'

if RUBY_VERSION >= "1.9.2"
  describe "symbol encoding" do
    it "should be US-ASCII" do
      :foo.encoding.name.should == "US-ASCII"
    end

    it "should be US-ASCII after converting to string" do
      :foo.to_s.encoding.name.should == "US-ASCII"
    end
  end
end
