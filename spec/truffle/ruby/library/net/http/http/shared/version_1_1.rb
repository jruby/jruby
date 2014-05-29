ruby_version_is ''...'1.9.3' do
  describe :net_http_version_1_1_p, :shared => true do
    it "returns the state of net/http 1.1 features" do
      Net::HTTP.version_1_2
      Net::HTTP.send(@method).should be_false
      Net::HTTP.version_1_1
      Net::HTTP.send(@method).should be_true
    end
  end
end

ruby_version_is '1.9.3' do
  describe :net_http_version_1_1_p, :shared => true do
    it "returns the state of net/http 1.1 features" do
      Net::HTTP.version_1_2
      Net::HTTP.send(@method).should be_false
    end
  end
end
