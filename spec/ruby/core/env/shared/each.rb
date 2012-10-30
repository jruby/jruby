describe :env_each, :shared => true do
  it "returns each pair" do
    orig = ENV.to_hash
    e = []
    begin
      ENV.clear
      ENV["foo"] = "bar"
      ENV["baz"] = "boo"
      ENV.send(@method) { |k, v| e << [k, v] }
      e.should include(["foo", "bar"])
      e.should include(["baz", "boo"])
    ensure
      ENV.replace orig
    end
  end

  ruby_version_is "" ... "1.8.7" do
    it "raises LocalJumpError if no block given" do
      lambda { ENV.send(@method) }.should raise_error(LocalJumpError)
    end
  end

  ruby_version_is "1.8.7" do
    it "returns an Enumerator if called without a block" do
      ENV.send(@method).should be_an_instance_of(enumerator_class)
    end
  end

  ruby_version_is "1.9" do
    it "uses the locale encoding" do
      ENV.send(@method) do |key, value|
        key.encoding.should == Encoding.find('locale')
        value.encoding.should == Encoding.find('locale')
      end
    end
  end
end
