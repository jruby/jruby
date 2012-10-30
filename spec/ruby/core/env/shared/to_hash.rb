describe :env_to_hash, :shared => true do
  it "returns the ENV as a hash" do
    ENV["foo"] = "bar"
    h = ENV.send(@method)
    h.should be_an_instance_of(Hash)
    h["foo"].should == "bar"
    ENV.delete "foo"
  end

  ruby_version_is "1.9" do
    it "uses the locale encoding for keys" do
      ENV.send(@method).keys.all? {|k| k.encoding == Encoding.find('locale') }.should be_true
    end

    it "uses the locale encoding for values" do
      ENV.send(@method).values.all? {|v| v.encoding == Encoding.find('locale') }.should be_true
    end
  end
end
