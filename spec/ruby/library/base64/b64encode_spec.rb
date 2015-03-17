require File.expand_path('../../../spec_helper', __FILE__)

require 'base64'

ruby_version_is "" ... "1.9" do
  describe "Base64#b64encode" do
    it "returns the Base64-encoded version of the given string with a newline at 60 characters" do
      b64encoded_version = "Tm93IGlzIHRoZSB0aW1lIGZvciBhbGwgZ29vZCBjb2RlcnMgdG8gbGVhcm4g\nUnVieQ==\n"
      lambda {
        Base64.b64encode("Now is the time for all good coders to learn Ruby").should == b64encoded_version
      }.should output
    end

    it "prints the Base64-encoded version of the given string with a newline after 60 characters" do
      b64encoded_version ="Tm93IGlzIHRoZSB0aW1lIGZvciBhbGwgZ29vZCBjb2RlcnMgdG8gbGVhcm4g\nUnVieQ==\n"
      lambda {
        Base64.b64encode("Now is the time for all good coders to learn Ruby")
      }.should output(b64encoded_version)
    end
  end

  describe "Base64#b64encode with length" do
    it "returns the Base64-encoded version of the given string with a newline at 60 characters" do
      b64encoded_version = "Tm93IGlzIHRoZSB0aW1lIGZvciBhbGwgZ29vZCBjb2RlcnMgdG8gbGVhcm4g\nUnVieQ==\n"
      lambda {
        Base64.b64encode("Now is the time for all good coders to learn Ruby", 2).should == b64encoded_version
      }.should output
    end

    it "prints the Base64-encoded version of the given stringwith a newline after length characters" do
      lambda {
        Base64.b64encode("hello", 2).should == "aGVsbG8=\n"
      }.should output("aG\nVs\nbG\n8=\n")
    end
  end
end
