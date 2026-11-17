# -*- encoding: utf-8 -*-
require_relative '../../spec_helper'
require_relative 'fixtures/classes'

describe "String#casecmp independent of case" do
  it "returns -1 when less than other" do
    "a".casecmp("b").should == -1
    "A".casecmp("b").should == -1
  end

  it "returns 0 when equal to other" do
    "a".casecmp("a").should == 0
    "A".casecmp("a").should == 0
  end

  it "returns 1 when greater than other" do
    "b".casecmp("a").should == 1
    "B".casecmp("a").should == 1
  end

  it "tries to convert other to string using to_str" do
    other = mock('x')
    other.should_receive(:to_str).and_return("abc")

    "abc".casecmp(other).should == 0
  end

  it "returns nil if other can't be converted to a string" do
    "abc".casecmp(mock('abc')).should == nil
  end

  it "returns nil if incompatible encodings" do
    "あれ".casecmp("れ".encode(Encoding::EUC_JP)).should == nil
  end

  describe "in UTF-8 mode" do
    describe "for non-ASCII characters" do
      before :each do
        @upper_a_tilde  = "Ã"
        @lower_a_tilde  = "ã"
        @upper_a_umlaut = "Ä"
        @lower_a_umlaut = "ä"
      end

      it "returns -1 when numerically less than other" do
        @upper_a_tilde.casecmp(@lower_a_tilde).should == -1
        @upper_a_tilde.casecmp(@upper_a_umlaut).should == -1
      end

      it "returns 0 when numerically equal to other" do
        @upper_a_tilde.casecmp(@upper_a_tilde).should == 0
      end

      it "returns 1 when numerically greater than other" do
        @lower_a_umlaut.casecmp(@upper_a_umlaut).should == 1
        @lower_a_umlaut.casecmp(@lower_a_tilde).should == 1
      end
    end

    describe "for ASCII characters" do
      it "returns -1 when less than other" do
        "a".casecmp("b").should == -1
        "A".casecmp("b").should == -1
      end

      it "returns 0 when equal to other" do
        "a".casecmp("a").should == 0
        "A".casecmp("a").should == 0
      end

      it "returns 1 when greater than other" do
        "b".casecmp("a").should == 1
        "B".casecmp("a").should == 1
      end
    end
  end

  describe "for non-ASCII characters" do
    before :each do
      @upper_a_tilde = "\xc3"
      @lower_a_tilde = "\xe3"
    end

    it "returns -1 when numerically less than other" do
      @upper_a_tilde.casecmp(@lower_a_tilde).should == -1
    end

    it "returns 0 when equal to other" do
      @upper_a_tilde.casecmp("\xc3").should == 0
    end

    it "returns 1 when numerically greater than other" do
      @lower_a_tilde.casecmp(@upper_a_tilde).should == 1
    end

    it "does not case fold" do
      "ß".casecmp("ss").should == 1
    end
  end

  describe "when comparing a subclass instance" do
    it "returns -1 when less than other" do
      b = StringSpecs::MyString.new "b"
      "a".casecmp(b).should == -1
      "A".casecmp(b).should == -1
    end

    it "returns 0 when equal to other" do
      a = StringSpecs::MyString.new "a"
      "a".casecmp(a).should == 0
      "A".casecmp(a).should == 0
    end

    it "returns 1 when greater than other" do
      a = StringSpecs::MyString.new "a"
      "b".casecmp(a).should == 1
      "B".casecmp(a).should == 1
    end
  end

  it "returns 0 for empty strings in different encodings" do
    ''.b.casecmp('').should == 0
    ''.b.casecmp(''.encode("UTF-32LE")).should == 0
  end
end

describe 'String#casecmp? independent of case' do
  it "compares frozen ASCII strings in ASCII-compatible encodings" do
    [Encoding::US_ASCII, Encoding::UTF_8, Encoding::ASCII_8BIT].each do |encoding|
      'aBc'.encode(encoding).freeze.casecmp?('AbC'.encode(encoding).freeze).should == true
      'abc'.encode(encoding).freeze.casecmp?('abx'.encode(encoding).freeze).should == false
    end
  end

  it "compares ASCII strings with different ASCII-compatible encodings" do
    'aBc'.b.casecmp?('ABC').should == true
  end

  it "compares bytes after an embedded NUL" do
    "aB\0cD".casecmp?("Ab\0Cd").should == true
    "aB\0cD".casecmp?("Ab\0Ce").should == false
  end

  it "returns false for strings with different byte lengths" do
    'prefix'.casecmp?('PREFIX-longer').should == false
    'longer-prefix'.casecmp?('LONGER').should == false
  end

  it "converts the argument before reading the receiver's contents" do
    receiver = +'before'
    calls = 0
    argument = Object.new
    argument.define_singleton_method(:to_str) do
      calls += 1
      receiver.replace('aBc')
      'ABC'
    end

    receiver.casecmp?(argument).should == true
    calls.should == 1
    receiver.should == 'aBc'
  end

  it "case folds Unicode characters to ASCII" do
    "\u212a".casecmp?('k').should == true
  end

  it "case folds Latin-1 characters but not binary non-ASCII bytes" do
    "\u00c4".encode('ISO-8859-1').casecmp?("\u00e4".encode('ISO-8859-1')).should == true
    "\xc4".b.casecmp?("\xe4".b).should == false
    "A\xc4".b.casecmp?("a\xc4".b).should == true
  end

  it "case folds ASCII letters in UTF-16 and UTF-32 encodings" do
    [Encoding::UTF_16LE, Encoding::UTF_16BE, Encoding::UTF_32LE, Encoding::UTF_32BE].each do |encoding|
      'aBc'.encode(encoding).casecmp?('AbC'.encode(encoding)).should == true
      'aBc'.casecmp?('aBc'.encode(encoding)).should == nil
    end
  end

  it "raises ArgumentError for invalid UTF-8" do
    -> { "a\xff".b.force_encoding(Encoding::UTF_8).casecmp?('a') }.should.raise(ArgumentError)
  end

  it "raises Encoding::CompatibilityError for a dummy encoding" do
    string = 'aBc'.dup.force_encoding('ISO-2022-JP')
    -> { string.casecmp?(string) }.should.raise(Encoding::CompatibilityError)
  end

  it "compares frozen slices without changing their source strings" do
    left = ('prefix' * 128) + ("aBc\0Def" * 64) + ('suffix' * 128)
    right = ('before' * 128) + ("AbC\0dEF" * 64) + ('after!' * 128)
    originals = [left.dup, right.dup]
    left_slice = left.byteslice(768, 448).freeze
    right_slice = right.byteslice(768, 448).freeze

    left_slice.casecmp?(right_slice).should == true
    left_slice.casecmp?(right_slice.byteslice(0, 447)).should == false
    [left, right].should == originals
  end

  it 'returns true when equal to other' do
    'abc'.casecmp?('abc').should == true
    'abc'.casecmp?('ABC').should == true
  end

  it 'returns false when not equal to other' do
    'abc'.casecmp?('DEF').should == false
    'abc'.casecmp?('def').should == false
  end

  it "tries to convert other to string using to_str" do
    other = mock('x')
    other.should_receive(:to_str).and_return("abc")

    "abc".casecmp?(other).should == true
  end

  it "returns nil if incompatible encodings" do
    "あれ".casecmp?("れ".encode(Encoding::EUC_JP)).should == nil
  end

  describe 'for UNICODE characters' do
    it 'returns true when downcase(:fold) on unicode' do
      'äöü'.casecmp?('ÄÖÜ').should == true
    end
  end

  describe "when comparing a subclass instance" do
    it 'returns true when equal to other' do
      a = StringSpecs::MyString.new "a"
      'a'.casecmp?(a).should == true
      'A'.casecmp?(a).should == true
    end

    it 'returns false when not equal to other' do
      b = StringSpecs::MyString.new "a"
      'b'.casecmp?(b).should == false
      'B'.casecmp?(b).should == false
    end
  end

  describe "in UTF-8 mode" do
    describe "for non-ASCII characters" do
      before :each do
        @upper_a_tilde  = "Ã"
        @lower_a_tilde  = "ã"
        @upper_a_umlaut = "Ä"
        @lower_a_umlaut = "ä"
      end

      it "returns true when they are the same with normalized case" do
        @upper_a_tilde.casecmp?(@lower_a_tilde).should == true
      end

      it "returns false when they are unrelated" do
        @upper_a_tilde.casecmp?(@upper_a_umlaut).should == false
      end

      it "returns true when they have the same bytes" do
        @upper_a_tilde.casecmp?(@upper_a_tilde).should == true
      end
    end
  end

  it "case folds" do
    "ß".casecmp?("ss").should == true
  end

  it "returns nil if other can't be converted to a string" do
    "abc".casecmp?(mock('abc')).should == nil
  end

  it "returns true for empty strings in different encodings" do
    ''.b.should.casecmp?('')
    ''.b.should.casecmp?(''.encode("UTF-32LE"))
  end
end
