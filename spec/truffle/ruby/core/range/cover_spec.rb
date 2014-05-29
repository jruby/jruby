require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.8.8" do
  describe "Range#cover?" do

    it "raises an ArgumentError without exactly one argument" do
      lambda{ (1..2).cover? }.should raise_error(ArgumentError)
      lambda{ (1..2).cover?(1,2) }.should raise_error(ArgumentError)
    end

    it "returns true if argument is equal to the first value of the range" do
      (0..5).cover?(0).should be_true
      ('f'..'s').cover?('f').should be_true
    end

    it "returns true if argument is equal to the last value of the range" do
      (0..5).cover?(5).should be_true
      (0...5).cover?(4).should be_true
      ('f'..'s').cover?('s').should be_true
    end

    it "returns true if argument is less than the last value of the range and greater than the first value" do
      (20..30).cover?(28).should be_true
      ('e'..'h').cover?('g').should be_true
      ("\u{999}".."\u{9999}").cover? "\u{9995}"
    end

    it "returns true if argument is sole element in the range" do
      (30..30).cover?(30).should be_true
    end

    it "returns false if range is empty" do
      (30...30).cover?(30).should be_false
      (30...30).cover?(nil).should be_false
    end

    it "returns false if the range does not contain the argument" do
      ('A'..'C').cover?(20.9).should be_false
      ('A'...'C').cover?('C').should be_false
    end

    it "uses the range element's <=> to make the comparison" do
      a = mock('a')
      a.should_receive(:<=>).twice.and_return(-1,-1)
      (a..'z').cover?('b').should be_true
    end

    it "uses a continuous inclusion test" do
      ('a'..'f').cover?('aa').should be_true
      ('a'..'f').cover?('babe').should be_true
      ('a'..'f').cover?('baby').should be_true
      ('a'..'f').cover?('ga').should be_false
      (-10..-2).cover?(-2.5).should be_true
    end

  end
end
