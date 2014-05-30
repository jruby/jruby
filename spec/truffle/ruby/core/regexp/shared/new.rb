# -*- encoding: ascii-8bit -*-

describe :regexp_new, :shared => true do
  it "requires one argument and creates a new regular expression object" do
    Regexp.send(@method, '').is_a?(Regexp).should == true
  end

  it "works by default for subclasses with overridden #initialize" do
    class RegexpSpecsSubclass < Regexp
      def initialize(*args)
        super
        @args = args
      end

      attr_accessor :args
    end

    class RegexpSpecsSubclassTwo < Regexp; end

    RegexpSpecsSubclass.send(@method, "hi").should be_kind_of(RegexpSpecsSubclass)
    RegexpSpecsSubclass.send(@method, "hi").args.first.should == "hi"

    RegexpSpecsSubclassTwo.send(@method, "hi").should be_kind_of(RegexpSpecsSubclassTwo)
  end
end

describe :regexp_new_string, :shared => true do
  it "uses the String argument as an unescaped literal to construct a Regexp object" do
    Regexp.send(@method, "^hi{2,3}fo.o$").should == /^hi{2,3}fo.o$/
  end

  it "raises a RegexpError when passed an incorrect regexp" do
    lambda { Regexp.send(@method, "^[$", 0) }.should raise_error(RegexpError)
  end

  it "does not set Regexp options if only given one argument" do
    r = Regexp.send(@method, 'Hi')
    (r.options & Regexp::IGNORECASE).should     == 0
    (r.options & Regexp::MULTILINE).should      == 0
    (r.options & Regexp::EXTENDED).should       == 0
  end

  it "does not set Regexp options if second argument is nil or false" do
    r = Regexp.send(@method, 'Hi', nil)
    (r.options & Regexp::IGNORECASE).should     == 0
    (r.options & Regexp::MULTILINE).should      == 0
    (r.options & Regexp::EXTENDED).should       == 0

    r = Regexp.send(@method, 'Hi', false)
    (r.options & Regexp::IGNORECASE).should     == 0
    (r.options & Regexp::MULTILINE).should      == 0
    (r.options & Regexp::EXTENDED).should       == 0
  end

  it "sets options from second argument if it is one of the Fixnum option constants" do
    r = Regexp.send(@method, 'Hi', Regexp::IGNORECASE)
    (r.options & Regexp::IGNORECASE).should_not == 0
    (r.options & Regexp::MULTILINE).should      == 0
    (r.options & Regexp::EXTENDED).should       == 0

    r = Regexp.send(@method, 'Hi', Regexp::MULTILINE)
    (r.options & Regexp::IGNORECASE).should     == 0
    (r.options & Regexp::MULTILINE).should_not  == 0
    (r.options & Regexp::EXTENDED).should       == 0

    r = Regexp.send(@method, 'Hi', Regexp::EXTENDED)
    (r.options & Regexp::IGNORECASE).should     == 0
    (r.options & Regexp::MULTILINE).should      == 0
    (r.options & Regexp::EXTENDED).should_not   == 1
  end

  it "accepts a Fixnum of two or more options ORed together as the second argument" do
    r = Regexp.send(@method, 'Hi', Regexp::IGNORECASE | Regexp::EXTENDED)
    (r.options & Regexp::IGNORECASE).should_not == 0
    (r.options & Regexp::MULTILINE).should      == 0
    (r.options & Regexp::EXTENDED).should_not   == 0
  end

  it "treats any non-Fixnum, non-nil, non-false second argument as IGNORECASE" do
    r = Regexp.send(@method, 'Hi', Object.new)
    (r.options & Regexp::IGNORECASE).should_not == 0
    (r.options & Regexp::MULTILINE).should      == 0
    (r.options & Regexp::EXTENDED).should       == 0
  end

  ruby_version_is ""..."1.9" do
    it "does not enable multibyte support by default" do
      r = Regexp.send @method, 'Hi', true
      r.kcode.should_not == 'euc'
      r.kcode.should_not == 'sjis'
      r.kcode.should_not == 'utf8'
    end

    it "enables EUC encoding if third argument is 'e' or 'euc' (case-insensitive)" do
      Regexp.send(@method, 'Hi', nil, 'e').kcode.should     == 'euc'
      Regexp.send(@method, 'Hi', nil, 'E').kcode.should     == 'euc'
      Regexp.send(@method, 'Hi', nil, 'euc').kcode.should   == 'euc'
      Regexp.send(@method, 'Hi', nil, 'EUC').kcode.should   == 'euc'
      Regexp.send(@method, 'Hi', nil, 'EuC').kcode.should   == 'euc'
    end

    it "enables SJIS encoding if third argument is 's' or 'sjis' (case-insensitive)" do
      Regexp.send(@method, 'Hi', nil, 's').kcode.should     == 'sjis'
      Regexp.send(@method, 'Hi', nil, 'S').kcode.should     == 'sjis'
      Regexp.send(@method, 'Hi', nil, 'sjis').kcode.should  == 'sjis'
      Regexp.send(@method, 'Hi', nil, 'SJIS').kcode.should  == 'sjis'
      Regexp.send(@method, 'Hi', nil, 'sJiS').kcode.should  == 'sjis'
    end

    it "enables UTF-8 encoding if third argument is 'u' or 'utf8' (case-insensitive)" do
      Regexp.send(@method, 'Hi', nil, 'u').kcode.should     == 'utf8'
      Regexp.send(@method, 'Hi', nil, 'U').kcode.should     == 'utf8'
      Regexp.send(@method, 'Hi', nil, 'utf8').kcode.should  == 'utf8'
      Regexp.send(@method, 'Hi', nil, 'UTF8').kcode.should  == 'utf8'
      Regexp.send(@method, 'Hi', nil, 'uTf8').kcode.should  == 'utf8'
    end

    it "disables multibyte support if third argument is 'n' or 'none' (case insensitive)" do
      Regexp.send(@method, 'Hi', nil, 'N').kcode.should == 'none'
      Regexp.send(@method, 'Hi', nil, 'n').kcode.should == 'none'
      Regexp.send(@method, 'Hi', nil, 'nONE').kcode.should == 'none'
      Regexp.send(@method, '[^\\x0d\\x22\\x5c\\x80-\\xff]', nil, 'n').kcode.should == 'none'
    end
  end

  ruby_version_is "1.9" do

    it "ignores the third argument if it is 'e' or 'euc' (case-insensitive)" do
      Regexp.send(@method, 'Hi', nil, 'e').encoding.should    == Encoding::US_ASCII
      Regexp.send(@method, 'Hi', nil, 'E').encoding.should    == Encoding::US_ASCII
      Regexp.send(@method, 'Hi', nil, 'euc').encoding.should  == Encoding::US_ASCII
      Regexp.send(@method, 'Hi', nil, 'EUC').encoding.should  == Encoding::US_ASCII
      Regexp.send(@method, 'Hi', nil, 'EuC').encoding.should  == Encoding::US_ASCII
    end

    it "ignores the third argument if it is 's' or 'sjis' (case-insensitive)" do
      Regexp.send(@method, 'Hi', nil, 's').encoding.should     == Encoding::US_ASCII
      Regexp.send(@method, 'Hi', nil, 'S').encoding.should     == Encoding::US_ASCII
      Regexp.send(@method, 'Hi', nil, 'sjis').encoding.should  == Encoding::US_ASCII
      Regexp.send(@method, 'Hi', nil, 'SJIS').encoding.should  == Encoding::US_ASCII
      Regexp.send(@method, 'Hi', nil, 'sJiS').encoding.should  == Encoding::US_ASCII
    end

    it "ignores the third argument if it is 'u' or 'utf8' (case-insensitive)" do
      Regexp.send(@method, 'Hi', nil, 'u').encoding.should     == Encoding::US_ASCII
      Regexp.send(@method, 'Hi', nil, 'U').encoding.should     == Encoding::US_ASCII
      Regexp.send(@method, 'Hi', nil, 'utf8').encoding.should  == Encoding::US_ASCII
      Regexp.send(@method, 'Hi', nil, 'UTF8').encoding.should  == Encoding::US_ASCII
      Regexp.send(@method, 'Hi', nil, 'uTf8').encoding.should  == Encoding::US_ASCII
    end

    it "uses US_ASCII encoding if third argument is 'n' or 'none' (case insensitive) and only ascii characters" do
      Regexp.send(@method, 'Hi', nil, 'N').encoding.should     == Encoding::US_ASCII
      Regexp.send(@method, 'Hi', nil, 'n').encoding.should     == Encoding::US_ASCII
      Regexp.send(@method, 'Hi', nil, 'nONE').encoding.should  == Encoding::US_ASCII
    end

    it "uses ASCII_8BIT encoding if third argument is 'n' or 'none' (case insensitive) and non-ascii characters" do
      Regexp.send(@method, "\xff", nil, 'N').encoding.should     == Encoding::ASCII_8BIT
      Regexp.send(@method, "\xff", nil, 'n').encoding.should     == Encoding::ASCII_8BIT
      Regexp.send(@method, "\xff", nil, 'nONE').encoding.should  == Encoding::ASCII_8BIT
      Regexp.send(@method, '[^\\x0d\\x22\\x5c\\x80-\\xff]', nil, 'n').encoding.should  == Encoding::ASCII_8BIT
    end
  end


  describe "with escaped characters" do
    it "raises a Regexp error if there is a trailing backslash" do
      lambda { Regexp.send(@method, "\\") }.should raise_error(RegexpError)
    end

    it "accepts a backspace followed by a character" do
      Regexp.send(@method, "\\N").should == /#{"\x5cN"}/
    end

    it "accepts a one-digit octal value" do
      Regexp.send(@method, "\0").should == /#{"\x00"}/
    end

    it "accepts a two-digit octal value" do
      Regexp.send(@method, "\11").should == /#{"\x09"}/
    end

    it "accepts a three-digit octal value" do
      Regexp.send(@method, "\315").should == /#{"\xcd"}/
    end

    it "interprets a digit following a three-digit octal value as a character" do
      Regexp.send(@method, "\3762").should == /#{"\xfe2"}/
    end

    it "accepts a one-digit hexadecimal value" do
      Regexp.send(@method, "\x9n").should == /#{"\x09n"}/
    end

    it "accepts a two-digit hexadecimal value" do
      Regexp.send(@method, "\x23").should == /#{"\x23"}/
    end

    it "interprets a digit following a two-digit hexadecimal value as a character" do
      Regexp.send(@method, "\x420").should == /#{"\x420"}/
    end

    ruby_version_is ""..."1.9" do
      # TODO: Add version argument to compliance guards
      not_supported_on :rubinius do
        it "raises a RegexpError if \\x is not followed by any hexadecimal digits" do
          lambda { Regexp.send(@method, "\\" + "xn") }.should raise_error(RegexpError)
        end
      end
    end

    ruby_version_is "1.9" do
      it "raises a RegexpError if \\x is not followed by any hexadecimal digits" do
        lambda { Regexp.send(@method, "\\" + "xn") }.should raise_error(RegexpError)
      end
    end

    it "accepts an escaped string interpolation" do
      Regexp.send(@method, "\#{abc}").should == /#{"\#{abc}"}/
    end

    it "accepts '\\n'" do
      Regexp.send(@method, "\n").should == /#{"\x0a"}/
    end

    it "accepts '\\t'" do
      Regexp.send(@method, "\t").should == /#{"\x09"}/
    end

    it "accepts '\\r'" do
      Regexp.send(@method, "\r").should == /#{"\x0d"}/
    end

    it "accepts '\\f'" do
      Regexp.send(@method, "\f").should == /#{"\x0c"}/
    end

    it "accepts '\\v'" do
      Regexp.send(@method, "\v").should == /#{"\x0b"}/
    end

    it "accepts '\\a'" do
      Regexp.send(@method, "\a").should == /#{"\x07"}/
    end

    it "accepts '\\e'" do
      Regexp.send(@method, "\e").should == /#{"\x1b"}/
    end

    it "accepts '\\C-\\n'" do
      Regexp.send(@method, "\C-\n").should == /#{"\x0a"}/
    end

    it "accepts '\\C-\\t'" do
      Regexp.send(@method, "\C-\t").should == /#{"\x09"}/
    end

    it "accepts '\\C-\\r'" do
      Regexp.send(@method, "\C-\r").should == /#{"\x0d"}/
    end

    it "accepts '\\C-\\f'" do
      Regexp.send(@method, "\C-\f").should == /#{"\x0c"}/
    end

    it "accepts '\\C-\\v'" do
      Regexp.send(@method, "\C-\v").should == /#{"\x0b"}/
    end

    it "accepts '\\C-\\a'" do
      Regexp.send(@method, "\C-\a").should == /#{"\x07"}/
    end

    it "accepts '\\C-\\e'" do
      Regexp.send(@method, "\C-\e").should == /#{"\x1b"}/
    end

    it "accepts '\\c\\n'" do
      Regexp.send(@method, "\C-\n").should == /#{"\x0a"}/
    end

    it "accepts '\\c\\t'" do
      Regexp.send(@method, "\C-\t").should == /#{"\x09"}/
    end

    it "accepts '\\c\\r'" do
      Regexp.send(@method, "\C-\r").should == /#{"\x0d"}/
    end

    it "accepts '\\c\\f'" do
      Regexp.send(@method, "\C-\f").should == /#{"\x0c"}/
    end

    it "accepts '\\c\\v'" do
      Regexp.send(@method, "\C-\v").should == /#{"\x0b"}/
    end

    it "accepts '\\c\\a'" do
      Regexp.send(@method, "\C-\a").should == /#{"\x07"}/
    end

    it "accepts '\\c\\e'" do
      Regexp.send(@method, "\C-\e").should == /#{"\x1b"}/
    end

    it "accepts '\\M-\\n'" do
      Regexp.send(@method, "\M-\n").should == /#{"\x8a"}/
    end

    it "accepts '\\M-\\t'" do
      Regexp.send(@method, "\M-\t").should == /#{"\x89"}/
    end

    it "accepts '\\M-\\r'" do
      Regexp.send(@method, "\M-\r").should == /#{"\x8d"}/
    end

    it "accepts '\\M-\\f'" do
      Regexp.send(@method, "\M-\f").should == /#{"\x8c"}/
    end

    it "accepts '\\M-\\v'" do
      Regexp.send(@method, "\M-\v").should == /#{"\x8b"}/
    end

    it "accepts '\\M-\\a'" do
      Regexp.send(@method, "\M-\a").should == /#{"\x87"}/
    end

    it "accepts '\\M-\\e'" do
      Regexp.send(@method, "\M-\e").should == /#{"\x9b"}/
    end

    it "accepts '\\M-\\C-\\n'" do
      Regexp.send(@method, "\M-\n").should == /#{"\x8a"}/
    end

    it "accepts '\\M-\\C-\\t'" do
      Regexp.send(@method, "\M-\t").should == /#{"\x89"}/
    end

    it "accepts '\\M-\\C-\\r'" do
      Regexp.send(@method, "\M-\r").should == /#{"\x8d"}/
    end

    it "accepts '\\M-\\C-\\f'" do
      Regexp.send(@method, "\M-\f").should == /#{"\x8c"}/
    end

    it "accepts '\\M-\\C-\\v'" do
      Regexp.send(@method, "\M-\v").should == /#{"\x8b"}/
    end

    it "accepts '\\M-\\C-\\a'" do
      Regexp.send(@method, "\M-\a").should == /#{"\x87"}/
    end

    it "accepts '\\M-\\C-\\e'" do
      Regexp.send(@method, "\M-\e").should == /#{"\x9b"}/
    end

    it "accepts '\\M-\\c\\n'" do
      Regexp.send(@method, "\M-\n").should == /#{"\x8a"}/
    end

    it "accepts '\\M-\\c\\t'" do
      Regexp.send(@method, "\M-\t").should == /#{"\x89"}/
    end

    it "accepts '\\M-\\c\\r'" do
      Regexp.send(@method, "\M-\r").should == /#{"\x8d"}/
    end

    it "accepts '\\M-\\c\\f'" do
      Regexp.send(@method, "\M-\f").should == /#{"\x8c"}/
    end

    it "accepts '\\M-\\c\\v'" do
      Regexp.send(@method, "\M-\v").should == /#{"\x8b"}/
    end

    it "accepts '\\M-\\c\\a'" do
      Regexp.send(@method, "\M-\a").should == /#{"\x87"}/
    end

    it "accepts '\\M-\\c\\e'" do
      Regexp.send(@method, "\M-\e").should == /#{"\x9b"}/
    end

    it "accepts multiple consecutive '\\' characters" do
      Regexp.send(@method, "\\\\\\N").should == /#{"\\\\\\N"}/
    end

    it "accepts characters and escaped octal digits" do
      Regexp.send(@method, "abc\076").should == /#{"abc\x3e"}/
    end

    it "accepts escaped octal digits and characters" do
      Regexp.send(@method, "\076abc").should == /#{"\x3eabc"}/
    end

    it "accepts characters and escaped hexadecimal digits" do
      Regexp.send(@method, "abc\x42").should == /#{"abc\x42"}/
    end

    it "accepts escaped hexadecimal digits and characters" do
      Regexp.send(@method, "\x3eabc").should == /#{"\x3eabc"}/
    end

    it "accepts escaped hexadecimal and octal digits" do
      Regexp.send(@method, "\061\x42").should == /#{"\x31\x42"}/
    end

    ruby_version_is "1.9" do
      it "accepts \\u{H} for a single Unicode codepoint" do
        Regexp.send(@method, "\u{f}").should == /#{"\x0f"}/
      end

      it "accepts \\u{HH} for a single Unicode codepoint" do
        Regexp.send(@method, "\u{7f}").should == /#{"\x7f"}/
      end

      it "accepts \\u{HHH} for a single Unicode codepoint" do
        Regexp.send(@method, "\u{07f}").should == /#{"\x7f"}/
      end

      it "accepts \\u{HHHH} for a single Unicode codepoint" do
        Regexp.send(@method, "\u{0000}").should == /#{"\x00"}/
      end

      it "accepts \\u{HHHHH} for a single Unicode codepoint" do
        Regexp.send(@method, "\u{00001}").should == /#{"\x01"}/
      end

      it "accepts \\u{HHHHHH} for a single Unicode codepoint" do
        Regexp.send(@method, "\u{000000}").should == /#{"\x00"}/
      end

      it "accepts characters followed by \\u{HHHH}" do
        Regexp.send(@method, "abc\u{3042}").should == /#{"abc\u3042"}/
      end

      it "accepts \\u{HHHH} followed by characters" do
        Regexp.send(@method, "\u{3042}abc").should == /#{"\u3042abc"}/
      end

      it "accepts escaped hexadecimal digits followed by \\u{HHHH}" do
        Regexp.send(@method, "\x42\u{3042}").should == /#{"\x42\u3042"}/
      end

      it "accepts escaped octal digits followed by \\u{HHHH}" do
        Regexp.send(@method, "\056\u{3042}").should == /#{"\x2e\u3042"}/
      end

      it "accepts a combination of escaped octal and hexadecimal digits and \\u{HHHH}" do
        Regexp.send(@method, "\056\x42\u{3042}\x52\076").should == /#{"\x2e\x42\u3042\x52\x3e"}/
      end

      it "accepts \\uHHHH for a single Unicode codepoint" do
        Regexp.send(@method, "\u3042").should == /#{"\u3042"}/
      end

      it "accepts characters followed by \\uHHHH" do
        Regexp.send(@method, "abc\u3042").should == /#{"abc\u3042"}/
      end

      it "accepts \\uHHHH followed by characters" do
        Regexp.send(@method, "\u3042abc").should == /#{"\u3042abc"}/
      end

      it "accepts escaped hexadecimal digits followed by \\uHHHH" do
        Regexp.send(@method, "\x42\u3042").should == /#{"\x42\u3042"}/
      end

      it "accepts escaped octal digits followed by \\uHHHH" do
        Regexp.send(@method, "\056\u3042").should == /#{"\x2e\u3042"}/
      end

      it "accepts a combination of escaped octal and hexadecimal digits and \\uHHHH" do
        Regexp.send(@method, "\056\x42\u3042\x52\076").should == /#{"\x2e\x42\u3042\x52\x3e"}/
      end

      it "raises a RegexpError if less than four digits are given for \\uHHHH" do
        lambda { Regexp.send(@method, "\\" + "u304") }.should raise_error(RegexpError)
      end

      it "raises a RegexpError if the \\u{} escape is empty" do
        lambda { Regexp.send(@method, "\\" + "u{}") }.should raise_error(RegexpError)
      end

      it "raises a RegexpError if more than six hexadecimal digits are given" do
        lambda { Regexp.send(@method, "\\" + "u{0ffffff}") }.should raise_error(RegexpError)
      end

      it "returns a Regexp with US-ASCII encoding if only 7-bit ASCII characters are present regardless of the input String's encoding" do
        Regexp.send(@method, "abc").encoding.should == Encoding::US_ASCII
      end

      it "returns a Regexp with source String having US-ASCII encoding if only 7-bit ASCII characters are present regardless of the input String's encoding" do
        Regexp.send(@method, "abc").source.encoding.should == Encoding::US_ASCII
      end

      it "returns a Regexp with US-ASCII encoding if UTF-8 escape sequences using only 7-bit ASCII are present" do
        Regexp.send(@method, "\u{61}").encoding.should == Encoding::US_ASCII
      end

      it "returns a Regexp with source String having US-ASCII encoding if UTF-8 escape sequences using only 7-bit ASCII are present" do
        Regexp.send(@method, "\u{61}").source.encoding.should == Encoding::US_ASCII
      end

      it "returns a Regexp with UTF-8 encoding if any UTF-8 escape sequences outside 7-bit ASCII are present" do
        Regexp.send(@method, "\u{ff}").encoding.should == Encoding::UTF_8
      end

      it "returns a Regexp with source String having UTF-8 encoding if any UTF-8 escape sequences outside 7-bit ASCII are present" do
        Regexp.send(@method, "\u{ff}").source.encoding.should == Encoding::UTF_8
      end

      it "returns a Regexp with the input String's encoding" do
        str = "\x82\xa0".force_encoding(Encoding::Shift_JIS)
        Regexp.send(@method, str).encoding.should == Encoding::Shift_JIS
      end

      it "returns a Regexp with source String having the input String's encoding" do
        str = "\x82\xa0".force_encoding(Encoding::Shift_JIS)
        Regexp.send(@method, str).source.encoding.should == Encoding::Shift_JIS
      end
    end
  end
end

describe :regexp_new_regexp, :shared => true do
  it "uses the argument as a literal to construct a Regexp object" do
    Regexp.send(@method, /^hi{2,3}fo.o$/).should == /^hi{2,3}fo.o$/
  end

  it "preserves any options given in the Regexp literal" do
    (Regexp.send(@method, /Hi/i).options & Regexp::IGNORECASE).should_not == 0
    (Regexp.send(@method, /Hi/m).options & Regexp::MULTILINE).should_not == 0
    (Regexp.send(@method, /Hi/x).options & Regexp::EXTENDED).should_not == 0

    r = Regexp.send @method, /Hi/imx
    (r.options & Regexp::IGNORECASE).should_not == 0
    (r.options & Regexp::MULTILINE).should_not == 0
    (r.options & Regexp::EXTENDED).should_not == 0

    r = Regexp.send @method, /Hi/
    (r.options & Regexp::IGNORECASE).should == 0
    (r.options & Regexp::MULTILINE).should == 0
    (r.options & Regexp::EXTENDED).should == 0
  end

  it "does not honour options given as additional arguments" do
    r = Regexp.send @method, /hi/, Regexp::IGNORECASE
    (r.options & Regexp::IGNORECASE).should == 0
  end

  ruby_version_is ""..."1.9" do
    it "does not enable multibyte support by default" do
      r = Regexp.send @method, /Hi/
      r.kcode.should_not == 'euc'
      r.kcode.should_not == 'sjis'
      r.kcode.should_not == 'utf8'
    end

    it "enables multibyte support if given in the literal" do
      Regexp.send(@method, /Hi/u).kcode.should == 'utf8'
      Regexp.send(@method, /Hi/e).kcode.should == 'euc'
      Regexp.send(@method, /Hi/s).kcode.should == 'sjis'
      Regexp.send(@method, /Hi/n).kcode.should == 'none'
    end

    describe "with $KCODE set" do

      before :each do
        @kcode = $KCODE
        $KCODE = 'u'
      end

      after :each do
        $KCODE = @kcode
      end

      it "falls back to ASCII for an invalid UTF-8 regexp" do
        (Regexp.send(@method, /^([\x00-\x7F]|[\xC2-\xDF][\x80-\xBF])*$/) =~ "hellø").should == 0
      end

    end
  end

  ruby_version_is "1.9" do
    it "sets the encoding to UTF-8 if the Regexp literal has the 'u' option" do
      Regexp.send(@method, /Hi/u).encoding.should == Encoding::UTF_8
    end

    it "sets the encoding to EUC-JP if the Regexp literal has the 'e' option" do
      Regexp.send(@method, /Hi/e).encoding.should == Encoding::EUC_JP
    end

    it "sets the encoding to Windows-31J if the Regexp literal has the 's' option" do
      Regexp.send(@method, /Hi/s).encoding.should == Encoding::Windows_31J
    end

    it "sets the encoding to US-ASCII if the Regexp literal has the 'n' option and the source String is ASCII only" do
      Regexp.send(@method, /Hi/n).encoding.should == Encoding::US_ASCII
    end

    it "sets the encoding to source String's encoding if the Regexp literal has the 'n' option and the source String is not ASCII only" do
      Regexp.send(@method, /\xff/n).encoding.should == Encoding::ASCII_8BIT
    end
  end
end
