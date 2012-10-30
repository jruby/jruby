require File.expand_path('../../../../spec_helper', __FILE__)
require 'net/ftp'

describe "Net::FTP#passive" do
  it "returns true when self is in passive mode" do
    ftp = Net::FTP.new
    ftp.passive.should be_false

    ftp.passive = true
    ftp.passive.should be_true
  end
end

describe "Net::FTP#passive=" do
  it "sets self to passive mode when passed true" do
    ftp = Net::FTP.new

    ftp.passive = true
    ftp.passive.should be_true

    ftp.passive = false
    ftp.passive.should be_false
  end
end
