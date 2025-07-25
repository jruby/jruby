# encoding: binary
require_relative '../../spec_helper'
require_relative 'fixtures/classes'
require_relative 'fixtures/marshal_data'

describe "Marshal.dump" do
  it "dumps nil" do
    Marshal.dump(nil).should == "\004\b0"
  end

  it "dumps true" do
    Marshal.dump(true).should == "\004\bT"
  end

  it "dumps false" do
    Marshal.dump(false).should == "\004\bF"
  end

  describe "with a Fixnum" do
    it "dumps a Fixnum" do
      [ [Marshal,  0,       "\004\bi\000"],
        [Marshal,  5,       "\004\bi\n"],
        [Marshal,  8,       "\004\bi\r"],
        [Marshal,  122,     "\004\bi\177"],
        [Marshal,  123,     "\004\bi\001{"],
        [Marshal,  1234,    "\004\bi\002\322\004"],
        [Marshal, -8,       "\004\bi\363"],
        [Marshal, -123,     "\004\bi\200"],
        [Marshal, -124,     "\004\bi\377\204"],
        [Marshal, -1234,    "\004\bi\376.\373"],
        [Marshal, -4516727, "\004\bi\375\211\024\273"],
        [Marshal,  2**8,    "\004\bi\002\000\001"],
        [Marshal,  2**16,   "\004\bi\003\000\000\001"],
        [Marshal,  2**24,   "\004\bi\004\000\000\000\001"],
        [Marshal, -2**8,    "\004\bi\377\000"],
        [Marshal, -2**16,   "\004\bi\376\000\000"],
        [Marshal, -2**24,   "\004\bi\375\000\000\000"],
      ].should be_computed_by(:dump)
    end

    platform_is c_long_size: 64 do
      it "dumps a positive Fixnum > 31 bits as a Bignum" do
        Marshal.dump(2**31 + 1).should == "\x04\bl+\a\x01\x00\x00\x80"
      end

      it "dumps a negative Fixnum > 31 bits as a Bignum" do
        Marshal.dump(-2**31 - 1).should == "\x04\bl-\a\x01\x00\x00\x80"
      end
    end

    it "does not use object links for objects repeatedly dumped" do
      Marshal.dump([0, 0]).should == "\x04\b[\ai\x00i\x00"
      Marshal.dump([2**16, 2**16]).should == "\x04\b[\ai\x03\x00\x00\x01i\x03\x00\x00\x01"
    end
  end

  describe "with a Symbol" do
    it "dumps a Symbol" do
      Marshal.dump(:symbol).should == "\004\b:\vsymbol"
    end

    it "dumps a big Symbol" do
      Marshal.dump(('big' * 100).to_sym).should == "\004\b:\002,\001#{'big' * 100}"
    end

    it "dumps an encoded Symbol" do
      s = "\u2192"
      [ [Marshal, s.encode("utf-8").to_sym,
            "\x04\bI:\b\xE2\x86\x92\x06:\x06ET"],
        [Marshal, s.encode("utf-16").to_sym,
            "\x04\bI:\t\xFE\xFF!\x92\x06:\rencoding\"\vUTF-16"],
        [Marshal, s.encode("utf-16le").to_sym,
            "\x04\bI:\a\x92!\x06:\rencoding\"\rUTF-16LE"],
        [Marshal, s.encode("utf-16be").to_sym,
            "\x04\bI:\a!\x92\x06:\rencoding\"\rUTF-16BE"],
        [Marshal, s.encode("euc-jp").to_sym,
            "\x04\bI:\a\xA2\xAA\x06:\rencoding\"\vEUC-JP"],
        [Marshal, s.encode("sjis").to_sym,
            "\x04\bI:\a\x81\xA8\x06:\rencoding\"\x10Windows-31J"]
      ].should be_computed_by(:dump)
    end

    it "dumps a binary encoded Symbol" do
      s = "\u2192".dup.force_encoding("binary").to_sym
      Marshal.dump(s).should == "\x04\b:\b\xE2\x86\x92"
    end

    it "dumps multiple Symbols sharing the same encoding" do
      # Note that the encoding is a link for the second Symbol
      symbol1 = "I:\t\xE2\x82\xACa\x06:\x06ET"
      symbol2 = "I:\t\xE2\x82\xACb\x06;\x06T"
      value = [
        "€a".dup.force_encoding(Encoding::UTF_8).to_sym,
        "€b".dup.force_encoding(Encoding::UTF_8).to_sym
      ]
      Marshal.dump(value).should == "\x04\b[\a#{symbol1}#{symbol2}"

      value = [*value, value[0]]
      Marshal.dump(value).should == "\x04\b[\b#{symbol1}#{symbol2};\x00"
    end

    it "uses symbol links for objects repeatedly dumped" do
      symbol = :foo
      Marshal.dump([symbol, symbol]).should == "\x04\b[\a:\bfoo;\x00" # ;\x00 is a link to the symbol object
    end
  end

  describe "with an object responding to #marshal_dump" do
    it "dumps the object returned by #marshal_dump" do
      Marshal.dump(UserMarshal.new).should == "\x04\bU:\x10UserMarshal:\tdata"
    end

    it "does not use Class#name" do
      UserMarshal.should_not_receive(:name)
      Marshal.dump(UserMarshal.new)
    end

    it "raises TypeError if an Object is an instance of an anonymous class" do
      -> { Marshal.dump(Class.new(UserMarshal).new) }.should raise_error(TypeError, /can't dump anonymous class/)
    end

    it "uses object links for objects repeatedly dumped" do
      obj = UserMarshal.new
      Marshal.dump([obj, obj]).should == "\x04\b[\aU:\x10UserMarshal:\tdata@\x06" # @\x06 is a link to the object
    end

    it "adds instance variables of a dumped object after the object itself into the objects table" do
      value = "<foo>"
      obj = MarshalSpec::UserMarshalDumpWithIvar.new("string", value)

      # expect a link to the object (@\x06, that means Integer 1) is smaller than a link
      # to the instance variable value (@\t, that means Integer 4)
      Marshal.dump([obj, obj, value]).should == "\x04\b[\bU:)MarshalSpec::UserMarshalDumpWithIvarI[\x06\"\vstring\x06:\t@foo\"\n<foo>@\x06@\t"
    end
  end

  describe "with an object responding to #_dump" do
    it "dumps the String returned by #_dump" do
      Marshal.dump(UserDefined.new).should == "\004\bu:\020UserDefined\022\004\b[\a:\nstuff;\000"
    end

    it "dumps the String in non US-ASCII and non UTF-8 encoding" do
      object = UserDefinedString.new("a".encode("windows-1251"))
      Marshal.dump(object).should == "\x04\bIu:\x16UserDefinedString\x06a\x06:\rencoding\"\x11Windows-1251"
    end

    it "dumps the String in multibyte encoding" do
      object = UserDefinedString.new("a".encode("utf-32le"))
      Marshal.dump(object).should == "\x04\bIu:\x16UserDefinedString\ta\x00\x00\x00\x06:\rencoding\"\rUTF-32LE"
    end

    it "ignores overridden name method" do
      obj = MarshalSpec::UserDefinedWithOverriddenName.new
      Marshal.dump(obj).should == "\x04\bu:/MarshalSpec::UserDefinedWithOverriddenName\x12\x04\b[\a:\nstuff;\x00"
    end

    it "raises a TypeError if _dump returns a non-string" do
      m = mock("marshaled")
      m.should_receive(:_dump).and_return(0)
      -> { Marshal.dump(m) }.should raise_error(TypeError)
    end

    it "raises TypeError if an Object is an instance of an anonymous class" do
      -> { Marshal.dump(Class.new(UserDefined).new) }.should raise_error(TypeError, /can't dump anonymous class/)
    end

    it "favors marshal_dump over _dump" do
      m = mock("marshaled")
      m.should_receive(:marshal_dump).and_return(0)
      m.should_not_receive(:_dump)
      Marshal.dump(m)
    end

    it "indexes instance variables of a String returned by #_dump at first and then indexes the object itself" do
      class MarshalSpec::M1::A
        def _dump(level)
          s = +"<dump>"
          s.instance_variable_set(:@foo, "bar")
          s
        end
      end

      a = MarshalSpec::M1::A.new

      # 0-based index of the object a = 2, that is encoded as \x07 and printed as "\a" character.
      # Objects are serialized in the following order: Array, a, "bar".
      # But they are indexed in different order: Array (index=0), "bar" (index=1), a (index=2)
      # So the second occurenc of the object a is encoded as an index 2.
      reference = "@\a"
      Marshal.dump([a, a]).should == "\x04\b[\aIu:\x17MarshalSpec::M1::A\v<dump>\x06:\t@foo\"\bbar#{reference}"
    end

    it "uses object links for objects repeatedly dumped" do
      obj = UserDefined.new
      Marshal.dump([obj, obj]).should == "\x04\b[\au:\x10UserDefined\x12\x04\b[\a:\nstuff;\x00@\x06" # @\x06 is a link to the object
    end

    it "adds instance variables of a dumped String before the object itself into the objects table" do
      value = "<foo>"
      obj = MarshalSpec::UserDefinedDumpWithIVars.new(+"string", value)

      # expect a link to the object (@\a, that means Integer 2) is greater than a link
      # to the instance variable value (@\x06, that means Integer 1)
      Marshal.dump([obj, obj, value]).should == "\x04\b[\bIu:*MarshalSpec::UserDefinedDumpWithIVars\vstring\x06:\t@foo\"\n<foo>@\a@\x06"
    end

    describe "Core library classes with #_dump returning a String with instance variables" do
      it "indexes instance variables and then a Time object itself" do
        t = Time.utc(2022)
        reference = "@\a"

        Marshal.dump([t, t]).should == "\x04\b[\aIu:\tTime\r \x80\x1E\xC0\x00\x00\x00\x00\x06:\tzoneI\"\bUTC\x06:\x06EF#{reference}"
      end
    end
  end

  describe "with a Class" do
    it "dumps a builtin Class" do
      Marshal.dump(String).should == "\004\bc\vString"
    end

    it "dumps a user Class" do
      Marshal.dump(UserDefined).should == "\x04\bc\x10UserDefined"
    end

    it "dumps a nested Class" do
      Marshal.dump(UserDefined::Nested).should == "\004\bc\030UserDefined::Nested"
    end

    it "ignores overridden name method" do
      Marshal.dump(MarshalSpec::ClassWithOverriddenName).should == "\x04\bc)MarshalSpec::ClassWithOverriddenName"
    end

    ruby_version_is "3.5" do
      it "dumps a class with multibyte characters in name" do
        source_object = eval("MarshalSpec::MultibyteぁあぃいClass".dup.force_encoding(Encoding::UTF_8))
        Marshal.dump(source_object).should == "\x04\bIc,MarshalSpec::Multibyte\xE3\x81\x81\xE3\x81\x82\xE3\x81\x83\xE3\x81\x84Class\x06:\x06ET"
        Marshal.load(Marshal.dump(source_object)) == source_object
      end
    end

    it "uses object links for objects repeatedly dumped" do
      Marshal.dump([String, String]).should == "\x04\b[\ac\vString@\x06" # @\x06 is a link to the object
    end

    it "raises TypeError with an anonymous Class" do
      -> { Marshal.dump(Class.new) }.should raise_error(TypeError, /can't dump anonymous class/)
    end

    it "raises TypeError with a singleton Class" do
      -> { Marshal.dump(class << self; self end) }.should raise_error(TypeError)
    end
  end

  describe "with a Module" do
    it "dumps a builtin Module" do
      Marshal.dump(Marshal).should == "\004\bm\fMarshal"
    end

    it "ignores overridden name method" do
      Marshal.dump(MarshalSpec::ModuleWithOverriddenName).should == "\x04\bc*MarshalSpec::ModuleWithOverriddenName"
    end

    ruby_version_is "3.5" do
      it "dumps a module with multibyte characters in name" do
        source_object = eval("MarshalSpec::MultibyteけげこごModule".dup.force_encoding(Encoding::UTF_8))
        Marshal.dump(source_object).should == "\x04\bIm-MarshalSpec::Multibyte\xE3\x81\x91\xE3\x81\x92\xE3\x81\x93\xE3\x81\x94Module\x06:\x06ET"
        Marshal.load(Marshal.dump(source_object)) == source_object
      end
    end

    it "uses object links for objects repeatedly dumped" do
      Marshal.dump([Marshal, Marshal]).should == "\x04\b[\am\fMarshal@\x06" # @\x06 is a link to the object
    end

    it "raises TypeError with an anonymous Module" do
      -> { Marshal.dump(Module.new) }.should raise_error(TypeError, /can't dump anonymous module/)
    end
  end

  describe "with a Float" do
    it "dumps a Float" do
      [ [Marshal,  0.0,            "\004\bf\0060"],
        [Marshal, -0.0,            "\004\bf\a-0"],
        [Marshal,  1.0,            "\004\bf\0061"],
        [Marshal,  123.4567,       "\004\bf\r123.4567"],
        [Marshal, -0.841,          "\x04\bf\v-0.841"],
        [Marshal, -9876.345,       "\x04\bf\x0E-9876.345"],
        [Marshal,  infinity_value, "\004\bf\binf"],
        [Marshal, -infinity_value, "\004\bf\t-inf"],
        [Marshal,  nan_value,      "\004\bf\bnan"],
      ].should be_computed_by(:dump)
    end

    it "may or may not use object links for objects repeatedly dumped" do
      # it's an MRI implementation detail - on x86 architecture object links
      # aren't used for Float values but on amd64 - object links are used

      dump = Marshal.dump([0.0, 0.0])
      ["\x04\b[\af\x060@\x06", "\x04\b[\af\x060f\x060"].should.include?(dump)

      # if object links aren't used - entries in the objects table are still
      # occupied by Float values
      if dump == "\x04\b[\af\x060f\x060"
        s = "string"
        # an index of "string" ("@\b") in the object table equals 3 (`"\b".ord - 5`),
        # so `0.0, 0,0` elements occupied indices 1 and 2
        Marshal.dump([0.0, 0.0, s, s]).should == "\x04\b[\tf\x060f\x060\"\vstring@\b"
      end
    end
  end

  describe "with a Bignum" do
    it "dumps a Bignum" do
      [ [Marshal, -4611686018427387903,    "\004\bl-\t\377\377\377\377\377\377\377?"],
        [Marshal, -2361183241434822606847, "\004\bl-\n\377\377\377\377\377\377\377\377\177\000"],
      ].should be_computed_by(:dump)
    end

    it "dumps a Bignum" do
      [ [Marshal,  2**64, "\004\bl+\n\000\000\000\000\000\000\000\000\001\000"],
        [Marshal,  2**90, "\004\bl+\v#{"\000" * 11}\004"],
        [Marshal, -2**63, "\004\bl-\t\000\000\000\000\000\000\000\200"],
        [Marshal, -2**64, "\004\bl-\n\000\000\000\000\000\000\000\000\001\000"],
      ].should be_computed_by(:dump)
    end

    it "uses object links for objects repeatedly dumped" do
      n = 2**64
      Marshal.dump([n, n]).should == "\x04\b[\al+\n\x00\x00\x00\x00\x00\x00\x00\x00\x01\x00@\x06" # @\x06 is a link to the object
    end

    it "increases the object links counter" do
      obj = Object.new
      object_1_link = "\x06" # representing of (0-based) index=1 (by adding 5 for small Integers)
      object_2_link = "\x07" # representing of index=2

      # objects: Array, Object, Object
      Marshal.dump([obj, obj]).should == "\x04\b[\ao:\vObject\x00@#{object_1_link}"

      # objects: Array, Bignum, Object, Object
      Marshal.dump([2**64, obj, obj]).should == "\x04\b[\bl+\n\x00\x00\x00\x00\x00\x00\x00\x00\x01\x00o:\vObject\x00@#{object_2_link}"
      Marshal.dump([2**48, obj, obj]).should == "\x04\b[\bl+\t\x00\x00\x00\x00\x00\x00\x01\x00o:\vObject\x00@#{object_2_link}"
      Marshal.dump([2**32, obj, obj]).should == "\x04\b[\bl+\b\x00\x00\x00\x00\x01\x00o:\vObject\x00@#{object_2_link}"
    end
  end

  describe "with a Rational" do
    it "dumps a Rational" do
      Marshal.dump(Rational(2, 3)).should == "\x04\bU:\rRational[\ai\ai\b"
    end

    it "uses object links for objects repeatedly dumped" do
      r = Rational(2, 3)
      Marshal.dump([r, r]).should == "\x04\b[\aU:\rRational[\ai\ai\b@\x06" # @\x06 is a link to the object
    end
  end

  describe "with a Complex" do
    it "dumps a Complex" do
      Marshal.dump(Complex(2, 3)).should == "\x04\bU:\fComplex[\ai\ai\b"
    end

    it "uses object links for objects repeatedly dumped" do
      c = Complex(2, 3)
      Marshal.dump([c, c]).should == "\x04\b[\aU:\fComplex[\ai\ai\b@\x06" # @\x06 is a link to the object
    end
  end

  describe "with a Data" do
    it "dumps a Data" do
      Marshal.dump(MarshalSpec::DataSpec::Measure.new(100, 'km')).should == "\x04\bS:#MarshalSpec::DataSpec::Measure\a:\vamountii:\tunit\"\akm"
    end

    it "dumps an extended Data" do
      obj = MarshalSpec::DataSpec::MeasureExtended.new(100, "km")
      Marshal.dump(obj).should == "\x04\bS:+MarshalSpec::DataSpec::MeasureExtended\a:\vamountii:\tunit\"\akm"
    end

    it "ignores overridden name method" do
      obj = MarshalSpec::DataSpec::MeasureWithOverriddenName.new(100, "km")
      Marshal.dump(obj).should == "\x04\bS:5MarshalSpec::DataSpec::MeasureWithOverriddenName\a:\vamountii:\tunit\"\akm"
    end

    it "uses object links for objects repeatedly dumped" do
      d = MarshalSpec::DataSpec::Measure.new(100, 'km')
      Marshal.dump([d, d]).should == "\x04\b[\aS:#MarshalSpec::DataSpec::Measure\a:\vamountii:\tunit\"\akm@\x06" # @\x06 is a link to the object
    end

    it "raises TypeError with an anonymous Struct" do
      -> { Marshal.dump(Data.define(:a).new(1)) }.should raise_error(TypeError, /can't dump anonymous class/)
    end
  end

  describe "with a String" do
    it "dumps a blank String" do
      Marshal.dump("".dup.force_encoding("binary")).should == "\004\b\"\000"
    end

    it "dumps a short String" do
      Marshal.dump("short".dup.force_encoding("binary")).should == "\004\b\"\012short"
    end

    it "dumps a long String" do
      Marshal.dump(("big" * 100).force_encoding("binary")).should == "\004\b\"\002,\001#{"big" * 100}"
    end

    it "dumps a String extended with a Module" do
      Marshal.dump("".dup.extend(Meths).force_encoding("binary")).should == "\004\be:\nMeths\"\000"
    end

    it "dumps a String subclass" do
      Marshal.dump(UserString.new.force_encoding("binary")).should == "\004\bC:\017UserString\"\000"
    end

    it "dumps a String subclass extended with a Module" do
      Marshal.dump(UserString.new.extend(Meths).force_encoding("binary")).should == "\004\be:\nMethsC:\017UserString\"\000"
    end

    it "ignores overridden name method when dumps a String subclass" do
      obj = MarshalSpec::StringWithOverriddenName.new
      Marshal.dump(obj).should == "\x04\bC:*MarshalSpec::StringWithOverriddenName\"\x00"
    end

    it "dumps a String with instance variables" do
      str = +""
      str.instance_variable_set("@foo", "bar")
      Marshal.dump(str.force_encoding("binary")).should == "\x04\bI\"\x00\x06:\t@foo\"\bbar"
    end

    it "dumps a US-ASCII String" do
      str = "abc".dup.force_encoding("us-ascii")
      Marshal.dump(str).should == "\x04\bI\"\babc\x06:\x06EF"
    end

    it "dumps a UTF-8 String" do
      str = "\x6d\xc3\xb6\x68\x72\x65".dup.force_encoding("utf-8")
      Marshal.dump(str).should == "\x04\bI\"\vm\xC3\xB6hre\x06:\x06ET"
    end

    it "dumps a String in another encoding" do
      str = "\x6d\x00\xf6\x00\x68\x00\x72\x00\x65\x00".dup.force_encoding("utf-16le")
      result = "\x04\bI\"\x0Fm\x00\xF6\x00h\x00r\x00e\x00\x06:\rencoding\"\rUTF-16LE"
      Marshal.dump(str).should == result
    end

    it "dumps multiple strings using symlinks for the :E (encoding) symbol" do
      Marshal.dump(["".encode("us-ascii"), "".encode("utf-8")]).should == "\x04\b[\aI\"\x00\x06:\x06EFI\"\x00\x06;\x00T"
    end

    it "uses object links for objects repeatedly dumped" do
      s = "string"
      Marshal.dump([s, s]).should == "\x04\b[\a\"\vstring@\x06" # @\x06 is a link to the object
    end

    it "adds instance variables after the object itself into the objects table" do
      obj = +"string"
      value = "<foo>"
      obj.instance_variable_set :@foo, value

      # expect a link to the object (@\x06, that means Integer 1) is smaller than a link
      # to the instance variable value (@\a, that means Integer 2)
      Marshal.dump([obj, obj, value]).should == "\x04\b[\bI\"\vstring\x06:\t@foo\"\n<foo>@\x06@\a"
    end
  end

  describe "with a Regexp" do
    it "dumps a Regexp" do
      Marshal.dump(/\A.\Z/).should == "\x04\bI/\n\\A.\\Z\x00\x06:\x06EF"
    end

    it "dumps a Regexp with flags" do
      Marshal.dump(//im).should == "\x04\bI/\x00\x05\x06:\x06EF"
    end

    it "dumps a Regexp with instance variables" do
      o = Regexp.new("")
      o.instance_variable_set(:@ivar, :ivar)
      Marshal.dump(o).should == "\x04\bI/\x00\x00\a:\x06EF:\n@ivar:\tivar"
    end

    it "dumps an extended Regexp" do
      Marshal.dump(Regexp.new("").extend(Meths)).should == "\x04\bIe:\nMeths/\x00\x00\x06:\x06EF"
    end

    it "dumps a Regexp subclass" do
      Marshal.dump(UserRegexp.new("")).should == "\x04\bIC:\x0FUserRegexp/\x00\x00\x06:\x06EF"
    end

    it "dumps a binary Regexp" do
      o = Regexp.new("".dup.force_encoding("binary"), Regexp::FIXEDENCODING)
      Marshal.dump(o).should == "\x04\b/\x00\x10"
    end

    it "dumps an ascii-compatible Regexp" do
      o = Regexp.new("a".encode("us-ascii"), Regexp::FIXEDENCODING)
      Marshal.dump(o).should == "\x04\bI/\x06a\x10\x06:\x06EF"

      o = Regexp.new("a".encode("us-ascii"))
      Marshal.dump(o).should == "\x04\bI/\x06a\x00\x06:\x06EF"

      o = Regexp.new("a".encode("windows-1251"), Regexp::FIXEDENCODING)
      Marshal.dump(o).should == "\x04\bI/\x06a\x10\x06:\rencoding\"\x11Windows-1251"

      o = Regexp.new("a".encode("windows-1251"))
      Marshal.dump(o).should == "\x04\bI/\x06a\x00\x06:\x06EF"
    end

    it "dumps a UTF-8 Regexp" do
      o = Regexp.new("".dup.force_encoding("utf-8"), Regexp::FIXEDENCODING)
      Marshal.dump(o).should == "\x04\bI/\x00\x10\x06:\x06ET"

      o = Regexp.new("a".dup.force_encoding("utf-8"), Regexp::FIXEDENCODING)
      Marshal.dump(o).should == "\x04\bI/\x06a\x10\x06:\x06ET"

      o = Regexp.new("\u3042".dup.force_encoding("utf-8"), Regexp::FIXEDENCODING)
      Marshal.dump(o).should == "\x04\bI/\b\xE3\x81\x82\x10\x06:\x06ET"
    end

    it "dumps a Regexp in another encoding" do
      o = Regexp.new("".dup.force_encoding("utf-16le"), Regexp::FIXEDENCODING)
      Marshal.dump(o).should == "\x04\bI/\x00\x10\x06:\rencoding\"\rUTF-16LE"

      o = Regexp.new("a".encode("utf-16le"), Regexp::FIXEDENCODING)
      Marshal.dump(o).should == "\x04\bI/\aa\x00\x10\x06:\rencoding\"\rUTF-16LE"
    end

    it "ignores overridden name method when dumps a Regexp subclass" do
      obj = MarshalSpec::RegexpWithOverriddenName.new("")
      Marshal.dump(obj).should == "\x04\bIC:*MarshalSpec::RegexpWithOverriddenName/\x00\x00\x06:\x06EF"
    end

    it "uses object links for objects repeatedly dumped" do
      r = /\A.\Z/
      Marshal.dump([r, r]).should == "\x04\b[\aI/\n\\A.\\Z\x00\x06:\x06EF@\x06" # @\x06 is a link to the object
    end
  end

  describe "with an Array" do
    it "dumps an empty Array" do
      Marshal.dump([]).should == "\004\b[\000"
    end

    it "dumps a non-empty Array" do
      Marshal.dump([:a, 1, 2]).should == "\004\b[\b:\006ai\006i\a"
    end

    it "dumps an Array subclass" do
      Marshal.dump(UserArray.new).should == "\004\bC:\016UserArray[\000"
    end

    it "dumps a recursive Array" do
      a = []
      a << a
      Marshal.dump(a).should == "\x04\b[\x06@\x00"
    end

    it "dumps an Array with instance variables" do
      a = []
      a.instance_variable_set(:@ivar, 1)
      Marshal.dump(a).should == "\004\bI[\000\006:\n@ivari\006"
    end

    it "dumps an extended Array" do
      Marshal.dump([].extend(Meths)).should == "\004\be:\nMeths[\000"
    end

    it "ignores overridden name method when dumps an Array subclass" do
      obj = MarshalSpec::ArrayWithOverriddenName.new
      Marshal.dump(obj).should == "\x04\bC:)MarshalSpec::ArrayWithOverriddenName[\x00"
    end

    it "uses object links for objects repeatedly dumped" do
      a = [1]
      Marshal.dump([a, a]).should == "\x04\b[\a[\x06i\x06@\x06" # @\x06 is a link to the object
    end

    it "adds instance variables after the object itself into the objects table" do
      obj = []
      value = "<foo>"
      obj.instance_variable_set :@foo, value

      # expect a link to the object (@\x06, that means Integer 1) is smaller than a link
      # to the instance variable value (@\a, that means Integer 2)
      Marshal.dump([obj, obj, value]).should == "\x04\b[\bI[\x00\x06:\t@foo\"\n<foo>@\x06@\a"
    end
  end

  describe "with a Hash" do
    it "dumps a Hash" do
      Marshal.dump({}).should == "\004\b{\000"
    end

    it "dumps a non-empty Hash" do
      Marshal.dump({a: 1}).should == "\x04\b{\x06:\x06ai\x06"
    end

    it "dumps a Hash subclass" do
      Marshal.dump(UserHash.new).should == "\004\bC:\rUserHash{\000"
    end

    it "dumps a Hash with a default value" do
      Marshal.dump(Hash.new(1)).should == "\004\b}\000i\006"
    end

    it "dumps a Hash with compare_by_identity" do
      h = {}
      h.compare_by_identity

      Marshal.dump(h).should == "\004\bC:\tHash{\x00"
    end

    it "dumps a Hash subclass with compare_by_identity" do
      h = UserHash.new
      h.compare_by_identity

      Marshal.dump(h).should == "\x04\bC:\rUserHashC:\tHash{\x00"
    end

    it "raises a TypeError with hash having default proc" do
      -> { Marshal.dump(Hash.new {}) }.should raise_error(TypeError, "can't dump hash with default proc")
    end

    it "dumps a Hash with instance variables" do
      a = {}
      a.instance_variable_set(:@ivar, 1)
      Marshal.dump(a).should == "\004\bI{\000\006:\n@ivari\006"
    end

    it "dumps an extended Hash" do
      Marshal.dump({}.extend(Meths)).should == "\004\be:\nMeths{\000"
    end

    it "dumps an Hash subclass with a parameter to initialize" do
      Marshal.dump(UserHashInitParams.new(1)).should == "\004\bIC:\027UserHashInitParams{\000\006:\a@ai\006"
    end

    it "ignores overridden name method when dumps a Hash subclass" do
      obj = MarshalSpec::HashWithOverriddenName.new
      Marshal.dump(obj).should == "\x04\bC:(MarshalSpec::HashWithOverriddenName{\x00"
    end

    it "uses object links for objects repeatedly dumped" do
      h = {a: 1}
      Marshal.dump([h, h]).should == "\x04\b[\a{\x06:\x06ai\x06@\x06" # @\x06 is a link to the object
    end

    it "adds instance variables after the object itself into the objects table" do
      obj = {}
      value = "<foo>"
      obj.instance_variable_set :@foo, value

      # expect a link to the object (@\x06, that means Integer 1) is smaller than a link
      # to the instance variable value (@\a, that means Integer 2)
      Marshal.dump([obj, obj, value]).should == "\x04\b[\bI{\x00\x06:\t@foo\"\n<foo>@\x06@\a"
    end

    it "dumps a ruby2_keywords_hash Hash instance" do
      kw_hash = Hash.ruby2_keywords_hash({})

      Marshal.dump(kw_hash).should == "\x04\bI{\x00\x06:\x06KT"
    end
  end

  describe "with a Struct" do
    it "dumps a Struct" do
      Marshal.dump(Struct::Pyramid.new).should == "\004\bS:\024Struct::Pyramid\000"
    end

    it "dumps a Struct" do
      Marshal.dump(Struct::Useful.new(1, 2)).should == "\004\bS:\023Struct::Useful\a:\006ai\006:\006bi\a"
    end

    it "dumps a Struct with instance variables" do
      st = Struct.new("Thick").new
      st.instance_variable_set(:@ivar, 1)
      Marshal.dump(st).should == "\004\bIS:\022Struct::Thick\000\006:\n@ivari\006"
      Struct.send(:remove_const, :Thick)
    end

    it "dumps an extended Struct" do
      obj = Struct.new("Extended", :a, :b).new.extend(Meths)
      Marshal.dump(obj).should == "\004\be:\nMethsS:\025Struct::Extended\a:\006a0:\006b0"

      s = 'hi'
      obj.a = [:a, s]
      obj.b = [:Meths, s]
      Marshal.dump(obj).should == "\004\be:\nMethsS:\025Struct::Extended\a:\006a[\a;\a\"\ahi:\006b[\a;\000@\a"
      Struct.send(:remove_const, :Extended)
    end

    it "ignores overridden name method" do
      obj = MarshalSpec::StructWithOverriddenName.new("member")
      Marshal.dump(obj).should == "\x04\bS:*MarshalSpec::StructWithOverriddenName\x06:\x06a\"\vmember"
    end

    it "uses object links for objects repeatedly dumped" do
      s = Struct::Pyramid.new
      Marshal.dump([s, s]).should == "\x04\b[\aS:\x14Struct::Pyramid\x00@\x06" # @\x06 is a link to the object
    end

    it "raises TypeError with an anonymous Struct" do
      -> { Marshal.dump(Struct.new(:a).new(1)) }.should raise_error(TypeError, /can't dump anonymous class/)
    end

    it "adds instance variables after the object itself into the objects table" do
      obj = Struct::Pyramid.new
      value = "<foo>"
      obj.instance_variable_set :@foo, value

      # expect a link to the object (@\x06, that means Integer 1) is smaller than a link
      # to the instance variable value (@\a, that means Integer 2)
      Marshal.dump([obj, obj, value]).should == "\x04\b[\bIS:\x14Struct::Pyramid\x00\x06:\t@foo\"\n<foo>@\x06@\a"
    end
  end

  describe "with an Object" do
    it "dumps an Object" do
      Marshal.dump(Object.new).should == "\004\bo:\x0BObject\x00"
    end

    it "dumps an extended Object" do
      Marshal.dump(Object.new.extend(Meths)).should == "\004\be:\x0AMethso:\x0BObject\x00"
    end

    it "dumps an Object with an instance variable" do
      obj = Object.new
      obj.instance_variable_set(:@ivar, 1)
      Marshal.dump(obj).should == "\004\bo:\vObject\006:\n@ivari\006"
    end

    it "dumps an Object with a non-US-ASCII instance variable" do
      obj = Object.new
      ivar = "@é".dup.force_encoding(Encoding::UTF_8).to_sym
      obj.instance_variable_set(ivar, 1)
      Marshal.dump(obj).should == "\x04\bo:\vObject\x06I:\b@\xC3\xA9\x06:\x06ETi\x06"
    end

    it "dumps an Object that has had an instance variable added and removed as though it was never set" do
      obj = Object.new
      obj.instance_variable_set(:@ivar, 1)
      obj.send(:remove_instance_variable, :@ivar)
      Marshal.dump(obj).should == "\004\bo:\x0BObject\x00"
    end

    it "dumps an Object if it has a singleton class but no singleton methods and no singleton instance variables" do
      obj = Object.new
      obj.singleton_class
      Marshal.dump(obj).should == "\004\bo:\x0BObject\x00"
    end

    it "ignores overridden name method" do
      obj = MarshalSpec::ClassWithOverriddenName.new
      Marshal.dump(obj).should == "\x04\bo:)MarshalSpec::ClassWithOverriddenName\x00"
    end

    it "raises TypeError if an Object has a singleton class and singleton methods" do
      obj = Object.new
      def obj.foo; end
      -> {
        Marshal.dump(obj)
      }.should raise_error(TypeError, "singleton can't be dumped")
    end

    it "raises TypeError if an Object has a singleton class and singleton instance variables" do
      obj = Object.new
      class << obj
        @v = 1
      end

      -> {
        Marshal.dump(obj)
      }.should raise_error(TypeError, "singleton can't be dumped")
    end

    it "raises TypeError if an Object is an instance of an anonymous class" do
      anonymous_class = Class.new
      obj = anonymous_class.new

      -> { Marshal.dump(obj) }.should raise_error(TypeError, /can't dump anonymous class/)
    end

    it "raises TypeError if an Object extends an anonymous module" do
      anonymous_module = Module.new
      obj = Object.new
      obj.extend(anonymous_module)

      -> { Marshal.dump(obj) }.should raise_error(TypeError, /can't dump anonymous class/)
    end

    it "dumps a BasicObject subclass if it defines respond_to?" do
      obj = MarshalSpec::BasicObjectSubWithRespondToFalse.new
      Marshal.dump(obj).should == "\x04\bo:2MarshalSpec::BasicObjectSubWithRespondToFalse\x00"
    end

    it "dumps without marshaling any attached finalizer" do
      obj = Object.new
      finalizer = Object.new
      def finalizer.noop(_)
      end
      ObjectSpace.define_finalizer(obj, finalizer.method(:noop))
      Marshal.load(Marshal.dump(obj)).class.should == Object
    end

    it "uses object links for objects repeatedly dumped" do
      obj = Object.new
      Marshal.dump([obj, obj]).should == "\x04\b[\ao:\vObject\x00@\x06" # @\x06 is a link to the object
    end

    it "adds instance variables after the object itself into the objects table" do
      obj = Object.new
      value = "<foo>"
      obj.instance_variable_set :@foo, value

      # expect a link to the object (@\x06, that means Integer 1) is smaller than a link
      # to the instance variable value (@\a, that means Integer 2)
      Marshal.dump([obj, obj, value]).should == "\x04\b[\bo:\vObject\x06:\t@foo\"\n<foo>@\x06@\a"
    end
  end

  describe "with a Range" do
    it "dumps a Range inclusive of end" do
      dump = Marshal.dump(1..2)
      dump.should == "\x04\bo:\nRange\b:\texclF:\nbegini\x06:\bendi\a"

      load = Marshal.load(dump)
      load.should == (1..2)
    end

    it "dumps a Range exclusive of end" do
      dump = Marshal.dump(1...2)
      dump.should == "\x04\bo:\nRange\b:\texclT:\nbegini\x06:\bendi\a"

      load = Marshal.load(dump)
      load.should == (1...2)
    end

    it "uses object links for objects repeatedly dumped" do
      r = 1..2
      Marshal.dump([r, r]).should == "\x04\b[\ao:\nRange\b:\texclF:\nbegini\x06:\bendi\a@\x06" # @\x06 is a link to the object
    end

    it "raises TypeError with an anonymous Range subclass" do
      -> { Marshal.dump(Class.new(Range).new(1, 2)) }.should raise_error(TypeError, /can't dump anonymous class/)
    end
  end

  describe "with a Time" do
    before :each do
      @internal = Encoding.default_internal
      Encoding.default_internal = Encoding::UTF_8

      @utc = Time.utc(2012, 1, 1)
      @utc_dump = @utc.send(:_dump)

      with_timezone 'AST', 3 do
        @t = Time.local(2012, 1, 1)
        @fract = Time.local(2012, 1, 1, 1, 59, 56.2)
        @t_dump = @t.send(:_dump)
        @fract_dump = @fract.send(:_dump)
      end
    end

    after :each do
      Encoding.default_internal = @internal
    end

    it "dumps the zone and the offset" do
      with_timezone 'AST', 3 do
        dump = Marshal.dump(@t)
        base = "\x04\bIu:\tTime\r#{@t_dump}\a"
        offset = ":\voffseti\x020*"
        zone = ":\tzoneI\"\bAST\x06:\x06EF" # Last is 'F' (US-ASCII)
        [ "#{base}#{offset}#{zone}", "#{base}#{zone}#{offset}" ].should include(dump)
      end
    end

    it "dumps the zone, but not the offset if zone is UTC" do
      dump = Marshal.dump(@utc)
      zone = ":\tzoneI\"\bUTC\x06:\x06EF" # Last is 'F' (US-ASCII)
      dump.should == "\x04\bIu:\tTime\r#{@utc_dump}\x06#{zone}"
    end

    it "ignores overridden name method" do
      obj = MarshalSpec::TimeWithOverriddenName.new
      Marshal.dump(obj).should include("MarshalSpec::TimeWithOverriddenName")
    end

    ruby_version_is "3.5" do
      it "dumps a Time subclass with multibyte characters in name" do
        source_object = eval("MarshalSpec::MultibyteぁあぃいTime".dup.force_encoding(Encoding::UTF_8))
        Marshal.dump(source_object).should == "\x04\bIc+MarshalSpec::Multibyte\xE3\x81\x81\xE3\x81\x82\xE3\x81\x83\xE3\x81\x84Time\x06:\x06ET"
        Marshal.load(Marshal.dump(source_object)) == source_object
      end
    end

    it "uses object links for objects repeatedly dumped" do
      # order of the offset and zone instance variables is a subject to change
      # and may be different on different CRuby versions
      base = Regexp.quote("\x04\b[\aIu:\tTime\r\xF5\xEF\e\x80\x00\x00\x00\x00\a")
      offset = Regexp.quote(":\voffseti\x020*:\tzoneI\"\bAST\x06:\x06EF")
      zone = Regexp.quote(":\tzoneI\"\bAST\x06:\x06EF:\voffseti\x020*")
      instance_variables = /#{offset}|#{zone}/
      Marshal.dump([@t, @t]).should =~ /\A#{base}#{instance_variables}@\a\Z/ # @\a is a link to the object
    end

    it "adds instance variables before the object itself into the objects table" do
      obj = @utc
      value = "<foo>"
      obj.instance_variable_set :@foo, value

      # expect a link to the object (@\b, that means Integer 3) is greater than a link
      # to the instance variable value (@\x06, that means Integer 1)
      Marshal.dump([obj, obj, value]).should == "\x04\b[\bIu:\tTime\r \x00\x1C\xC0\x00\x00\x00\x00\a:\t@foo\"\n<foo>:\tzoneI\"\bUTC\x06:\x06EF@\b@\x06"
    end

    it "raises TypeError with an anonymous Time subclass" do
      -> { Marshal.dump(Class.new(Time).now) }.should raise_error(TypeError)
    end
  end

  describe "with an Exception" do
    it "dumps an empty Exception" do
      Marshal.dump(Exception.new).should == "\x04\bo:\x0EException\a:\tmesg0:\abt0"
    end

    it "dumps the message for the exception" do
      Marshal.dump(Exception.new("foo")).should == "\x04\bo:\x0EException\a:\tmesg\"\bfoo:\abt0"
    end

    it "contains the filename in the backtrace" do
      obj = Exception.new("foo")
      obj.set_backtrace(["foo/bar.rb:10"])
      Marshal.dump(obj).should == "\x04\bo:\x0EException\a:\tmesg\"\bfoo:\abt[\x06\"\x12foo/bar.rb:10"
    end

    it "dumps instance variables if they exist" do
      obj = Exception.new("foo")
      obj.instance_variable_set(:@ivar, 1)
      Marshal.dump(obj).should == "\x04\bo:\x0EException\b:\tmesg\"\bfoo:\abt0:\n@ivari\x06"
    end

    it "dumps the cause for the exception" do
      exc = nil
      begin
        raise StandardError, "the cause"
      rescue StandardError => cause
        begin
          raise RuntimeError, "the consequence"
        rescue RuntimeError => e
          e.cause.should equal(cause)
          exc = e
        end
      end

      reloaded = Marshal.load(Marshal.dump(exc))
      reloaded.cause.should be_an_instance_of(StandardError)
      reloaded.cause.message.should == "the cause"
    end

    # NoMethodError uses an exception formatter on TruffleRuby and computes a message lazily
    it "dumps the message for the raised NoMethodError exception" do
      begin
        "".foo
      rescue => e
      end

      Marshal.dump(e).should =~ /undefined method [`']foo' for ("":String|an instance of String)/
    end

    it "uses object links for objects repeatedly dumped" do
      e = Exception.new
      Marshal.dump([e, e]).should == "\x04\b[\ao:\x0EException\a:\tmesg0:\abt0@\x06" # @\x\a is a link to the object
    end

    it "adds instance variables after the object itself into the objects table" do
      obj = Exception.new
      value = "<foo>"
      obj.instance_variable_set :@foo, value

      # expect a link to the object (@\x06, that means Integer 1) is smaller than a link
      # to the instance variable value (@\a, that means Integer 2)
      Marshal.dump([obj, obj, value]).should == "\x04\b[\bo:\x0EException\b:\tmesg0:\abt0:\t@foo\"\n<foo>@\x06@\a"
    end

    it "raises TypeError if an Object is an instance of an anonymous class" do
      anonymous_class = Class.new(Exception)
      obj = anonymous_class.new

      -> { Marshal.dump(obj) }.should raise_error(TypeError, /can't dump anonymous class/)
    end
  end

  it "dumps subsequent appearances of a symbol as a link" do
    Marshal.dump([:a, :a]).should == "\004\b[\a:\006a;\000"
  end

  it "dumps subsequent appearances of an object as a link" do
    o = Object.new
    Marshal.dump([o, o]).should == "\004\b[\ao:\vObject\000@\006"
  end

  MarshalSpec::DATA_19.each do |description, (object, marshal, attributes)|
    it "#{description} returns a binary string" do
      Marshal.dump(object).encoding.should == Encoding::BINARY
    end
  end

  it "raises an ArgumentError when the recursion limit is exceeded" do
    h = {'one' => {'two' => {'three' => 0}}}
    -> { Marshal.dump(h, 3) }.should raise_error(ArgumentError)
    -> { Marshal.dump([h], 4) }.should raise_error(ArgumentError)
    -> { Marshal.dump([], 0) }.should raise_error(ArgumentError)
    -> { Marshal.dump([[[]]], 1) }.should raise_error(ArgumentError)
  end

  it "ignores the recursion limit if the limit is negative" do
    Marshal.dump([], -1).should == "\004\b[\000"
    Marshal.dump([[]], -1).should == "\004\b[\006[\000"
    Marshal.dump([[[]]], -1).should == "\004\b[\006[\006[\000"
  end

  describe "when passed an IO" do
    it "writes the serialized data to the IO-Object" do
      (obj = mock('test')).should_receive(:write).at_least(1)
      Marshal.dump("test", obj)
    end

    it "returns the IO-Object" do
      (obj = mock('test')).should_receive(:write).at_least(1)
      Marshal.dump("test", obj).should == obj
    end

    it "raises an Error when the IO-Object does not respond to #write" do
      obj = mock('test')
      -> { Marshal.dump("test", obj) }.should raise_error(TypeError)
    end


    it "calls binmode when it's defined" do
      obj = mock('test')
      obj.should_receive(:write).at_least(1)
      obj.should_receive(:binmode).at_least(1)
      Marshal.dump("test", obj)
    end
  end

  describe "when passed a StringIO" do
    it "should raise an error" do
      require "stringio"
      -> { Marshal.dump(StringIO.new) }.should raise_error(TypeError)
    end
  end

  it "raises a TypeError if marshalling a Method instance" do
    -> { Marshal.dump(Marshal.method(:dump)) }.should raise_error(TypeError)
  end

  it "raises a TypeError if marshalling a Proc" do
    -> { Marshal.dump(proc {}) }.should raise_error(TypeError)
  end

  it "raises a TypeError if dumping a IO/File instance" do
    -> { Marshal.dump(STDIN) }.should raise_error(TypeError)
    -> { File.open(__FILE__) { |f| Marshal.dump(f) } }.should raise_error(TypeError)
  end

  it "raises a TypeError if dumping a MatchData instance" do
    -> { Marshal.dump(/(.)/.match("foo")) }.should raise_error(TypeError)
  end

  it "raises a TypeError if dumping a Mutex instance" do
    m = Mutex.new
    -> { Marshal.dump(m) }.should raise_error(TypeError)
  end

  it "raises a TypeError if dumping a Binding instance" do
    -> { Marshal.dump(binding) }.should raise_error(TypeError)
  end
end
