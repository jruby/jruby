# encoding: utf-8

describe :strscan_pos, :shared => true do
  before :each do
    @s = StringScanner.new("This is a test")
    @m = StringScanner.new("cölorfül")
  end

  it "returns the position of the scan pointer" do
    @s.send(@method).should == 0
    @s.scan_until /This is/
    @s.send(@method).should == 7
    @s.get_byte
    @s.send(@method).should == 8
    @s.terminate
    @s.send(@method).should == 14
  end

  it "returns the position of the scan pointer for multibyte string" do
    @m.send(@method).should == 0
    @m.scan_until /cö/
    @m.send(@method).should == 3
    @m.get_byte
    @m.send(@method).should == 4
    @m.terminate
    @m.send(@method).should == 10
  end

  it "returns 0 in the reset position" do
    @s.reset
    @s.send(@method).should == 0
  end

  it "returns the length of the string in the terminate position" do
    @s.terminate
    @s.send(@method).should == @s.string.length
  end

  it "returns the `bytesize` for multibyte string in the terminate position" do
    @m.terminate
    @m.send(@method).should == @m.string.bytesize
    @m.send(@method).should >= @m.string.length
  end
end

describe :strscan_pos_set, :shared => true do
  before :each do
    @s = StringScanner.new("This is a test")
    @m = StringScanner.new("cölorfül")
  end

  it "modify the scan pointer" do
    @s.send(@method, 5)
    @s.rest.should == "is a test"
  end

  it "can poin position that greater than string length for multibyte string" do
    @m.send(@method, 9)
    @m.rest.should == "l"
  end

  it "positions from the end if the argument is negative" do
    @s.send(@method, -2)
    @s.rest.should == "st"
    @s.pos.should == 12
  end

  it "positions from the end if the argument is negative for multibyte string" do
    @m.send(@method, -3)
    @m.rest.should == "ül"
    @m.pos.should == 7
  end

  it "raises a RangeError if position too far backward" do
    lambda {
      @s.send(@method, -20)
    }.should raise_error(RangeError)
  end

  it "raises a RangeError when the passed argument is out of range" do
    lambda { @s.send(@method, 20) }.should raise_error(RangeError)
  end
end
