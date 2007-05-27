require File.dirname(__FILE__) + '/../spec_helper'

# tr_s, tr_s!, unpack, upcase, upcase!, upto

describe "Sprintf::Parser instance method" do

  before(:each) do
    @helper = Sprintf::Parser.new("foo wow!", [1,2,3])
  end
  
  it "initilize object" do
    @helper[].should == ?f
    @helper.cursor.should == 0
    @helper.mark.should == 0
    @helper.get_argument(0).should == 1
    @helper.get_argument(1).should == 2
    @helper.get_argument(2).should == 3
  end
  
  it "[] relative string character access" do
    @helper[].should == ?f
    @helper[1].should == ?o
    @helper.next
    @helper[].should == ?o
    @helper[3].should == ?w
  end
  
  it "cursor returns the absolute string position" do
    @helper.cursor.should == 0
    @helper.next
    @helper.cursor.should == 1
  end
  
  it "next increments the absolute string position" do
    @helper.cursor.should == 0
    @helper.next
    @helper.next
    @helper.cursor.should == 2
  end
  
  it "end_of_string? returns boolean indicating cursor position is off the end of the string" do
    while @helper.[] != ?!
      @helper.next
      @helper.end_of_string?.should == false
    end
    @helper.end_of_string?.should == false
    @helper.next
    @helper.end_of_string?.should == true
  end
  
  it "index returns the character position (or nil) of the searched character" do
    @helper.index(?w).should == 4
    @helper.cursor.should == 4
    @helper.rewind
    @helper.index(?&).should == nil
    @helper.cursor.should == 8
    @helper.end_of_string?.should == true
  end

  it "mark & drop_mark_point records the current cursor point" do
    @helper.cursor.should == 0
    @helper.mark.should == 0
    @helper.next
    @helper.next
    @helper.cursor.should == 2
    @helper.mark.should == 0
    @helper.drop_mark_point
    @helper.mark.should == 2
  end
  
  it "get_mark_to_cursor returns the marked string segment" do
    @helper.cursor.should == 0
    @helper.next
    @helper.drop_mark_point
    @helper.next
    @helper.get_mark_to_cursor.should == 'o'
  end
  
  it "get_argument returns an items from the argument list" do
    @helper.get_argument(0).should == 1
    @helper.get_argument(1).should == 2
    @helper.get_argument(2).should == 3
    should_raise(ArgumentError) { @helper.get_argument(4) }
  end
  
  it "get_next_argument returns the next item from the argument list" do
    @helper.get_next_argument.should == 1
    @helper.get_next_argument.should == 2
    @helper.get_next_argument.should == 3

    should_raise(ArgumentError) do
      @helper.get_next_argument
      @helper.get_next_argument
      @helper.get_next_argument
      @helper.get_next_argument
    end
  end
  
  it "get_number return a numerical value from a number string at the cursor position" do
    @helper = Sprintf::Parser.new("12345 %ABCD %789", 1)
    @helper.get_number.should == 12345
    @helper.cursor.should == 5
    @helper.index(?%)
    @helper.next
    @helper.get_number.should == 0
    @helper.cursor.should == 7
    @helper.index(?%)
    @helper.next
    @helper[].should == ?7
    begin
      @helper.get_number
    rescue ArgumentError 
      $!.message
    end.should == "malformed format string - %.[0-9]"
  end
  
  it "get_argument_value return an argument value from a *nn$ or * string" do
    @helper = Sprintf::Parser.new("*2$ %* %*789", [1, 2, 3])
    @helper.get_argument_value.should == 2
    @helper.index(?%)
    @helper.next
    @helper.get_argument_value.should == 2
    @helper.cursor.should == 6
    @helper.index(?%)
    @helper.next
    begin
      @helper.get_argument_value
    rescue ArgumentError
      $!.message
    end.should == "malformed format string - %.[0-9]"
  end
end

describe "Sprintf::Formatter instance method" do
  before(:each) do
    @formatter = Sprintf::Formatter.new
  end
  
  it "flags & flags= returns/sets the flag attribute" do
    @formatter.flags = 1
    @formatter.flags.should == 1
  end

  it "value & value= returns/sets the value attribute" do
    @formatter.value = 2
    @formatter.value.should == 2
    begin
      # value can only be set once
      @formatter.value = 2
      "No Error"
    rescue ArgumentError
      $!.message
    end.should == 'value given twice '
  end

  it "type & type= returns/sets the type attribute" do
    @formatter.type = 2
    @formatter.type.should == 2
    begin
      # type can only be set once
      @formatter.type = 2
      'No Error'
    rescue ArgumentError
      $!.message
    end.should == 'type given twice '
  end

  it "width & width= returns/sets the width attribute" do
    @formatter.width = 2
    @formatter.width.should == 2
    begin
      # width can only be set once
      @formatter.width = 2
      'No Error'
    rescue ArgumentError
      $!.message
    end.should == 'width given twice '
  end

  it "precision & precision= returns/sets the precision attribute" do
    @formatter.precision = 2
    @formatter.precision.should == 2
    begin
      # precision can only be set once
      @formatter.precision = 2
      'No Error'
    rescue ArgumentError
      $!.message
    end.should == 'precision given twice'
  end

  it "fill returns the string padded to width" do
    @formatter.width = 10
    @formatter.fill("hello").should == "     hello"
    @formatter.flags = Sprintf::Formatter::PAD_LJUSTIFY
    @formatter.fill("hello").should == "hello     "
    @formatter.fill("12345678901").should == "12345678901"
  end
  
  it "truncate returns the string truncated to precision" do
    @formatter.truncate("hello").should == "hello"
    @formatter.precision = 3
    @formatter.truncate("hello").should == "hel"
  end
  
  it "radix returns the radix appended to the string" do
    @formatter.type = ?b
    @formatter.radix("1").should == '1'
    @formatter.flags = Sprintf::Formatter::PAD_RADIX
    @formatter.radix("1").should == '0b1'
  end
  
  it "radix returns the uppercase binary radix appended to the string" do
    @formatter.type = ?B
    @formatter.flags = Sprintf::Formatter::PAD_RADIX
    @formatter.radix("1").should == '0B1'
  end
  
  it "radix returns the octal radix appended to the string" do
    @formatter.type = ?o
    @formatter.flags = Sprintf::Formatter::PAD_RADIX
    @formatter.radix("1").should == "01"
  end

  it "radix returns the hexadecimal radix appended to the string" do
    @formatter.type = ?x
    @formatter.flags = Sprintf::Formatter::PAD_RADIX
    @formatter.radix("1").should == "0x1"
  end
  
  it "radix returns the uppercase hexadecimal radix appended to the string" do
    @formatter.type = ?X
    @formatter.flags = Sprintf::Formatter::PAD_RADIX
    @formatter.radix("1").should == "0X1"
  end
  
  it "radix returns appends no radix for decimal and floating point" do
    @formatter.type = ?d
    @formatter.flags = Sprintf::Formatter::PAD_RADIX
    @formatter.radix("1").should == "1"
  end
  
  it "onespad returns string left padded with digit relevant to the number base" do
    @formatter.onespad("0", 2).should == '0'
    @formatter.flags = Sprintf::Formatter::PAD_ZERO
    @formatter.width = 10
    @formatter.onespad("0", 2).should == "1111111110"
    @formatter.precision = 5
    @formatter.onespad("0", 2).should == "11110"
  end
  
  it "onespad returns octal string left padded with max octal digit" do
    @formatter.precision = 5
    @formatter.onespad("0", 8).should == "77770"
  end
  
  it "onespad returns decimal string left padded with max decimal digit" do
    @formatter.precision = 5
    @formatter.onespad("0", 10).should == "....0"
  end
  
  it "onespad returns hex string left padded with max hex digit" do
    @formatter.precision = 5
    @formatter.onespad("0", 16).should == "ffff0"
  end
  
  it "zeropad returns str left padded with 0" do
     @formatter.zeropad("aaaa", 4).should == "aaaa"
    @formatter.zeropad("aaaa", 5).should == "0aaaa"
  end
  
  it "sign returns signed string" do
    @formatter.sign("1", 1).should == '1'
    @formatter.flags = Sprintf::Formatter::PAD_PLUS
    @formatter.sign("1", 1).should == '+1'
    @formatter.flags = Sprintf::Formatter::PAD_SPACE
    @formatter.sign("1", 1).should == ' 1'
    @formatter.sign("-1", -1).should == '-1'
  end
end
