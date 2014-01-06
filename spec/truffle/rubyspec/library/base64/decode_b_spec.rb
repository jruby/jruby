require File.expand_path('../../../spec_helper', __FILE__)

require 'base64'

ruby_version_is "" ... "1.9" do
  describe "Base64#decode_b" do
    it "supports an encoding type of base64 and the charachter set SHIFT_JIS" do
      Base64.decode_b("=?SHIFT_JIS?B?Zm9v?=").should == 'foo'
    end

    it "supports an encoding type of base64 and the character set ISO-2022-JP" do
      Base64.decode_b("=?ISO-2022-JP?B?Zm9v?=").should == 'foo'
    end

    # mSpec doesn't have pending specs yet
    # Waiting on Kconv implementation
    # it "decodes MIME encoded string and convert halfwidth katakana to fullwidth katakana."
  end
end
