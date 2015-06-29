# -*- encoding: utf-8 -*-
require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "2.2" do
  describe "String#unicode_normalize" do
    it "raises an Encoding::CompatibilityError if string is not in an unicode encoding" do
      lambda do
        "\xE0".force_encoding("ISO-8859-1").unicode_normalize(:nfd)
      end.should raise_error(Encoding::CompatibilityError)
    end

    # http://unicode.org/faq/normalization.html#6
    context "returns normalized form of string by default" do
      it "03D3 (ϓ) GREEK UPSILON WITH ACUTE AND HOOK SYMBOL" do
        "\u03D3".unicode_normalize(:nfc).should  == "\u03D3"
        "\u03D3".unicode_normalize(:nfd).should  == "\u03D2\u0301"
        "\u03D3".unicode_normalize(:nfkc).should == "\u038E"
        "\u03D3".unicode_normalize(:nfkd).should == "\u03A5\u0301"
      end

      it "03D4 (ϔ) GREEK UPSILON WITH DIAERESIS AND HOOK SYMBOL" do
        "\u03D4".unicode_normalize(:nfc).should  == "\u03D4"
        "\u03D4".unicode_normalize(:nfd).should  == "\u03D2\u0308"
        "\u03D4".unicode_normalize(:nfkc).should == "\u03AB"
        "\u03D4".unicode_normalize(:nfkd).should == "\u03A5\u0308"
      end

      it "1E9B (ẛ) LATIN SMALL LETTER LONG S WITH DOT ABOVE" do
        "\u1E9B".unicode_normalize(:nfc).should  == "\u1E9B"
        "\u1E9B".unicode_normalize(:nfd).should  == "\u017F\u0307"
        "\u1E9B".unicode_normalize(:nfkc).should == "\u1E61"
        "\u1E9B".unicode_normalize(:nfkd).should == "\u0073\u0307"
      end
    end
  end

  describe "String#unicode_normalize!" do
    it "modifies original string (nfc)" do
      str = "a\u0300"
      str.unicode_normalize!(:nfc)

      str.should_not == "a\u0300"
      str.should == "à"
    end

    it "modifies self in place (nfd)" do
      str = "\u00E0"
      str.unicode_normalize!(:nfd)

      str.should_not == "a\u00E0"
      str.should == "à"
    end

    it "modifies self in place (nfkc)" do
      str = "a\u0300"
      str.unicode_normalize!(:nfkc)

      str.should_not == "a\u0300"
      str.should == "à"
    end

    it "modifies self in place (nfkd)" do
      str = "\u00E0"
      str.unicode_normalize!(:nfkd)

      str.should_not == "a\u00E0"
      str.should == "à"
    end
  end

  describe "unicode_normalized?" do
    it "returns true if str is in Unicode normalization form (nfc)" do
      str = "a\u0300"
      str.unicode_normalized?(:nfc).should be_false
      str.unicode_normalize!(:nfc)
      str.unicode_normalized?(:nfc).should be_true
    end

    it "returns true if str is in Unicode normalization form (nfd)" do
      str = "a\u00E0"
      str.unicode_normalized?(:nfd).should be_false
      str.unicode_normalize!(:nfd)
      str.unicode_normalized?(:nfd).should be_true
    end

    it "returns true if str is in Unicode normalization form (nfkc)" do
      str = "a\u0300"
      str.unicode_normalized?(:nfkc).should be_false
      str.unicode_normalize!(:nfkc)
      str.unicode_normalized?(:nfkc).should be_true
    end

    it "returns true if str is in Unicode normalization form (nfkd)" do
      str = "a\u00E0"
      str.unicode_normalized?(:nfkd).should be_false
      str.unicode_normalize!(:nfkd)
      str.unicode_normalized?(:nfkd).should be_true
    end
  end
end
