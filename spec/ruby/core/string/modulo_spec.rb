require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes.rb', __FILE__)

describe "String#%" do
  it "formats multiple expressions" do
    ("%b %x %d %s" % [10, 10, 10, 10]).should == "1010 a 10 10"
  end

  it "formats expressions mid string" do
    ("hello %s!" % "world").should == "hello world!"
  end

  it "formats %% into %" do
    ("%d%% %s" % [10, "of chickens!"]).should == "10% of chickens!"
  end

  it "formats single % characters before a newline or NULL as literal %s" do
    ("%" % []).should == "%"
    ("foo%" % []).should == "foo%"
    ("%\n" % []).should == "%\n"
    ("foo%\n" % []).should == "foo%\n"
    ("%\0" % []).should == "%\0"
    ("foo%\0" % []).should == "foo%\0"
    ("%\n.3f" % 1.2).should == "%\n.3f"
    ("%\0.3f" % 1.2).should == "%\0.3f"
  end

  it "raises an error if single % appears anywhere else" do
    lambda { (" % " % []) }.should raise_error(ArgumentError)
    lambda { ("foo%quux" % []) }.should raise_error(ArgumentError)
  end

  it "raises an error if NULL or \\n appear anywhere else in the format string" do
    begin
      old_debug, $DEBUG = $DEBUG, false

      lambda { "%.\n3f" % 1.2 }.should raise_error(ArgumentError)
      lambda { "%.3\nf" % 1.2 }.should raise_error(ArgumentError)
      lambda { "%.\03f" % 1.2 }.should raise_error(ArgumentError)
      lambda { "%.3\0f" % 1.2 }.should raise_error(ArgumentError)
    ensure
      $DEBUG = old_debug
    end
  end

  it "ignores unused arguments when $DEBUG is false" do
    begin
      old_debug = $DEBUG
      $DEBUG = false

      ("" % [1, 2, 3]).should == ""
      ("%s" % [1, 2, 3]).should == "1"
    ensure
      $DEBUG = old_debug
    end
  end

  it "raises an ArgumentError for unused arguments when $DEBUG is true" do
    begin
      old_debug = $DEBUG
      $DEBUG = true
      s = $stderr
      $stderr = IOStub.new

      lambda { "" % [1, 2, 3]   }.should raise_error(ArgumentError)
      lambda { "%s" % [1, 2, 3] }.should raise_error(ArgumentError)
    ensure
      $DEBUG = old_debug
      $stderr = s
    end
  end

  it "always allows unused arguments when positional argument style is used" do
    begin
      old_debug = $DEBUG
      $DEBUG = false

      ("%2$s" % [1, 2, 3]).should == "2"
      $DEBUG = true
      ("%2$s" % [1, 2, 3]).should == "2"
    ensure
      $DEBUG = old_debug
    end
  end

  it "replaces trailing absolute argument specifier without type with percent sign" do
    ("hello %1$" % "foo").should == "hello %"
  end

  it "raises an ArgumentError when given invalid argument specifiers" do
    lambda { "%1" % [] }.should raise_error(ArgumentError)
    lambda { "%+" % [] }.should raise_error(ArgumentError)
    lambda { "%-" % [] }.should raise_error(ArgumentError)
    lambda { "%#" % [] }.should raise_error(ArgumentError)
    lambda { "%0" % [] }.should raise_error(ArgumentError)
    lambda { "%*" % [] }.should raise_error(ArgumentError)
    lambda { "%." % [] }.should raise_error(ArgumentError)
    lambda { "%_" % [] }.should raise_error(ArgumentError)
    lambda { "%0$s" % "x"              }.should raise_error(ArgumentError)
    lambda { "%*0$s" % [5, "x"]        }.should raise_error(ArgumentError)
    lambda { "%*1$.*0$1$s" % [1, 2, 3] }.should raise_error(ArgumentError)
  end

  it "raises an ArgumentError when multiple positional argument tokens are given for one format specifier" do
    lambda { "%1$1$s" % "foo" }.should raise_error(ArgumentError)
  end

  it "respects positional arguments and precision tokens given for one format specifier" do
    ("%2$1d" % [1, 0]).should == "0"
    ("%2$1d" % [0, 1]).should == "1"

    ("%2$.2f" % [1, 0]).should == "0.00"
    ("%2$.2f" % [0, 1]).should == "1.00"
  end

  it "allows more than one digit of position" do
    ("%50$d" % (0..100).to_a).should == "49"
  end

  it "raises an ArgumentError when multiple width star tokens are given for one format specifier" do
    lambda { "%**s" % [5, 5, 5] }.should raise_error(ArgumentError)
  end

  it "raises an ArgumentError when a width star token is seen after a width token" do
    lambda { "%5*s" % [5, 5] }.should raise_error(ArgumentError)
  end

  it "raises an ArgumentError when multiple precision tokens are given" do
    lambda { "%.5.5s" % 5      }.should raise_error(ArgumentError)
    lambda { "%.5.*s" % [5, 5] }.should raise_error(ArgumentError)
    lambda { "%.*.5s" % [5, 5] }.should raise_error(ArgumentError)
  end

  it "raises an ArgumentError when there are less arguments than format specifiers" do
    ("foo" % []).should == "foo"
    lambda { "%s" % []     }.should raise_error(ArgumentError)
    lambda { "%s %s" % [1] }.should raise_error(ArgumentError)
  end

  it "raises an ArgumentError when absolute and relative argument numbers are mixed" do
    lambda { "%s %1$s" % "foo" }.should raise_error(ArgumentError)
    lambda { "%1$s %s" % "foo" }.should raise_error(ArgumentError)

    lambda { "%s %2$s" % ["foo", "bar"] }.should raise_error(ArgumentError)
    lambda { "%2$s %s" % ["foo", "bar"] }.should raise_error(ArgumentError)

    lambda { "%*2$s" % [5, 5, 5]     }.should raise_error(ArgumentError)
    lambda { "%*.*2$s" % [5, 5, 5]   }.should raise_error(ArgumentError)
    lambda { "%*2$.*2$s" % [5, 5, 5] }.should raise_error(ArgumentError)
    lambda { "%*.*2$s" % [5, 5, 5]   }.should raise_error(ArgumentError)
  end

  it "allows reuse of the one argument multiple via absolute argument numbers" do
    ("%1$s %1$s" % "foo").should == "foo foo"
    ("%1$s %2$s %1$s %2$s" % ["foo", "bar"]).should == "foo bar foo bar"
  end

  it "always interprets an array argument as a list of argument parameters" do
    lambda { "%p" % [] }.should raise_error(ArgumentError)
    ("%p" % [1]).should == "1"
    ("%p %p" % [1, 2]).should == "1 2"
  end

  it "always interprets an array subclass argument as a list of argument parameters" do
    lambda { "%p" % StringSpecs::MyArray[] }.should raise_error(ArgumentError)
    ("%p" % StringSpecs::MyArray[1]).should == "1"
    ("%p %p" % StringSpecs::MyArray[1, 2]).should == "1 2"
  end

  it "allows positional arguments for width star and precision star arguments" do
    ("%*1$.*2$3$d" % [10, 5, 1]).should == "     00001"
  end

  it "allows negative width to imply '-' flag" do
    ("%*1$.*2$3$d" % [-10, 5, 1]).should == "00001     "
    ("%-*1$.*2$3$d" % [10, 5, 1]).should == "00001     "
    ("%-*1$.*2$3$d" % [-10, 5, 1]).should == "00001     "
  end

  it "ignores negative precision" do
    ("%*1$.*2$3$d" % [10, -5, 1]).should == "         1"
  end

  it "allows a star to take an argument number to use as the width" do
    ("%1$*2$s" % ["a", 8]).should == "       a"
    ("%1$*10$s" % ["a",0,0,0,0,0,0,0,0,8]).should == "       a"
  end

  it "calls to_int on width star and precision star tokens" do
    w = mock('10')
    w.should_receive(:to_int).and_return(10)

    p = mock('5')
    p.should_receive(:to_int).and_return(5)

    ("%*.*f" % [w, p, 1]).should == "   1.00000"


    w = mock('10')
    w.should_receive(:to_int).and_return(10)

    p = mock('5')
    p.should_receive(:to_int).and_return(5)

    ("%*.*d" % [w, p, 1]).should == "     00001"
  end

  ruby_bug "#", "1.8.6.228" do
    it "tries to convert the argument to Array by calling #to_ary" do
      obj = mock('[1,2]')
      def obj.to_ary() [1, 2] end
      def obj.to_s() "obj" end
      ("%s %s" % obj).should == "1 2"
      ("%s" % obj).should == "1"
    end
  end

  it "doesn't return subclass instances when called on a subclass" do
    universal = mock('0')
    def universal.to_int() 0 end
    def universal.to_str() "0" end
    def universal.to_f() 0.0 end

    [
      "", "foo",
      "%b", "%B", "%c", "%d", "%e", "%E",
      "%f", "%g", "%G", "%i", "%o", "%p",
      "%s", "%u", "%x", "%X"
    ].each do |format|
      (StringSpecs::MyString.new(format) % universal).should be_kind_of(String)
    end
  end

  it "always taints the result when the format string is tainted" do
    universal = mock('0')
    def universal.to_int() 0 end
    def universal.to_str() "0" end
    def universal.to_f() 0.0 end

    [
      "", "foo",
      "%b", "%B", "%c", "%d", "%e", "%E",
      "%f", "%g", "%G", "%i", "%o", "%p",
      "%s", "%u", "%x", "%X"
    ].each do |format|
      subcls_format = StringSpecs::MyString.new(format)
      subcls_format.taint
      format.taint

      (format % universal).tainted?.should == true
      (subcls_format % universal).tainted?.should == true
    end
  end

  it "supports binary formats using %b for positive numbers" do
    ("%b" % 10).should == "1010"
    ("% b" % 10).should == " 1010"
    ("%1$b" % [10, 20]).should == "1010"
    ("%#b" % 10).should == "0b1010"
    ("%+b" % 10).should == "+1010"
    ("%-9b" % 10).should == "1010     "
    ("%05b" % 10).should == "01010"
    ("%*b" % [10, 6]).should == "       110"
    ("%*b" % [-10, 6]).should == "110       "
    ("%.4b" % 2).should == "0010"
  end

  ruby_version_is ""..."1.9" do
    it "supports binary formats using %b for negative numbers" do
      ("%b" % -5).should == "..1011"
      ("%0b" % -5).should == "1011"
      ("%.4b" % 2).should == "0010"
      ("%.1b" % -5).should == "1011"
      ("%.7b" % -5).should == "1111011"
      ("%.10b" % -5).should == "1111111011"
      ("% b" % -5).should == "-101"
      ("%+b" % -5).should == "-101"
      ("%b" % -(2 ** 64 + 5)).should ==
      "..101111111111111111111111111111111111111111111111111111111111111011"
    end
  end

  ruby_version_is "1.9" do
    it "supports binary formats using %b for negative numbers" do
      ("%b" % -5).should == "..1011"
      ("%0b" % -5).should == "..1011"
      ("%.1b" % -5).should == "..1011"
      ("%.7b" % -5).should == "..11011"
      ("%.10b" % -5).should == "..11111011"
      ("% b" % -5).should == "-101"
      ("%+b" % -5).should == "-101"
      ("%b" % -(2 ** 64 + 5)).should ==
      "..101111111111111111111111111111111111111111111111111111111111111011"
    end
  end

  it "supports binary formats using %B with same behaviour as %b except for using 0B instead of 0b for #" do
    ("%B" % 10).should == ("%b" % 10)
    ("% B" % 10).should == ("% b" % 10)
    ("%1$B" % [10, 20]).should == ("%1$b" % [10, 20])
    ("%+B" % 10).should == ("%+b" % 10)
    ("%-9B" % 10).should == ("%-9b" % 10)
    ("%05B" % 10).should == ("%05b" % 10)
    ("%*B" % [10, 6]).should == ("%*b" % [10, 6])
    ("%*B" % [-10, 6]).should == ("%*b" % [-10, 6])

    ("%B" % -5).should == ("%b" % -5)
    ("%0B" % -5).should == ("%0b" % -5)
    ("%.1B" % -5).should == ("%.1b" % -5)
    ("%.7B" % -5).should == ("%.7b" % -5)
    ("%.10B" % -5).should == ("%.10b" % -5)
    ("% B" % -5).should == ("% b" % -5)
    ("%+B" % -5).should == ("%+b" % -5)
    ("%B" % -(2 ** 64 + 5)).should == ("%b" % -(2 ** 64 + 5))

    ("%#B" % 10).should == "0B1010"
  end

  ruby_version_is ""..."1.9" do
    it "supports character formats using %c" do
      ("%c" % 10).should == "\n"
      ("%2$c" % [10, 11, 14]).should == "\v"
      ("%-4c" % 10).should == "\n   "
      ("%*c" % [10, 3]).should == "         \003"
      ("%c" % (256 + 42)).should == "*"

      lambda { "%c" % Object }.should raise_error(TypeError)
    end

    it "uses argument % 256" do
      ("%c" % [256 * 3 + 64]).should == ("%c" % 64)
      ("%c" % -200).should == ("%c" % 56)
    end
  end

  ruby_version_is "1.9" do
    it "supports character formats using %c" do
      ("%c" % 10).should == "\n"
      ("%2$c" % [10, 11, 14]).should == "\v"
      ("%-4c" % 10).should == "\n   "
      ("%*c" % [10, 3]).should == "         \003"
      ("%c" % 42).should == "*"

      lambda { "%c" % Object }.should raise_error(TypeError)
    end

    it "supports single character strings as argument for %c" do
      ("%c" % 'A').should == "A"
    end

    it "raises an exception for multiple character strings as argument for %c" do
      lambda { "%c" % 'AA' }.should raise_error(ArgumentError)
    end

    it "calls to_str on argument for %c formats" do
      obj = mock('A')
      obj.should_receive(:to_str).and_return('A')

      ("%c" % obj).should == "A"
    end
  end

  ruby_version_is "1.8.6.278" do
    it "calls #to_ary on argument for %c formats" do
      obj = mock('65')
      obj.should_receive(:to_ary).and_return([65])
      ("%c" % obj).should == ("%c" % [65])
    end

    it "calls #to_int on argument for %c formats, if the argument does not respond to #to_ary" do
      obj = mock('65')
      obj.should_receive(:to_int).and_return(65)

      ("%c" % obj).should == ("%c" % 65)
    end
  end

  %w(d i).each do |f|
    format = "%" + f

    it "supports integer formats using #{format}" do
      ("%#{f}" % 10).should == "10"
      ("% #{f}" % 10).should == " 10"
      ("%1$#{f}" % [10, 20]).should == "10"
      ("%+#{f}" % 10).should == "+10"
      ("%-7#{f}" % 10).should == "10     "
      ("%04#{f}" % 10).should == "0010"
      ("%*#{f}" % [10, 4]).should == "         4"
    end

    it "supports negative integers using #{format}" do
      ("%#{f}" % -5).should == "-5"
      ("%3#{f}" % -5).should == " -5"
      ("%03#{f}" % -5).should == "-05"
      ("%+03#{f}" % -5).should == "-05"
      ("%-3#{f}" % -5).should == "-5 "
    end

    # The following version inconsistency in negative-integers is explained in
    # http://ujihisa.blogspot.com/2009/12/string-differs-between-ruby-18-and-19.html
    ruby_version_is ""..."1.9" do
      it "supports negative integers using #{format}, giving priority to `0`" do
        ("%-03#{f}" % -5).should == "-05"
        ("%+-03#{f}" % -5).should == "-05"
      end
    end

    ruby_version_is "1.9" do
      it "supports negative integers using #{format}, giving priority to `-`" do
        ("%-03#{f}" % -5).should == "-5 "
        ("%+-03#{f}" % -5).should == "-5 "
      end
    end
  end

  it "supports float formats using %e" do
    ("%e" % 10).should == "1.000000e+01"
    ("% e" % 10).should == " 1.000000e+01"
    ("%1$e" % 10).should == "1.000000e+01"
    ("%#e" % 10).should == "1.000000e+01"
    ("%+e" % 10).should == "+1.000000e+01"
    ("%-7e" % 10).should == "1.000000e+01"
    ("%05e" % 10).should == "1.000000e+01"
    ("%*e" % [10, 9]).should == "9.000000e+00"
  end

  ruby_version_is ""..."1.9" do
    not_compliant_on :rubinius, :jruby do
      it "supports float formats using %e, and downcases -Inf, Inf" do
        ("%e" % 1e1020).should == "inf"
        ("%e" % -1e1020).should == "-inf"
      end

      platform_is :bsd do
        it "supports float formats using %e, and downcases NaN" do
          ("%e" % (0.0/0)).should == "nan"
          ("%e" % (-0e0/0)).should == "nan"
        end
      end

      platform_is :linux do
        it "supports float formats using %e, and downcases -NaN" do
          ("%e" % (0.0/0)).should == "-nan"
          ("%e" % (-0e0/0)).should == "-nan"
        end
      end
    end
  end

  # Inf, -Inf, and NaN are identifiers for results of floating point operations
  # that cannot be expressed with any value in the set of real numbers. Upcasing
  # or downcasing these identifiers for %e or %E, which refers to the case of the
  # of the exponent identifier, is silly.

  deviates_on :rubinius, :jruby do
    it "supports float formats using %e, but Inf, -Inf, and NaN are not floats" do
      ("%e" % 1e1020).should == "Inf"
      ("%e" % -1e1020).should == "-Inf"
      ("%e" % (-0e0/0)).should == "NaN"
      ("%e" % (0.0/0)).should == "NaN"
    end

    it "supports float formats using %E, but Inf, -Inf, and NaN are not floats" do
      ("%E" % 1e1020).should == "Inf"
      ("%E" % -1e1020).should == "-Inf"
      ("%-10E" % 1e1020).should == "Inf       "
      ("%10E" % 1e1020).should == "       Inf"
      ("%+E" % 1e1020).should == "+Inf"
      ("% E" % 1e1020).should == " Inf"
      ("%E" % (0.0/0)).should == "NaN"
      ("%E" % (-0e0/0)).should == "NaN"
    end
  end

  it "supports float formats using %E" do
    ("%E" % 10).should == "1.000000E+01"
    ("% E" % 10).should == " 1.000000E+01"
    ("%1$E" % 10).should == "1.000000E+01"
    ("%#E" % 10).should == "1.000000E+01"
    ("%+E" % 10).should == "+1.000000E+01"
    ("%-7E" % 10).should == "1.000000E+01"
    ("%05E" % 10).should == "1.000000E+01"
    ("%*E" % [10, 9]).should == "9.000000E+00"
  end

  not_compliant_on :rubinius, :jruby do
    ruby_version_is ""..."1.9" do
      it "supports float formats using %E, and upcases Inf, -Inf" do
        ("%E" % 1e1020).should == "INF"
        ("%E" % -1e1020).should == "-INF"
        ("%-10E" % 1e1020).should == "INF       "
        ("%+E" % 1e1020).should == "+INF"
        ("% E" % 1e1020).should == " INF"
      end

      platform_is :bsd do
        it "supports float formats using %E, and upcases NaN" do
          ("%E" % (0.0/0)).should == "NAN"
          ("%E" % (-0e0/0)).should == "NAN"
        end
      end

      platform_is :linux do
        it "supports float formats using %E, and upcases -NaN" do
          ("%E" % (0.0/0)).should == "-NAN"
          ("%E" % (-0e0/0)).should == "-NAN"
        end
      end
    end

    ruby_version_is ""..."1.9" do
      platform_is :darwin do
        it "pads with zeros using %E with Inf, -Inf, and NaN" do
          ("%010E" % -1e1020).should == "-000000INF"
          ("%010E" % 1e1020).should == "0000000INF"
          ("%010E" % (0.0/0)).should == "0000000NAN"
        end
      end

      platform_is :linux do
        it "pads with spaces for %E with Inf, -Inf, and NaN" do
          ("%010E" % -1e1020).should == "      -INF"
          ("%010E" % 1e1020).should == "       INF"
          ("%010E" % (0.0/0)).should == "      -NAN"
        end
      end

      platform_is_not :darwin, :linux do
        it "pads with spaces for %E with Inf, -Inf, and NaN" do
          ("%010E" % -1e1020).should == "      -INF"
          ("%010E" % 1e1020).should == "       INF"
          ("%010E" % (0.0/0)).should == "       NAN"
        end
      end
    end

    ruby_version_is "1.9" do
      it "pads with spaces for %E with Inf, -Inf, and NaN" do
        ("%010E" % -1e1020).should == "      -Inf"
        ("%010E" % 1e1020).should == "       Inf"
        ("%010E" % (0.0/0)).should == "       NaN"
      end
    end
  end

  it "supports float formats using %f" do
    ("%f" % 10).should == "10.000000"
    ("% f" % 10).should == " 10.000000"
    ("%1$f" % 10).should == "10.000000"
    ("%#f" % 10).should == "10.000000"
    ("%#0.3f" % 10).should == "10.000"
    ("%+f" % 10).should == "+10.000000"
    ("%-7f" % 10).should == "10.000000"
    ("%05f" % 10).should == "10.000000"
    ("%*f" % [10, 9]).should == "  9.000000"
  end

  it "supports float formats using %g" do
    ("%g" % 10).should == "10"
    ("% g" % 10).should == " 10"
    ("%1$g" % 10).should == "10"
    ("%#g" % 10).should == "10.0000"
    ("%#.3g" % 10).should == "10.0"
    ("%+g" % 10).should == "+10"
    ("%-7g" % 10).should == "10     "
    ("%05g" % 10).should == "00010"
    ("%g" % 10**10).should == "1e+10"
    ("%*g" % [10, 9]).should == "         9"
  end

  it "supports float formats using %G" do
    ("%G" % 10).should == "10"
    ("% G" % 10).should == " 10"
    ("%1$G" % 10).should == "10"
    ("%#G" % 10).should == "10.0000"
    ("%#.3G" % 10).should == "10.0"
    ("%+G" % 10).should == "+10"
    ("%-7G" % 10).should == "10     "
    ("%05G" % 10).should == "00010"
    ("%G" % 10**10).should == "1E+10"
    ("%*G" % [10, 9]).should == "         9"
  end

  it "supports octal formats using %o for positive numbers" do
    ("%o" % 10).should == "12"
    ("% o" % 10).should == " 12"
    ("%1$o" % [10, 20]).should == "12"
    ("%#o" % 10).should == "012"
    ("%+o" % 10).should == "+12"
    ("%-9o" % 10).should == "12       "
    ("%05o" % 10).should == "00012"
    ("%*o" % [10, 6]).should == "         6"
  end

  ruby_version_is ""..."1.9" do
    it "supports octal formats using %o for negative numbers" do
      # These are incredibly wrong. -05 == -5, not 7177777...whatever
      ("%o" % -5).should == "..73"
      ("%0o" % -5).should == "73"
      ("%.4o" % 20).should == "0024"
      ("%.1o" % -5).should == "73"
      ("%.7o" % -5).should == "7777773"
      ("%.10o" % -5).should == "7777777773"

      ("% o" % -26).should == "-32"
      ("%+o" % -26).should == "-32"
      ("%o" % -(2 ** 64 + 5)).should == "..75777777777777777777773"
    end
  end

  ruby_version_is "1.9" do
    it "supports octal formats using %o for negative numbers" do
      # These are incredibly wrong. -05 == -5, not 7177777...whatever
      ("%o" % -5).should == "..73"
      ("%0o" % -5).should == "..73"
      ("%.4o" % 20).should == "0024"
      ("%.1o" % -5).should == "..73"
      ("%.7o" % -5).should == "..77773"
      ("%.10o" % -5).should == "..77777773"

      ("% o" % -26).should == "-32"
      ("%+o" % -26).should == "-32"
      ("%o" % -(2 ** 64 + 5)).should == "..75777777777777777777773"
    end
  end

  it "supports inspect formats using %p" do
    ("%p" % 10).should == "10"
    ("%1$p" % [10, 5]).should == "10"
    ("%-22p" % 10).should == "10                    "
    ("%*p" % [10, 10]).should == "        10"
  end

  it "calls inspect on arguments for %p format" do
    obj = mock('obj')
    def obj.inspect() "obj" end
    ("%p" % obj).should == "obj"

    # undef is not working
    # obj = mock('obj')
    # class << obj; undef :inspect; end
    # def obj.method_missing(*args) "obj" end
    # ("%p" % obj).should == "obj"
  end

  it "taints result for %p when argument.inspect is tainted" do
    obj = mock('x')
    def obj.inspect() "x".taint end

    ("%p" % obj).tainted?.should == true

    obj = mock('x'); obj.taint
    def obj.inspect() "x" end

    ("%p" % obj).tainted?.should == false
  end

  it "supports string formats using %s" do
    ("%s" % "hello").should == "hello"
    ("%s" % "").should == ""
    ("%s" % 10).should == "10"
    ("%1$s" % [10, 8]).should == "10"
    ("%-5s" % 10).should == "10   "
    ("%*s" % [10, 9]).should == "         9"
  end

  it "respects a space padding request not as part of the width" do
    x = "% -5s" % ["foo"]
    x.should == "foo  "
  end

  it "calls to_s on non-String arguments for %s format" do
    obj = mock('obj')
    def obj.to_s() "obj" end

    ("%s" % obj).should == "obj"

    # undef doesn't work
    # obj = mock('obj')
    # class << obj; undef :to_s; end
    # def obj.method_missing(*args) "obj" end
    #
    # ("%s" % obj).should == "obj"
  end

  it "taints result for %s when argument is tainted" do
    ("%s" % "x".taint).tainted?.should == true
    ("%s" % mock('x').taint).tainted?.should == true
  end

  ruby_version_is ""..."2.0" do
    it "taints result for %s when argument is tainted float" do
      ("%s" % 0.0.taint).tainted?.should == true # float is frozen on 2.0
    end
  end

  # MRI crashes on this one.
  # See http://groups.google.com/group/ruby-core-google/t/c285c18cd94c216d
  it "raises an ArgumentError for huge precisions for %s" do
    block = lambda { "%.25555555555555555555555555555555555555s" % "hello world" }
    block.should raise_error(ArgumentError)
  end

  # Note: %u has been changed to an alias for %d in MRI 1.9 trunk.
  # Let's wait a bit for it to cool down and see if it will
  # be changed for 1.8 as well.
  it "supports unsigned formats using %u" do
    ("%u" % 10).should == "10"
    ("% u" % 10).should == " 10"
    ("%1$u" % [10, 20]).should == "10"
    ("%+u" % 10).should == "+10"
    ("%-7u" % 10).should == "10     "
    ("%04u" % 10).should == "0010"
    ("%*u" % [10, 4]).should == "         4"
  end

  ruby_version_is "" ... "1.9" do
    platform_is :wordsize => 64 do
      it "supports unsigned formats using %u on 64-bit" do
        ("%u" % -5).should == "..#{2**64 - 5}"
        ("%0u" % -5).should == (2**64 - 5).to_s
        ("%.1u" % -5).should == (2**64 - 5).to_s
        ("%.7u" % -5).should == (2**64 - 5).to_s
        ("%.10u" % -5).should == (2**64 - 5).to_s
      end
    end

    platform_is :wordsize => 32 do
      it "supports unsigned formats using %u on 32-bit" do
        ("%u" % -5).should == "..#{2**32 - 5}"
        ("%0u" % -5).should == (2**32 - 5).to_s
        ("%.1u" % -5).should == (2**32 - 5).to_s
        ("%.7u" % -5).should == (2**32 - 5).to_s
        ("%.10u" % -5).should == (2**32 - 5).to_s
      end
    end
  end

  it "formats negative values with a leading sign using %u" do
    ("% u" % -26).should == "-26"
    ("%+u" % -26).should == "-26"
  end

  ruby_version_is ""..."1.9" do
    # This is the proper, compliant behavior of both JRuby, and
    # MRI 1.8.6 with patchlevel greater than 114.
    ruby_bug "http://blade.nagaokaut.ac.jp/cgi-bin/scat.rb/ruby/ruby-core/8418", "1.8.6.114" do
      it "supports negative bignums by prefixing the value with dots" do
        ("%u" % -(2 ** 64 + 5)).should == "..79228162495817593519834398715"
      end
    end
  end

  ruby_version_is "1.9" do
    it "supports negative bignums with %u or %d" do
      ("%u" % -(2 ** 64 + 5)).should == "-18446744073709551621"
      ("%d" % -(2 ** 64 + 5)).should == "-18446744073709551621"
    end
  end

  it "supports hex formats using %x for positive numbers" do
    ("%x" % 10).should == "a"
    ("% x" % 10).should == " a"
    ("%1$x" % [10, 20]).should == "a"
    ("%#x" % 10).should == "0xa"
    ("%+x" % 10).should == "+a"
    ("%-9x" % 10).should == "a        "
    ("%05x" % 10).should == "0000a"
    ("%*x" % [10, 6]).should == "         6"
    ("%.4x" % 20).should == "0014"
    ("%x" % 0xFFFFFFFF).should == "ffffffff"
  end

  ruby_version_is ""..."1.9" do
    it "supports hex formats using %x for negative numbers" do
      ("%x" % -5).should == "..fb"
      ("%0x" % -5).should == "fb"
      ("%.4x" % 20).should == "0014"
      ("%.1x" % -5).should == "fb"
      ("%.7x" % -5).should == "ffffffb"
      ("%.10x" % -5).should == "fffffffffb"
      ("% x" % -26).should == "-1a"
      ("%+x" % -26).should == "-1a"
      ("%x" % 0xFFFFFFFF).should == "ffffffff"
      ("%x" % -(2 ** 64 + 5)).should == "..fefffffffffffffffb"
    end
  end

  ruby_version_is "1.9" do
    it "supports hex formats using %x for negative numbers" do
      ("%x" % -5).should == "..fb"
      ("%0x" % -5).should == "..fb"
      ("%.1x" % -5).should == "..fb"
      ("%.7x" % -5).should == "..ffffb"
      ("%.10x" % -5).should == "..fffffffb"
      ("% x" % -26).should == "-1a"
      ("%+x" % -26).should == "-1a"
      ("%x" % -(2 ** 64 + 5)).should == "..fefffffffffffffffb"
    end
  end

  it "supports hex formats using %X for positive numbers" do
    ("%X" % 10).should == "A"
    ("% X" % 10).should == " A"
    ("%1$X" % [10, 20]).should == "A"
    ("%#X" % 10).should == "0XA"
    ("%+X" % 10).should == "+A"
    ("%-9X" % 10).should == "A        "
    ("%05X" % 10).should == "0000A"
    ("%*X" % [10, 6]).should == "         6"
    ("%X" % 0xFFFFFFFF).should == "FFFFFFFF"
  end

  ruby_version_is "" ... "1.9" do
    it "supports hex formats using %X for negative numbers" do
      ("%X" % -5).should == "..FB"
      ("%0X" % -5).should == "FB"
      ("%.1X" % -5).should == "FB"
      ("%.7X" % -5).should == "FFFFFFB"
      ("%.10X" % -5).should == "FFFFFFFFFB"
      ("% X" % -26).should == "-1A"
      ("%+X" % -26).should == "-1A"
      ("%X" % 0xFFFFFFFF).should == "FFFFFFFF"
      ("%X" % -(2 ** 64 + 5)).should == "..FEFFFFFFFFFFFFFFFB"
    end
  end

  ruby_version_is "1.9" do
    it "supports hex formats using %X for negative numbers" do
      ("%X" % -5).should == "..FB"
      ("%0X" % -5).should == "..FB"
      ("%.1X" % -5).should == "..FB"
      ("%.7X" % -5).should == "..FFFFB"
      ("%.10X" % -5).should == "..FFFFFFFB"
      ("% X" % -26).should == "-1A"
      ("%+X" % -26).should == "-1A"
      ("%X" % -(2 ** 64 + 5)).should == "..FEFFFFFFFFFFFFFFFB"
    end
  end

  ruby_version_is "1.9" do
    it "formats zero without prefix using %#x" do
      ("%#x" % 0).should == "0"
    end

    it "formats zero without prefix using %#X" do
      ("%#X" % 0).should == "0"
    end
  end

  ruby_version_is "" ... "1.9" do
    it "formats zero with prefix using %#x" do
      ("%#x" % 0).should == "0x0"
    end

    it "formats zero without prefix using %#X" do
      ("%#X" % 0).should == "0X0"
    end
  end

  %w(b d i o u x X).each do |f|
    format = "%" + f

    it "behaves as if calling Kernel#Integer for #{format} argument, if it does not respond to #to_ary" do
      (format % "10").should == (format % Kernel.Integer("10"))
      (format % "0x42").should == (format % Kernel.Integer("0x42"))
      (format % "0b1101").should == (format % Kernel.Integer("0b1101"))
      (format % "0b1101_0000").should == (format % Kernel.Integer("0b1101_0000"))
      (format % "0777").should == (format % Kernel.Integer("0777"))
      lambda {
        # see [ruby-core:14139] for more details
        (format % "0777").should == (format % Kernel.Integer("0777"))
      }.should_not raise_error(ArgumentError)

      lambda { format % "0__7_7_7" }.should raise_error(ArgumentError)

      lambda { format % "" }.should raise_error(ArgumentError)
      lambda { format % "x" }.should raise_error(ArgumentError)
      lambda { format % "5x" }.should raise_error(ArgumentError)
      lambda { format % "08" }.should raise_error(ArgumentError)
      lambda { format % "0b2" }.should raise_error(ArgumentError)
      lambda { format % "123__456" }.should raise_error(ArgumentError)

      obj = mock('5')
      obj.should_receive(:to_i).and_return(5)
      (format % obj).should == (format % 5)

      obj = mock('6')
      obj.stub!(:to_i).and_return(5)
      obj.should_receive(:to_int).and_return(6)
      (format % obj).should == (format % 6)
    end

    # 1.9 raises a TypeError for Kernel.Integer(nil), so we version guard this
    # case
    ruby_version_is ""..."1.9" do
      it "behaves as if calling Kernel#Integer(nil) for format argument, if it does not respond to #to_ary" do
        %w(b d i o u x X).each do |f|
          format = "%" + f
          (format % nil).should == (format % Kernel.Integer(nil))
        end
      end
    end

    it "doesn't taint the result for #{format} when argument is tainted" do
      (format % "5".taint).tainted?.should == false
    end
  end

  %w(e E f g G).each do |f|
    format = "%" + f

    ruby_version_is "1.8.6.278" do
      it "tries to convert the passed argument to an Array using #to_ary" do
        obj = mock('3.14')
        obj.should_receive(:to_ary).and_return([3.14])
        (format % obj).should == (format % [3.14])
      end
    end

    it "behaves as if calling Kernel#Float for #{format} arguments, when the passed argument does not respond to #to_ary" do
      (format % 10).should == (format % 10.0)
      (format % "-10.4e-20").should == (format % -10.4e-20)
      (format % ".5").should == (format % 0.5)
      (format % "-.5").should == (format % -0.5)
      # Something's strange with this spec:
      # it works just fine in individual mode, but not when run as part of a group
      (format % "10_1_0.5_5_5").should == (format % 1010.555)

      (format % "0777").should == (format % 777)

      lambda { format % "" }.should raise_error(ArgumentError)
      lambda { format % "x" }.should raise_error(ArgumentError)
      lambda { format % "." }.should raise_error(ArgumentError)
      lambda { format % "10." }.should raise_error(ArgumentError)
      lambda { format % "5x" }.should raise_error(ArgumentError)
      lambda { format % "0b1" }.should raise_error(ArgumentError)
      lambda { format % "10e10.5" }.should raise_error(ArgumentError)
      lambda { format % "10__10" }.should raise_error(ArgumentError)
      lambda { format % "10.10__10" }.should raise_error(ArgumentError)

      obj = mock('5.0')
      obj.should_receive(:to_f).and_return(5.0)
      (format % obj).should == (format % 5.0)
    end
    ruby_version_is ""..."1.9.2" do
      it "behaves as if calling Kernel#Float for #{format} arguments, when the passed argument is hexadecimal string" do
        lambda { format % "0xA" }.should raise_error(ArgumentError)
      end
    end
    ruby_version_is "1.9.2" do
      it "behaves as if calling Kernel#Float for #{format} arguments, when the passed argument is hexadecimal string" do
        (format % "0xA").should == (format % 0xA)
      end
    end

    it "doesn't taint the result for #{format} when argument is tainted" do
      (format % "5".taint).tainted?.should == false
    end
  end
  
  ruby_version_is "1.9.2" do
    describe "when format string contains %{} sections" do
    
      it "replaces %{} sections with values from passed-in hash" do
        ("%{foo}bar" % {:foo => 'oof'}).should == "oofbar"
      end
      
      it "raises KeyError if key is missing from passed-in hash" do
        lambda {"%{foo}" % {}}.should raise_error(KeyError)
      end
      
      it "should raise ArgumentError if no hash given" do
        lambda {"%{foo}" % []}.should raise_error(ArgumentError)
      end
    end
    
    describe "when format string contains %<> formats" do
      it "uses the named argument for the format's value" do
        ("%<foo>d" % {:foo => 1}).should == "1"
      end
      
      it "raises KeyError if key is missing from passed-in hash" do
        lambda {"%<foo>d" % {}}.should raise_error(KeyError)
      end
      
      it "should raise ArgumentError if no hash given" do
        lambda {"%<foo>" % []}.should raise_error(ArgumentError)
      end
    end
  end
end
