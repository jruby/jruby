# encoding: binary
require_relative '../../../spec_helper'
require_relative '../fixtures/classes'
require_relative 'shared/basic'

describe "Array#pack with format 'x'" do
  it_behaves_like :array_pack_basic, 'x'
  it_behaves_like :array_pack_basic_non_float, 'x'
  it_behaves_like :array_pack_no_platform, 'x'

  it "adds a NULL byte with an empty array" do
    [].pack("x").should == "\x00"
  end

  it "adds a NULL byte without consuming an element" do
    [1, 2].pack("CxC").should == "\x01\x00\x02"
  end

  it "is not affected by a previous count modifier" do
    [].pack("x3x").should == "\x00\x00\x00\x00"
  end

  it "adds multiple NULL bytes when passed a count modifier" do
    [].pack("x3").should == "\x00\x00\x00"
  end

  it "does not add a NULL byte if the count modifier is zero" do
    [].pack("x0").should == ""
  end

  it "does not add a NULL byte when passed the '*' modifier" do
    [].pack("x*").should == ""
    [1, 2].pack("Cx*C").should == "\x01\x02"
  end

  ruby_version_is "4.1" do
    it "aligns to the given byte boundary with the '!' modifier" do
      [1, 2].pack("C x!4 C").should == "\x01\x00\x00\x00\x02"
    end

    it "aligns from the beginning of the output with '@!'" do
      buffer = +"z"
      [1, 2].pack("C @!4 C", buffer: buffer)
      buffer.should == "z\x01\x00\x00\x02"
    end

    it "aligns to a directive's alignment with the '!' modifier" do
      [1, 2].pack("C x!i i").should == [1, 2].pack("i< i")
    end

    it "aligns from the start of the new output with 'x!' when given a buffer" do
      buffer = +"z"
      [1, 2].pack("C x!4 C", buffer: buffer)
      buffer.should == "z\x01\x00\x00\x00\x02"
    end

    it "accepts the '_' and '!' modifiers after the alignment directive" do
      [1, 2].pack("C x!s_ C").should == "\x01\x00\x02"
      [1, 2].pack("C x!s! C").should == "\x01\x00\x02"
    end

    it "raises an ArgumentError when the '!' modifier has no alignment" do
      -> { [1].pack("C x!") }.should.raise(ArgumentError, "missing alignment")
    end

    it "raises a RangeError for an alignment of zero" do
      -> { [1].pack("C x!0") }.should.raise(RangeError, "invalid alignment")
    end

    it "raises a RangeError for an alignment that does not fit in an unsigned long" do
      -> { [1].pack("C x!99999999999999999999") }.should.raise(RangeError, "invalid alignment")
    end

    it "raises an ArgumentError for an unknown alignment directive" do
      -> { [1].pack("C x!k") }.should.raise(ArgumentError, /unknown alignment directive 'k'/)
    end

    it "raises an ArgumentError for a modifier after a directive that does not take it" do
      -> { [1].pack("C x!c_") }.should.raise(ArgumentError, /'_' allowed only after types/)
      -> { [1].pack("C x!c<") }.should.raise(ArgumentError, /'<' allowed only after types/)
    end

    it "raises an ArgumentError for an endian modifier after the alignment directive" do
      -> { [1].pack("C x!s<") }.should.raise(ArgumentError, "endian modifier is not allowed for alignment")
      -> { [1].pack("C @!s>") }.should.raise(ArgumentError, "endian modifier is not allowed for alignment")
    end

    it "raises a RangeError for both '<' and '>' before rejecting the endian modifier" do
      -> { [1].pack("C x!s<>") }.should.raise(RangeError, "Can't use both '<' and '>'")
    end
  end
end

describe "Array#pack with format 'X'" do
  it_behaves_like :array_pack_basic, 'X'
  it_behaves_like :array_pack_basic_non_float, 'X'
  it_behaves_like :array_pack_no_platform, 'X'

  it "reduces the output string by one byte at the point it is encountered" do
    [1, 2, 3].pack("C2XC").should == "\x01\x03"
  end

  it "does not consume any elements" do
    [1, 2, 3].pack("CXC").should == "\x02"
  end

  it "reduces the output string by multiple bytes when passed a count modifier" do
    [1, 2, 3, 4, 5].pack("C2X2C").should == "\x03"
  end

  it "has no affect when passed the '*' modifier" do
    [1, 2, 3].pack("C2X*C").should == "\x01\x02\x03"
  end

  it "raises an ArgumentError if the output string is empty" do
    -> { [1, 2, 3].pack("XC") }.should.raise(ArgumentError)
  end

  it "raises an ArgumentError if the count modifier is greater than the bytes in the string" do
    -> { [1, 2, 3].pack("C2X3") }.should.raise(ArgumentError)
  end
end
