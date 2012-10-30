require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do
  require 'ping'

  describe "Ping.pingecho" do
    it "pings a host using the correct number of arguments" do
      Ping.pingecho('127.0.0.1', 10, 7).should be_true
      Ping.pingecho('127.0.0.1', 10).should be_true
      Ping.pingecho('127.0.0.1').should be_true
    end

    it "returns false if the port is invalid" do
      Ping.pingecho('127.0.0.1', 5, 'invalid port').should be_false
    end

    it "returns false if the timeout value is invalid" do
      Ping.pingecho('127.0.0.1', 'invalid timeout').should be_false
    end

    # Is it ever possible to spec invalid hosts? I think the consensus
    # is building towards "no".
    quarantine! do
      it "returns false if the host is invalid" do
        Ping.pingecho(0).should be_false
      end
    end
  end
end
