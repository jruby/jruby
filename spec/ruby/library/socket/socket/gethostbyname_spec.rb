# -*- encoding: US-ASCII -*-
require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

require 'socket'

describe "Socket#gethostbyname" do
  it "returns broadcast address info for '<broadcast>'" do
    addr = Socket.gethostbyname('<broadcast>').first;
    ["broadcasthost", "255.255.255.255"].should include(addr)
  end

  it "returns broadcast address info for '<any>'" do
    addr = Socket.gethostbyname('<any>').first;
    addr.should == "0.0.0.0"
  end

  it "returns address list in pack format (IPv4)" do
    laddr = Socket.gethostbyname('127.0.0.1')[3..-1];
    laddr.should == ["\x7f\x00\x00\x01"]
  end

  it "returns address list in pack format (IPv6)" do
    laddr = Socket.gethostbyname('::1')[3..-1]
    laddr.should == ["\x00" * 15 + "\x01"]
  end
end
