require File.expand_path('../../../../../spec_helper', __FILE__)
require 'net/http'
require File.expand_path('../shared/version_1_1', __FILE__)

ruby_version_is ''...'1.9.3' do
  describe "Net::HTTP.version_1_1" do
    it "turns on net/http 1.1 features" do
      Net::HTTP.version_1_1

      Net::HTTP.version_1_1?.should be_true
      Net::HTTP.version_1_2?.should be_false
    end

    it "returns false" do
      Net::HTTP.version_1_1.should be_false
    end
  end
end

describe "Net::HTTP.version_1_1?" do
  it_behaves_like :net_http_version_1_1_p, :version_1_1?
end
