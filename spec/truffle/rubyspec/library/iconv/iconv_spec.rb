# -*- encoding: utf-8 -*-
require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/initialize_exceptions', __FILE__)
require File.expand_path('../fixtures/classes.rb', __FILE__)

# These specs assume the Iconv implementation supports at least
# the following encodings:
#   us-ascii, utf-8, utf-16, utf-16be, utf-16le, iso-8859-1

ruby_version_is ''...'2.0' do 
  describe "Iconv#iconv" do
    it "raises an ArgumentError when called on a closed converter" do
      conv = Iconv.new("us-ascii", "us-ascii")
      conv.close
      lambda { conv.iconv("test") }.should raise_error(ArgumentError)
    end

    it "when given a string or string-like parameter returns a converted version of it" do
      Iconv.open "utf-8", "iso-8859-1" do |conv|
        conv.iconv("expos\xe9").should == "expos\xc3\xa9"

        stringlike = mock("string-like")
        stringlike.should_receive(:to_str).and_return("r\xe9sum\xe9")
        conv.iconv(stringlike).should == "r\xc3\xa9sum\xc3\xa9"
      end
    end

    it "keeps context between calls" do
      Iconv.open "utf-16", "us-ascii" do |conv|
        # BOM for first call of utf-16
        conv.iconv("a").should equal_utf16("\xfe\xff\0a")
        # no BOM for consecutive calls
        conv.iconv("a").should equal_utf16("\0a")
      end
    end

    it "when given a start and end position returns the substring" do
      Iconv.open "us-ascii", "us-ascii" do |conv|
        conv.iconv("testing", 1, 4).should == "esti"
        conv.iconv("testing", 2, 1).should == "s"
      end
    end

    it "when given a negative start position counts from the end of string" do
      Iconv.open "us-ascii", "us-ascii" do |conv|
        conv.iconv("testing", -7, 4).should == "test"
        conv.iconv("testing", -3, 7).should == "ing"
      end
    end

    it "when the end parameter is omitted or nil goes until the end of the string" do
      Iconv.open "us-ascii", "us-ascii" do |conv|
        conv.iconv("testing", 0).should == "testing"
        conv.iconv("testing", 4).should == "ing"
        conv.iconv("testing", 4, nil).should == "ing"
        conv.iconv("testing", -3).should == "ing"
        conv.iconv("testing", -4, nil).should == "ting"
      end
    end

    ruby_bug "[ruby-core:17092]", "1.8.6.258" do
      it "when given a positive length" do
        Iconv.open "us-ascii", "us-ascii" do |conv|
          conv.iconv("testing", 0, 4).should == "test"
          conv.iconv("testing", 4, 6).should == "ing"
          conv.iconv("substring", -6, 6).should == "string"
        end
      end

      it "when given a negative length" do
        Iconv.open "us-ascii", "us-ascii" do |conv|
          conv.iconv("testing", 0, -1).should == "testing"
          conv.iconv("testing", 2, -4).should == "sting"
          conv.iconv("substring", -6, -4).should == "string"
        end
      end
    end

    it "raises Iconv::IllegalSequence when faced with an invalid byte for the source encoding" do
      Iconv.open "utf-8", "utf-8" do |conv|
        lambda { conv.iconv("test\x80") }.should raise_error(Iconv::IllegalSequence)
      end
    end

    platform_is :linux, :darwin, :freebsd do
      # glibc iconv and GNU libiconv wrongly raises EILSEQ.
      # Linux, Darwin, and FreeBSD usually use them.
      # NetBSD's libc iconv, Citrus iconv, correctly behaves as POSIX,
      # but on NetBSD users may install GNU libiconv and use it.
      it "raises Iconv::IllegalSequence when a character cannot be represented on the target encoding" do
        Iconv.open "us-ascii", "utf-8" do |conv|
          lambda { conv.iconv("euro \xe2\x82\xac") }.should raise_error(Iconv::IllegalSequence)
        end
      end
    end

    it "raises Iconv::InvalidCharacter when an incomplete character or shift sequence happens at the end of the input buffer" do
      Iconv.open "utf-8", "utf-8" do |conv|
        lambda { conv.iconv("euro \xe2") }.should raise_error(Iconv::InvalidCharacter)
        lambda { conv.iconv("euro \xe2\x82") }.should raise_error(Iconv::InvalidCharacter)
      end
      Iconv.open "utf-16be", "utf-16be" do |conv|
        lambda { conv.iconv("a") }.should raise_error(Iconv::InvalidCharacter)
      end
    end

    ruby_bug "#17910", "1.8.6.114" do
      it "sanitizes invalid upper bounds" do
        Iconv.open "us-ascii", "us-ascii" do |conv|
          conv.iconv("testing", 0, 99).should == "testing"
          conv.iconv("testing", 10, 12).should == ""
        end
      end
    end

    it "returns a blank string on invalid lower bounds" do
      Iconv.open "us-ascii", "us-ascii" do |conv|
        conv.iconv("testing", -10, -8).should == ""
        conv.iconv("testing", -8).should == ""
        conv.iconv("testing", -9, 5).should == ""
      end
    end
  end

  describe "Iconv.iconv" do
    it "converts a series of strings with a single converter" do
      ary = [encode("\0a\0b\0c", "utf-16be"), encode("\0d\0e", "utf-16be")]
      Iconv.iconv("utf-16be", "us-ascii", "abc", "de").should == ary
      # BOM only on first string
      Iconv.iconv("utf-16", "utf-8", "abc", "de").should equal_utf16(["\xfe\xff\0a\0b\0c", "\0d\0e"])
    end

    it "returns an empty array when given no strings to convert" do
      Iconv.iconv("us-ascii", "utf-8").should == []
    end

    it_behaves_like :iconv_initialize_exceptions, :iconv, "test"

    platform_is :linux, :darwin, :freebsd do
      # //ignore is glibc iconv and GNU libiconv specific behavior, not POSIX
      describe "using the ignore option" do
        # This spec exists because some implementions of libiconv return
        # an error for this sequence even though they consume all of the
        # input and write the proper output. We want to be sure that those
        # platforms ignore the error and give us the data back.
        #
        it "causes unknown bytes to be ignored" do
          str = "f\303\266\303\266 bar" # this is foo bar, with umlate o's
          Iconv.iconv('ascii//ignore', 'utf-8', str)[0].should == "f bar"
        end
      end
    end
  end

  describe "The 'utf-8' encoder" do
    it "emits proper representations for characters outside the Basic Multilingual Plane" do
      Iconv.iconv("utf-8", "utf-16be", "\xd8\x40\xdc\x00").should == ["\xf0\xa0\x80\x80"]
    end
  end

  describe "The 'utf-16' encoder" do

    ruby_version_is "".."1.8.6p230" do
      it "emits an empty string when the source input is empty" do
        Iconv.iconv("utf-16", "us-ascii", "", "").should == ["", ""]
        Iconv.open "utf-16", "utf-8" do |conv|
          conv.iconv("").should == ""
          conv.iconv("test", 1, 1).should == ""
          conv.iconv("test", 3, -3).should == ""
          conv.iconv("test", 1, -4).should == ""
        end
      end
    end

    ruby_version_is "1.8.6p238".."1.9" do
      it "emits an empty string when the source input is empty" do
        Iconv.iconv("utf-16", "us-ascii", "", "").should == ["", ""]
        Iconv.open "utf-16", "utf-8" do |conv|
          conv.iconv("").should == ""
          conv.iconv("test", 1, 0).should == ""
        end
      end
    end

    it "emits a byte-order mark on first non-empty output" do
      Iconv.iconv("utf-16", "us-ascii", "a").should equal_utf16(["\xfe\xff\0a"])
      Iconv.iconv("utf-16", "utf-16", "\x80\x80", "\x81\x81").should equal_utf16(["\xfe\xff\x80\x80", "\x81\x81"])
    end
  end

  describe "The 'utf-16be' decoder" do
    it "does not emit a byte-order mark" do
      Iconv.iconv("utf-16be", "utf-8", "ab").should == [encode("\0a\0b", "utf-16be")]
    end

    it "treats possible byte-order marks as regular characters" do
      Iconv.iconv("utf-8", "utf-16be", "\xfe\xff\0a").should == ["\xef\xbb\xbfa"]
      Iconv.iconv("utf-8", "utf-16be", "\xff\xfe\0a").should == ["\xef\xbf\xbea"]
    end
  end

  describe "The 'utf-16le' decoder" do
    it "does not emit a byte-order mark" do
      Iconv.iconv("utf-16le", "utf-8", "ab").should == [encode("a\0b\0", "utf-16le")]
    end

    it "treats possible byte-order marks as regular characters" do
      Iconv.iconv("utf-8", "utf-16le", "\xfe\xff\0a").should == ["\xef\xbf\xbe\xe6\x84\x80"]
      Iconv.iconv("utf-8", "utf-16le", "\xff\xfe\0a").should == ["\xef\xbb\xbf\xe6\x84\x80"]
    end
  end
end
