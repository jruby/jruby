# encoding: binary
require_relative '../../spec_helper'

describe "String#unpack1" do
  it "returns the first value of #unpack" do
    "ABCD".unpack1('x3C').should == "ABCD".unpack('x3C')[0]
    "\u{3042 3044 3046}".unpack1("U*").should == 0x3042
    "aG9nZWZ1Z2E=".unpack1("m").should == "hogefuga"
    "A".unpack1("B*").should == "01000001"
  end

  it "starts unpacking from the given offset" do
    "ZZABCD".unpack1('x3C', offset: 2).should == "ABCD".unpack('x3C')[0]
    "ZZZZaG9nZWZ1Z2E=".unpack1("m", offset: 4).should == "hogefuga"
    "ZA".unpack1("B*", offset: 1).should == "01000001"
  end

  it "traits offset as a bytes offset" do
    "؈".unpack("CC").should == [216, 136]
    "؈".unpack1("C").should == 216
    "؈".unpack1("C", offset: 1).should == 136
  end

  it "raises an ArgumentError when the offset is negative" do
    -> { "a".unpack1("C", offset: -1) }.should raise_error(ArgumentError, "offset can't be negative")
  end

  it "returns nil if the offset is at the end of the string" do
    "a".unpack1("C", offset: 1).should == nil
  end

  it "raises an ArgumentError when the offset is larger than the string bytesize" do
    -> { "a".unpack1("C", offset: 2) }.should raise_error(ArgumentError, "offset outside of string")
  end

  context "with format 'm0'" do
    # unpack1("m0") takes a special code path that calls Pack.unpackBase46Strict instead of Pack.unpack_m,
    # which is why we repeat the tests for unpack("m0") here.

    it "decodes base64" do
      "dA==".unpack1("m0").should == "t"
      "dGU=".unpack1("m0").should == "te"
      "dGVz".unpack1("m0").should == "tes"
      "dGVzdA==".unpack1("m0").should == "test"
      "dGVzdHQ=".unpack1("m0").should == "testt"
      "dGVzdHRl".unpack1("m0").should == "testte"
      "dGVzdHRlcw==".unpack1("m0").should == "testtes"
      "dGVzdHRlc3Q=".unpack1("m0").should == "testtest"
    end

    it "decodes an empty string" do
      "".unpack1("m0").should == ""
    end

    it "decodes all pre-encoded ascii byte values" do
      [ ["AAECAwQFBg==",                          "\x00\x01\x02\x03\x04\x05\x06"],
        ["BwgJCgsMDQ==",                          "\a\b\t\n\v\f\r"],
        ["Dg8QERITFBUW",                          "\x0E\x0F\x10\x11\x12\x13\x14\x15\x16"],
        ["FxgZGhscHR4f",                          "\x17\x18\x19\x1a\e\x1c\x1d\x1e\x1f"],
        ["ISIjJCUmJygpKissLS4v",                  "!\"\#$%&'()*+,-./"],
        ["MDEyMzQ1Njc4OQ==",                      "0123456789"],
        ["Ojs8PT4/QA==",                          ":;<=>?@"],
        ["QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVo=",  "ABCDEFGHIJKLMNOPQRSTUVWXYZ"],
        ["W1xdXl9g",                              "[\\]^_`"],
        ["YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo=",  "abcdefghijklmnopqrstuvwxyz"],
        ["e3x9fg==",                              "{|}~"],
        ["f8KAwoHCgsKD",                          "\x7f\xc2\x80\xc2\x81\xc2\x82\xc2\x83"],
        ["woTChcKGwofC",                          "\xc2\x84\xc2\x85\xc2\x86\xc2\x87\xc2"],
        ["iMKJworCi8KM",                          "\x88\xc2\x89\xc2\x8a\xc2\x8b\xc2\x8c"],
        ["wo3CjsKPwpDC",                          "\xc2\x8d\xc2\x8e\xc2\x8f\xc2\x90\xc2"],
        ["kcKSwpPClMKV",                          "\x91\xc2\x92\xc2\x93\xc2\x94\xc2\x95"],
        ["wpbCl8KYwpnC",                          "\xc2\x96\xc2\x97\xc2\x98\xc2\x99\xc2"],
        ["msKbwpzCncKe",                          "\x9a\xc2\x9b\xc2\x9c\xc2\x9d\xc2\x9e"],
        ["wp/CoMKhwqLC",                          "\xc2\x9f\xc2\xa0\xc2\xa1\xc2\xa2\xc2"],
        ["o8KkwqXCpsKn",                          "\xa3\xc2\xa4\xc2\xa5\xc2\xa6\xc2\xa7"],
        ["wqjCqcKqwqvC",                          "\xc2\xa8\xc2\xa9\xc2\xaa\xc2\xab\xc2"],
        ["rMKtwq7Cr8Kw",                          "\xac\xc2\xad\xc2\xae\xc2\xaf\xc2\xb0"],
        ["wrHCssKzwrTC",                          "\xc2\xb1\xc2\xb2\xc2\xb3\xc2\xb4\xc2"],
        ["tcK2wrfCuMK5",                          "\xb5\xc2\xb6\xc2\xb7\xc2\xb8\xc2\xb9"],
        ["wrrCu8K8wr3C",                          "\xc2\xba\xc2\xbb\xc2\xbc\xc2\xbd\xc2"],
        ["vsK/w4DDgcOC",                          "\xbe\xc2\xbf\xc3\x80\xc3\x81\xc3\x82"],
        ["w4PDhMOFw4bD",                          "\xc3\x83\xc3\x84\xc3\x85\xc3\x86\xc3"],
        ["h8OIw4nDisOL",                          "\x87\xc3\x88\xc3\x89\xc3\x8a\xc3\x8b"],
        ["w4zDjcOOw4/D",                          "\xc3\x8c\xc3\x8d\xc3\x8e\xc3\x8f\xc3"],
        ["kMORw5LDk8OU",                          "\x90\xc3\x91\xc3\x92\xc3\x93\xc3\x94"],
        ["w5XDlsOXw5jD",                          "\xc3\x95\xc3\x96\xc3\x97\xc3\x98\xc3"],
        ["mcOaw5vDnMOd",                          "\x99\xc3\x9a\xc3\x9b\xc3\x9c\xc3\x9d"],
        ["w57Dn8Ogw6HD",                          "\xc3\x9e\xc3\x9f\xc3\xa0\xc3\xa1\xc3"],
        ["osOjw6TDpcOm",                          "\xa2\xc3\xa3\xc3\xa4\xc3\xa5\xc3\xa6"],
        ["w6fDqMOpw6rD",                          "\xc3\xa7\xc3\xa8\xc3\xa9\xc3\xaa\xc3"],
        ["q8Osw63DrsOv",                          "\xab\xc3\xac\xc3\xad\xc3\xae\xc3\xaf"],
        ["w7DDscOyw7PD",                          "\xc3\xb0\xc3\xb1\xc3\xb2\xc3\xb3\xc3"],
        ["tMO1w7bDt8O4",                          "\xb4\xc3\xb5\xc3\xb6\xc3\xb7\xc3\xb8"],
        ["w7nDusO7w7zD",                          "\xc3\xb9\xc3\xba\xc3\xbb\xc3\xbc\xc3"],
        ["vcO+w78=",                              "\xbd\xc3\xbe\xc3\xbf"]
      ].should be_computed_by(:unpack1, "m0")
    end

    it "produces binary strings" do
      "".unpack1("m0").encoding.should == Encoding::BINARY
      "Ojs8PT4/QA==".unpack1("m0").encoding.should == Encoding::BINARY
    end

    it "raises an ArgumentError for an invalid base64 character" do
      -> { "dGV%zdA==".unpack1("m0") }.should raise_error(ArgumentError)
    end

    it "correctly decodes inputs longer than 2^31 / 3 characters" do
      ("X" * (2 ** 31 / 3 + 98)).unpack1("m0").length.should == 536870985
    end

    it "correctly decodes inputs longer than 2^32 / 3 characters" do
      ("X" * (2 ** 32 / 3 + 99)).unpack1("m0").length.should == 1073741898
    end
  end

  context "with format 'm0'" do
    # unpack1("m0") takes a special code path that calls Pack.unpackBase46Strict instead of Pack.unpack_m,
    # which is why we repeat the tests for unpack("m0") here.

    it "decodes base64" do
      "dGVzdA==".unpack1("m0").should == "test"
    end

    it "raises an ArgumentError for an invalid base64 character" do
      -> { "dGV%zdA==".unpack1("m0") }.should raise_error(ArgumentError)
    end
  end
end
