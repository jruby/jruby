# -*- encoding: binary -*-
require File.expand_path('../../../spec_helper', __FILE__)

with_feature :encoding do
  describe "Encoding#replicate" do
    it "returns a replica of ASCII" do
      e = Encoding::ASCII.replicate('RS-ASCII')
      e.name.should == 'RS-ASCII'
      "a".force_encoding(e).valid_encoding?.should be_true
      "\x80".force_encoding(e).valid_encoding?.should be_false
    end

    it "returns a replica of UTF-8" do
      e = Encoding::UTF_8.replicate('RS-UTF-8')
      e.name.should == 'RS-UTF-8'
      "a".force_encoding(e).valid_encoding?.should be_true
      "\u3042".force_encoding(e).valid_encoding?.should be_true
      "\x80".force_encoding(e).valid_encoding?.should be_false
    end

    it "returns a replica of UTF-16BE" do
      e = Encoding::UTF_16BE.replicate('RS-UTF-16BE')
      e.name.should == 'RS-UTF-16BE'
      "a".force_encoding(e).valid_encoding?.should be_false
      "\x30\x42".force_encoding(e).valid_encoding?.should be_true
      "\x80".force_encoding(e).valid_encoding?.should be_false
    end

    it "returns a replica of ISO-2022-JP" do
      e = Encoding::ISO_2022_JP.replicate('RS-ISO-2022-JP')
      e.name.should == 'RS-ISO-2022-JP'
      e.dummy?.should be_true
    end
  end
end
