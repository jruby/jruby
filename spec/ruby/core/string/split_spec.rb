# -*- encoding: utf-8 -*-
require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes.rb', __FILE__)

describe "String#split with String" do

  before :each do
    @kcode = $KCODE
  end

  after :each do
    $KCODE = @kcode
  end

  it "returns an array of substrings based on splitting on the given string" do
    "mellow yellow".split("ello").should == ["m", "w y", "w"]
  end

  it "suppresses trailing empty fields when limit isn't given or 0" do
    "1,2,,3,4,,".split(',').should == ["1", "2", "", "3", "4"]
    "1,2,,3,4,,".split(',', 0).should == ["1", "2", "", "3", "4"]
    "  a  b  c\nd  ".split("  ").should == ["", "a", "b", "c\nd"]
    "hai".split("hai").should == []
    ",".split(",").should == []
    ",".split(",", 0).should == []
  end

  it "returns an array with one entry if limit is 1: the original string" do
    "hai".split("hai", 1).should == ["hai"]
    "x.y.z".split(".", 1).should == ["x.y.z"]
    "hello world ".split(" ", 1).should == ["hello world "]
    "hi!".split("", 1).should == ["hi!"]
  end

  it "returns at most limit fields when limit > 1" do
    "hai".split("hai", 2).should == ["", ""]

    "1,2,,3,4,,".split(',', 2).should == ["1", "2,,3,4,,"]
    "1,2,,3,4,,".split(',', 3).should == ["1", "2", ",3,4,,"]
    "1,2,,3,4,,".split(',', 4).should == ["1", "2", "", "3,4,,"]
    "1,2,,3,4,,".split(',', 5).should == ["1", "2", "", "3", "4,,"]
    "1,2,,3,4,,".split(',', 6).should == ["1", "2", "", "3", "4", ","]

    "x".split('x', 2).should == ["", ""]
    "xx".split('x', 2).should == ["", "x"]
    "xx".split('x', 3).should == ["", "", ""]
    "xxx".split('x', 2).should == ["", "xx"]
    "xxx".split('x', 3).should == ["", "", "x"]
    "xxx".split('x', 4).should == ["", "", "", ""]
  end

  it "doesn't suppress or limit fields when limit is negative" do
    "1,2,,3,4,,".split(',', -1).should == ["1", "2", "", "3", "4", "", ""]
    "1,2,,3,4,,".split(',', -5).should == ["1", "2", "", "3", "4", "", ""]
    "  a  b  c\nd  ".split("  ", -1).should == ["", "a", "b", "c\nd", ""]
    ",".split(",", -1).should == ["", ""]
  end

  it "defaults to $; when string isn't given or nil" do
    begin
      old_fs = $;

      [",", ":", "", "XY", nil].each do |fs|
        $; = fs

        ["x,y,z,,,", "1:2:", "aXYbXYcXY", ""].each do |str|
          expected = str.split(fs || " ")

          str.split(nil).should == expected
          str.split.should == expected

          str.split(nil, -1).should == str.split(fs || " ", -1)
          str.split(nil, 0).should == str.split(fs || " ", 0)
          str.split(nil, 2).should == str.split(fs || " ", 2)
        end
      end
    ensure
      $; = old_fs
    end
  end

  it "ignores leading and continuous whitespace when string is a single space" do
    " now's  the time  ".split(' ').should == ["now's", "the", "time"]
    " now's  the time  ".split(' ', -1).should == ["now's", "the", "time", ""]
    " now's  the time  ".split(' ', 3).should == ["now's", "the", "time  "]

    "\t\n a\t\tb \n\r\r\nc\v\vd\v ".split(' ').should == ["a", "b", "c", "d"]
    "a\x00a b".split(' ').should == ["a\x00a", "b"]
  end

  it "splits between characters when its argument is an empty string" do
    "hi!".split("").should == ["h", "i", "!"]
    "hi!".split("", -1).should == ["h", "i", "!", ""]
    "hi!".split("", 2).should == ["h", "i!"]
  end

  it "tries converting its pattern argument to a string via to_str" do
    obj = mock('::')
    obj.should_receive(:to_str).and_return("::")

    "hello::world".split(obj).should == ["hello", "world"]
  end

  it "tries converting limit to an integer via to_int" do
    obj = mock('2')
    obj.should_receive(:to_int).and_return(2)

    "1.2.3.4".split(".", obj).should == ["1", "2.3.4"]
  end

  it "doesn't set $~" do
    $~ = nil
    "x.y.z".split(".")
    $~.should == nil
  end

  it "returns subclass instances based on self" do
    ["", "x.y.z.", "  x  y  "].each do |str|
      ["", ".", " "].each do |pat|
        [-1, 0, 1, 2].each do |limit|
          StringSpecs::MyString.new(str).split(pat, limit).each do |x|
            x.should be_kind_of(StringSpecs::MyString)
          end

          str.split(StringSpecs::MyString.new(pat), limit).each do |x|
            x.should be_kind_of(String)
          end
        end
      end
    end
  end

  it "does not call constructor on created subclass instances" do
    # can't call should_not_receive on an object that doesn't yet exist
    # so failure here is signalled by exception, not expectation failure

    s = StringSpecs::StringWithRaisingConstructor.new('silly:string')
    s.split(':').first.should == 'silly'
  end

  it "taints the resulting strings if self is tainted" do
    ["", "x.y.z.", "  x  y  "].each do |str|
      ["", ".", " "].each do |pat|
        [-1, 0, 1, 2].each do |limit|
          str.dup.taint.split(pat).each do |x|
            x.tainted?.should == true
          end

          str.split(pat.dup.taint).each do |x|
            x.tainted?.should == false
          end
        end
      end
    end
  end
end

describe "String#split with Regexp" do
  it "divides self on regexp matches" do
    " now's  the time".split(/ /).should == ["", "now's", "", "the", "time"]
    " x\ny ".split(/ /).should == ["", "x\ny"]
    "1, 2.34,56, 7".split(/,\s*/).should == ["1", "2.34", "56", "7"]
    "1x2X3".split(/x/i).should == ["1", "2", "3"]
  end

  it "treats negative limits as no limit" do
    "".split(%r!/+!, -1).should == []
  end

  it "suppresses trailing empty fields when limit isn't given or 0" do
    "1,2,,3,4,,".split(/,/).should == ["1", "2", "", "3", "4"]
    "1,2,,3,4,,".split(/,/, 0).should == ["1", "2", "", "3", "4"]
    "  a  b  c\nd  ".split(/\s+/).should == ["", "a", "b", "c", "d"]
    "hai".split(/hai/).should == []
    ",".split(/,/).should == []
    ",".split(/,/, 0).should == []
  end

  it "returns an array with one entry if limit is 1: the original string" do
    "hai".split(/hai/, 1).should == ["hai"]
    "xAyBzC".split(/[A-Z]/, 1).should == ["xAyBzC"]
    "hello world ".split(/\s+/, 1).should == ["hello world "]
    "hi!".split(//, 1).should == ["hi!"]
  end

  it "returns at most limit fields when limit > 1" do
    "hai".split(/hai/, 2).should == ["", ""]

    "1,2,,3,4,,".split(/,/, 2).should == ["1", "2,,3,4,,"]
    "1,2,,3,4,,".split(/,/, 3).should == ["1", "2", ",3,4,,"]
    "1,2,,3,4,,".split(/,/, 4).should == ["1", "2", "", "3,4,,"]
    "1,2,,3,4,,".split(/,/, 5).should == ["1", "2", "", "3", "4,,"]
    "1,2,,3,4,,".split(/,/, 6).should == ["1", "2", "", "3", "4", ","]

    "x".split(/x/, 2).should == ["", ""]
    "xx".split(/x/, 2).should == ["", "x"]
    "xx".split(/x/, 3).should == ["", "", ""]
    "xxx".split(/x/, 2).should == ["", "xx"]
    "xxx".split(/x/, 3).should == ["", "", "x"]
    "xxx".split(/x/, 4).should == ["", "", "", ""]
  end

  it "doesn't suppress or limit fields when limit is negative" do
    "1,2,,3,4,,".split(/,/, -1).should == ["1", "2", "", "3", "4", "", ""]
    "1,2,,3,4,,".split(/,/, -5).should == ["1", "2", "", "3", "4", "", ""]
    "  a  b  c\nd  ".split(/\s+/, -1).should == ["", "a", "b", "c", "d", ""]
    ",".split(/,/, -1).should == ["", ""]
  end

  it "defaults to $; when regexp isn't given or nil" do
    begin
      old_fs = $;

      [/,/, /:/, //, /XY/, /./].each do |fs|
        $; = fs

        ["x,y,z,,,", "1:2:", "aXYbXYcXY", ""].each do |str|
          expected = str.split(fs)

          str.split(nil).should == expected
          str.split.should == expected

          str.split(nil, -1).should == str.split(fs, -1)
          str.split(nil, 0).should == str.split(fs, 0)
          str.split(nil, 2).should == str.split(fs, 2)
        end
      end
    ensure
      $; = old_fs
    end
  end

  it "splits between characters when regexp matches a zero-length string" do
    "hello".split(//).should == ["h", "e", "l", "l", "o"]
    "hello".split(//, -1).should == ["h", "e", "l", "l", "o", ""]
    "hello".split(//, 2).should == ["h", "ello"]

    "hi mom".split(/\s*/).should == ["h", "i", "m", "o", "m"]

    "AABCCBAA".split(/(?=B)/).should == ["AA", "BCC", "BAA"]
    "AABCCBAA".split(/(?=B)/, -1).should == ["AA", "BCC", "BAA"]
    "AABCCBAA".split(/(?=B)/, 2).should == ["AA", "BCCBAA"]
  end

  it "respects $KCODE when splitting between characters" do
    str = "こにちわ"
    reg = %r!!

    $KCODE = "utf-8"
    ary = str.split(reg)
    ary.size.should == 4
    ary.should == ["こ", "に", "ち", "わ"]
  end

  ruby_version_is ""..."1.9" do
    it "uses $KCODE when splitting invalid characters" do
      str = [129, 0].pack('C*')

      $KCODE = "SJIS"
      ary = str.split(//)
      ary.size.should == 1
      ary.should == [str]
    end
  end

  it "respects the encoding of the regexp when splitting between characters" do
    str = "\303\202"

    $KCODE = "a"

    ary = str.split(//u)
    ary.size.should == 1
    ary.should == ["\303\202"]
  end

  it "includes all captures in the result array" do
    "hello".split(/(el)/).should == ["h", "el", "lo"]
    "hi!".split(/()/).should == ["h", "", "i", "", "!"]
    "hi!".split(/()/, -1).should == ["h", "", "i", "", "!", "", ""]
    "hello".split(/((el))()/).should == ["h", "el", "el", "", "lo"]
    "AabB".split(/([a-z])+/).should == ["A", "b", "B"]
  end

  it "does not include non-matching captures in the result array" do
    "hello".split(/(el)|(xx)/).should == ["h", "el", "lo"]
  end

  it "tries converting limit to an integer via to_int" do
    obj = mock('2')
    obj.should_receive(:to_int).and_return(2)

    "1.2.3.4".split(".", obj).should == ["1", "2.3.4"]
  end

  it "returns a type error if limit can't be converted to an integer" do
    lambda {"1.2.3.4".split(".", "three")}.should raise_error(TypeError)
    lambda {"1.2.3.4".split(".", nil)    }.should raise_error(TypeError)
  end

  it "doesn't set $~" do
    $~ = nil
    "x:y:z".split(/:/)
    $~.should == nil
  end

  it "returns the original string if no matches are found" do
    "foo".split("\n").should == ["foo"]
  end

  it "returns subclass instances based on self" do
    ["", "x:y:z:", "  x  y  "].each do |str|
      [//, /:/, /\s+/].each do |pat|
        [-1, 0, 1, 2].each do |limit|
          StringSpecs::MyString.new(str).split(pat, limit).each do |x|
            x.should be_kind_of(StringSpecs::MyString)
          end
        end
      end
    end
  end

  it "does not call constructor on created subclass instances" do
    # can't call should_not_receive on an object that doesn't yet exist
    # so failure here is signalled by exception, not expectation failure

    s = StringSpecs::StringWithRaisingConstructor.new('silly:string')
    s.split(/:/).first.should == 'silly'
  end

  it "taints the resulting strings if self is tainted" do
    ["", "x:y:z:", "  x  y  "].each do |str|
      [//, /:/, /\s+/].each do |pat|
        [-1, 0, 1, 2].each do |limit|
          str.dup.taint.split(pat, limit).each do |x|
            # See the spec below for why the conditional is here
            x.tainted?.should be_true unless x.empty?
          end
        end
      end
    end
  end

  # When split is called with a limit of -1, empty fields are not suppressed
  # and a final empty field is *alawys* created (who knows why). This empty
  # string is not tainted (again, who knows why) on 1.8 but is on 1.9.
  ruby_bug "#", "1.8" do
    it "taints an empty string if self is tainted" do
      ":".taint.split(//, -1).last.tainted?.should be_true
    end
  end

  it "doesn't taints the resulting strings if the Regexp is tainted" do
    ["", "x:y:z:", "  x  y  "].each do |str|
      [//, /:/, /\s+/].each do |pat|
        [-1, 0, 1, 2].each do |limit|
          str.split(pat.dup.taint, limit).each do |x|
            x.tainted?.should be_false
          end
        end
      end
    end
  end

  ruby_version_is "1.9" do
    it "retains the encoding of the source string" do
      ary = "а б в".split
      encodings = ary.map { |s| s.encoding }
      encodings.should == [Encoding::UTF_8, Encoding::UTF_8, Encoding::UTF_8]
    end

    it "returns an ArgumentError if an invalid UTF-8 string is supplied" do
      broken_str = 'проверка' # in russian, means "test"
      broken_str.force_encoding('binary')
      broken_str.chop!
      broken_str.force_encoding('utf-8')
      lambda{ broken_str.split(/\r\n|\r|\n/) }.should raise_error(ArgumentError)
    end
  end
end
