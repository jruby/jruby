require File.dirname(__FILE__) + '/../spec_helper'

# %, *, +, <<, <=>, ==, =~, [], []=, capitalize, capitalize!,
# casecmp, center, chomp, chomp!, chop, chop!, concat, count, crypt,
# delete, delete!, downcase, downcase!, dump, each, each_byte,
# each_line, empty?, eql?, gsub, gsub!, hash, hex, include?, index,
# initialize_copy, insert, inspect, intern, length, ljust, lstrip,
# lstrip!, match, next, next!, oct, replace, reverse, reverse!,
# rindex, rjust, rstrip, rstrip!, scan, size, slice, slice!, split,
# squeeze, squeeze!, strip, strip!, sub, sub!, succ, succ!, sum,
# swapcase, swapcase!, to_f, to_i, to_s, to_str, to_sym, tr, tr!,
# tr_s, tr_s!, unpack, upcase, upcase!, upto

context "String instance method" do
  
  # specify "% should return a string resulting from applying self as a format specification to other" do
    # TODO: yeah, this should be broken up I dare say
    # example do
    #   [ "%b" % 10,
    #     "% b" % 10,
    #     "%1$b" % [10, 20],
    #     "%#b" % 10,
    #     "%+b" % 10,
    #     "%-9b" % 10,
    #     "%05b" % 10,
    #     "%*b" % [10, 6],
    # 
    #     "%c" % 10,
    #     "%2$c" % [10, 11, 14],
    #     "%-4c" % 10,
    #     "%*c" % [10, 3],
    # 
    #     "%d" % 10,
    #     "% d" % 10,
    #     "%1$d" % [10, 20],
    #     "%+d" % 10,
    #     "%-7d" % 10,
    #     "%04d" % 10,
    #     "%*d" % [10, 4],
    # 
    #     "%E" % 10,
    #     "% E" % 10,
    #     "%1$E" % 10,
    #     "%#E" % 10,
    #     "%+E" % 10,
    #     "%-7E" % 10,
    #     "%05E" % 10,
    #     "%*E" % [10, 9],
    #     
    #     "%e" % 10,
    #     "% e" % 10,
    #     "%1$e" % 10,
    #     "%#e" % 10,
    #     "%+e" % 10,
    #     "%-7e" % 10,
    #     "%05e" % 10,
    #     "%*e" % [10, 9],
    # 
    #     "%f" % 10,
    #     "% f" % 10,
    #     "%1$f" % 10,
    #     "%#f" % 10,
    #     "%+f" % 10,
    #     "%-7f" % 10,
    #     "%05f" % 10,
    #     "%*f" % [10, 9],
    # 
    #     "%G" % 10,
    #     "% G" % 10,
    #     "%1$G" % 10,
    #     "%#G" % 10,
    #     "%+G" % 10,
    #     "%-7G" % 10,
    #     "%05G" % 10,
    #     "%*G" % [10, 9],
    # 
    #     "%g" % 10,
    #     "% g" % 10,
    #     "%1$g" % 10,
    #     "%#g" % 10,
    #     "%+g" % 10,
    #     "%-7g" % 10,
    #     "%05g" % 10,
    #     "%*g" % [10, 9],
    # 
    #     "%i" % 10,
    #     "% i" % 10,
    #     "%1$i" % [10, 20],
    #     "%+i" % 10,
    #     "%-7i" % 10,
    #     "%04i" % 10,
    #     "%*i" % [10, 4],
    # 
    #     "%o" % 10,
    #     "% o" % 10,
    #     "%1$o" % [10, 20],
    #     "%#o" % 10,
    #     "%+o" % 10,
    #     "%-9o" % 10,
    #     "%05o" % 10,
    #     "%*o" % [10, 6],
    #     
    #     "%p" % 10,
    #     "%1$p" % [10, 5],
    #     "%-22p" % 10,
    #     "%*p" % [10, 10],
    # 
    #     "%s" % 10,
    #     "%1$s" % [10, 8],
    #     "%-5s" % 10,
    #     "%*s" % [10, 9],
    # 
    #     "%u" % 10,
    #     "% u" % 10,
    #     "%1$u" % [10, 20],
    #     "%+u" % 10,
    #     "%-7u" % 10,
    #     "%04u" % 10,
    #     "%*u" % [10, 4],
    # 
    #     "%X" % 10,
    #     "% X" % 10,
    #     "%1$X" % [10, 20],
    #     "%#X" % 10,
    #     "%+X" % 10,
    #     "%-9X" % 10,
    #     "%05X" % 10,
    #     "%*X" % [10, 6],
    # 
    #     "%x" % 10,
    #     "% x" % 10,
    #     "%1$x" % [10, 20],
    #     "%#x" % 10,
    #     "%+x" % 10,
    #     "%-9x" % 10,
    #     "%05x" % 10,
    #     "%*x" % [10, 6] ]
    # end.should == ["1010", " 1010", "1010", "0b1010", "+1010", "1010     ", "01010", "       110", 
    #       "\n", "\v", "\n   ", "         \003", "10", " 10", "10", "+10", "10     ", "0010", 
    #       "         4", "1.000000E+01", " 1.000000E+01", "1.000000E+01", "1.000000E+01", 
    #       "+1.000000E+01", "1.000000E+01", "1.000000E+01", "9.000000E+00", "1.000000e+01", 
    #       " 1.000000e+01", "1.000000e+01", "1.000000e+01", "+1.000000e+01", "1.000000e+01", 
    #       "1.000000e+01", "9.000000e+00", "10.000000", " 10.000000", "10.000000", "10.000000", 
    #       "+10.000000", "10.000000", "10.000000", "  9.000000", "10", " 10", "10", "10.0000", 
    #       "+10", "10     ", "00010", "         9", "10", " 10", "10", "10.0000", "+10", "10     ", 
    #       "00010", "         9", "10", " 10", "10", "+10", "10     ", "0010", "         4", 
    #       "12", " 12", "12", "012", "+12", "12       ", "00012", "         6", "10", "10", 
    #       "10                    ", "        10", "10", "10", "10   ", "         9", "10", 
    #       " 10", "10", "+10", "10     ", "0010", "         4", "A", " A", "A", "0XA", "+A", 
    #       "A        ", "0000A", "         6", "a", " a", "a", "0xa", "+a", "a        ", "0000a", "         6" ]
  # end

  specify "* should return a new string that is n copies of self" do
    ("cool" * 3).should == "coolcoolcool"
  end
  
  specify "+ should concatentate self with other" do
    ("Ruby !" + "= Rubinius").should == "Ruby != Rubinius"
  end
  
  specify "<< should concatenate the other object" do
    a = 'hello ' << 'world'
    b = a.dup << 33
    a.should == "hello world"
    b.should == "hello world!"
  end
  
  specify "<=> should return -1, 0, 1 when self is less than, equal, or greater than other" do
    ("this" <=> "those").should == -1
    ("yep" <=> "yep").should == 0
    ("yoddle" <=> "griddle").should == 1
  end

  specify "<=> returns 0 if strings are equal" do
    ("abc" <=> "abc").should == 0
  end

  specify "<=> considers string that comes lexicographically first to be less if strings have equal size" do
    ("aba" <=> "abc").should == -1
    ("abc" <=> "aba").should == 1
  end

  specify "<=> considers shorter string to be less if longer string starts with shorter one" do
    ("abc" <=> "abcd").should == -1
    ("abcd" <=> "abc").should == 1
  end

  specify "<=> compares shorter string with corresponding number of first chars of longer string" do
    ("abx" <=> "abcd").should == 1
    ("abcd" <=> "abx").should == -1
  end

  specify "<=> returns nil if other is not String" do
    ("abc" <=> 1).should == nil
    ("abc" <=> :abc).should == nil
    ("abc" <=> Object.new).should == nil
  end
  
  specify "== with other String should return true if <=> returns 0" do
    ("equal" == "equal").should == true
    ("more" == "MORE").should == false
    ("less" == "greater").should == false
  end
  
  specify "== with other not String should return self == other.to_str if other responds to to_str" do
    class Foo
      def to_str
      end
      def ==(other)
        "foo" == other
      end
    end
    ("foo" == Foo.new).should == true
  end
  
  specify "== with other not String should return false if other does not respond to to_str" do
    class Bar
      def ==(other)
        "foo" == other
      end
    end
    ("foo" == Bar.new).should == false
  end
  
  specify "=== should be a synonym for ==" do
    ("equal" == "equal").should == true
    ("more" == "MORE").should == false
    ("less" == "greater").should == false
  end
  
  specify "=~ should return the position of match start" do
    ("rudder" =~ /udder/).should == 1
    ("boat" =~ /[^fl]oat/).should == 0
  end
  
  specify "=~ should return nil if there is no match" do
    ("bean" =~ /bag/).should == nil
    ("true" =~ /false/).should == nil
  end
  
  specify "[] with index should return the code of the character at index" do
    "hello"[1].should == 101
  end
  
  specify "[] with start, length should return the substring of length characters begin at start" do
    "hello"[1, 3].should == "ell"
    "hello"[-3, 2].should == "ll"
    "hello"[5, 7].should == ""
  end
  
  specify "[] with range should return the substring specified by range" do
    "world"[0..2].should == "wor"
    "world"[-4..-2].should == "orl"
    "world"[1...3].should == "or"
  end
  
  specify "[] with regexp should return the string matching pattern" do
    "hello there"[/[aeiou](.)\1/].should == "ell"
    "hello there"[/ell/].should == "ell"
    "hello there"[/o /].should == "o "
    "hello there"[/d/].should == nil
  end
  
  specify "[] with regexp, index should return the string specified by the nth component MatchData" do
    "hello there"[/[aeiou](.)\1/, 0].should == "ell"
    "hello there"[/he/, 1].should == nil
    "hello there"[/he/, 0].should == "he"
    "hello there"[/[aeiou](.)\1/, 2].should == nil
  end
  
  specify "[] with string should return other if it occurs in self, otherwise nil" do
    "hello"["el"].should == "el"
    "hello"["bye"].should == nil
    "hello"["hello"].should == "hello"
  end
  
  specify "[]= with index should replace the character at index with the character code" do
    s = "barf"
    (s[3] = ?k).should == 107
    s.should == "bark"
  end
  
  specify "[]= with index should replace the character at index with string" do
    s = "Ruby"
    (s[3] = "inius").should == "inius"
    s.should == "Rubinius"
  end
  
  specify "[]= with start, length should replace length characters at start with string" do
    r = 'once upon a time'
    r.send(:[]=,5,11,'a lifetime').should == "a lifetime"
    r.should == "once a lifetime"
  end
  
  specify "[]= with range should replace characters in range with string" do
    s = "drabble"
    (s[3..6] = "pe").should == "pe"
    s.should == "drape"
  end
  
  specify "[]= with regexp should replace the characters that match pattern with string" do
    s = "caddie"
    (s[/d{2}/] = "bb").should == "bb"
    s.should == "cabbie"
  end

  specify "[]= with regexp and index should replace the specified portion of the match with string" do
    r = "abc123def411"
    r.send(:[]=,/(\d+)([a-e]*)/, 1, "abcc").should == "abcc"
    r.should == "abcabccdef411"
  end

  specify "[]= with string should replace the string with other string" do
    s = "ten times"
    (s["ten"] = "twenty").should == "twenty"
    s.should == "twenty times"
  end
  
  specify "~ should be equivalent to $_ =~ self" do
    $_ = "bat"
    (~ /c?at/).should == 1
  end

  specify "capitalize should return a copy of string and convert the first character to uppercase and the rest to lowercase" do
    a = "LOWER"
    b = "MenTaLguY"
    a.capitalize.should == "Lower"
    a.should == "LOWER"
    b.capitalize.should == "Mentalguy"
    b.should == "MenTaLguY"
  end
  
  specify "capitalize! should modify self to convert the first character to upper case and the rest to lowercase" do
    a = "this"
    b = "THIS"
    a.capitalize!.should == "This"
    a.should == "This"
    b.should == "THIS"
    b.capitalize!.should == "This"
  end

  specify "casecmp should be a case-insensitive version of <=>" do
    "One".casecmp("one").should == 0
    "Two".casecmp("too").should == 1
    "MINE".casecmp("nINE").should == -1
  end

  specify "center should return a string padded on both sides" do
    "one".center(9,'.').should == "...one..."
    "two".center(5).should == " two "
    "middle".center(13,'-').should == "---middle----"
  end
  
  specify "center should return a string when length of self is greater than argument" do
    "this".center(3).should == "this"
    "radiology".center(8, '-').should == "radiology"
  end

  specify "chomp should return a new string with the given record separator removed from the end" do
    s = "one\n"
    s.chomp.should == "one"
    s.should == "one\n"
    t = "two\r\n"
    t.chomp.should == "two"
    t.should == "two\r\n"
    u = "three\r"
    u.chomp.should == "three"
    u.should == "three\r"
    v = "four"
    v.chomp.should == "four"
  end

  specify "chomp! should modify self to remove the given record separator from the end" do
    s = "one\n"
    s.chomp!
    s.should == "one"
    t = "two\r\n"
    t.chomp!
    t.should == "two"
    u = "three\r"
    u.chomp!
    u.should == "three"
    v = "four"
    v.chomp!.should == nil
    v.should == "four"
    w = "four\n\r"
    w.chomp!
    w.should == "four\n"
  end
  
  specify "chomp! should return nil if no changes are made" do
    "".chomp!.should == nil
    "line".chomp!.should == nil
  end

  specify "chop should return a new string with the last character removed" do
    s = "block"
    s.chop.should == "bloc"
    s.should == "block"
  end

  specify "chop! should modify self to remove the last character" do
    "ouch".chop!.should == "ouc"
  end
  
  specify "chop! should return nil if no changes are made" do
    "".chop!.should == nil
  end

  specify "concat should be a synonym for <<" do
    a = 'hello '
    a.concat('world')
    b = a.dup
    b.concat(33)
    a.should == "hello world"
    b.should == "hello world!"
  end

  specify "count should return the count of characters specified by the union of the arguments" do
    a = "hello world" 
    a.count("lo").should == 5
    a.count("lo", "o").should == 2
    a.count("hello", "^l").should == 4
    a.count("ej-m").should == 4
  end

  specify "crypt should return a one-way cryptographic hash of self using the standard libary function crypt" do
    "secret".crypt("mysterious").should == "my6b6UoW5FeNw"
  end

  # specify "delete should return a copy of self removing all characters in the intersection of its arguments" do
  #   "hello".delete("l","lo").should == "heo"
  #   "hello".delete("lo").should == "he"
  #   "hello".delete("aeiou", "^e").should == "hell"
  #   "hello".delete("ej-m").should == "ho"
  # end
  # 
  # specify "delete! should perform method delete in place on self" do
  #   s = "hello"
  #   s.delete!("l", "lo").should == "heo"
  #   s.should == "heo"
  # 
  #   t = "world"
  #   t.delete!("aeiou", "^e").should == "wrld"
  #   t.should == "wrld"
  # end
  # 
  # specify "delete! should return nil if no changes are made" do
  #   "help".delete!("q", "r", "biff").should == nil
  # end
  # 
  specify "downcase should return a copy of self with A-Z converted to a-z" do
    "ABCDEFGHIJKLMNOPQRSTUVWXYZ".downcase.should == "abcdefghijklmnopqrstuvwxyz"
  end

  specify "downcase! should perform downcase in place on self" do
    s = "MenTaLguY"
    s.downcase!.should == "mentalguy"
    s.should == "mentalguy"
  end
  
  specify "downcase! should return nil if no changes are made" do
    "lower case".downcase!.should == nil
  end

  specify "dump should return a string with all non-printing characters replaced with \\nnn notation" do
    ("\000".."A").to_a.to_s.dump.should == "\"\\000\\001\\002\\003\\004\\005\\006\\a\\b\\t\\n\\v\\f\\r\\016\\017\\020\\021\\022\\023\\024\\025\\026\\027\\030\\031\\032\\e\\034\\035\\036\\037 !\\\"\\\#$%&'()*+,-./0123456789\""
  end

  specify "each should split self on the argument ($/ default) and pass each substring to block" do
    a = []
    "once\nup\na\ntime\n".each { |s| a << s }
    a.should == ["once\n", "up\n", "a\n", "time\n"]
  end
  
  specify "each_byte should pass each byte to block" do
    a = []
    "Rubinius".each_byte { |b| a << b }
    a.should == [82, 117, 98, 105, 110, 105, 117, 115]
  end

  specify "each_line should be a synonym for each" do
    a = []
    "once\nup\na\ntime\n".each { |s| a << s }
    a.should == ["once\n", "up\n", "a\n", "time\n"]
  end

  specify "empty? should return true if self is sero-length" do
    "".empty?.should == true
    "Ruby".empty?.should == false
  end

  specify "eql? should return true if other is a String with identical contents" do
    "".eql?("").should == true
    "Rubinius".eql?(Fixnum).should == false
    "1".eql?(1).should == false
  end
  
  specify "gsub should return a string with all instances of pattern replaced by replacement" do
    "abracadabra".gsub("a", "xu").should == "xubrxucxudxubrxu"
  end
  
  specify "gsub with block should return a string with all instances of pattern replaced by the value of block" do
    "every which way to grab one".gsub!(/[aeiou]/) { |s| s.succ }.should == "fvfry whjch wby tp grbb pnf"
  end

  specify "gsub! should perform gsub in place on self" do
    s = "Ruby Ruby Ruby"
    s.gsub!(/Ru/, "cu").should == "cuby cuby cuby"
    s.should == "cuby cuby cuby"
  end
  
  specify "gsub! should return nil if no changes are made" do
    "".gsub!(/./, 'R').should == nil
  end

  specify "hash should be provided" do
    "".respond_to?(:hash).should == true
  end

  specify "hex should convert the leading characters (0x optional) to a number base 16" do
    "0x1".hex.should == 1
    "0x".hex.should == 0
    "0x10".hex.should == 16
    "ffb3".hex.should == 65459
  end
  
  specify "hex should return 0 when characters are not a hexadecimal representation" do
    "x0".hex.should == 0
    "Jupiter".hex.should == 0
  end

  specify "include? should return true when self contains other string or character" do
    s = "abracadabra"
    s.include?('a').should == true
    s.include?("ba").should == false
    s.include?("dab").should == true
    s.include?("bra").should == true
  end

  specify "index with fixnum should return the index of the given character" do
    "hello".index(101).should == 1
    "hello".index(52).should == nil
  end
  
  specify "index with string should return the index of the beginning of string" do
    "hello".index('e').should == 1
    "hello".index('lo', -3).should == 3
    "hello".index('lo', -1).should == nil
    "hello".index('a').should == nil
  end
  
  specify "index with regexp should return the index of the beginning of pattern" do
    "hello".index(/[aeiou]/, -3).should == 4
    "hello".index(/he/, 2).should == nil
    "hello".index(/el/).should == 1
  end

  specify "index with substring arg should return substring when it exists" do
    "hello".index('e').should == 1
  end

  specify "initialize_copy should return a new string initialized with other" do
    "".send(:initialize_copy, "Dolly").should == "Dolly"
  end
  
  specify "insert with positive index should insert other string before the character at index" do
    "rinius".insert(1,"ub").should == "rubinius"
    "p".insert(0,"lis").should == "lisp"
  end
  
  specify "insert with negative index should insert other string after the charracter at index" do
    "to".insert(-1,"day").should == "today"
    "hosal".insert(-3,"pit").should == "hospital"
  end

  specify "inspect should return a quoted string suitable to eval" do
    "Rubinius".inspect.should == "\"Rubinius\""
    "Ruby".inspect.should == "\"Ruby\""
  end

  specify "intern should return the symbol corresponding to self" do
    "intern".intern.should == :intern
    "rubinius".intern.should == :rubinius
  end

  specify "length should return the length of self" do
    "".length.should == 0
    "one".length.should == 3
    "two".length.should == 3
    "three".length.should == 5
    "four".length.should == 4
  end

  specify "ljust should return a string left justified in width characters padded on the right" do
    "one".ljust(4).should == "one "
    "two".ljust(5).should == "two  "
    "three".ljust(6).should == "three "
    "four".ljust(7).should == "four   "
  end
  
  specify "ljust should return self if length of self is greater than width" do
    "five".ljust(3).should == "five"
  end

  specify "lstrip should return self with leading whitespace characters removed" do
    "".lstrip.should == ""
    " hello ".lstrip.should == "hello "
    "hello".lstrip.should == "hello"
  end

  specify "lstrip! should modify self removing leading whitespace characters" do
    s = "\n\t This \000"
    s.lstrip!.should == "This \000"
    t = " another one"
    t.lstrip!.should == "another one"
    u = "  two  "
    u.lstrip!.should == "two  "
  end
  
  specify "lstrip! should return nil if no changes were made" do
    "this".lstrip!.should == nil
  end

  specify "match should convert pattern to a Regexp and invoke its match method on self" do
    "backwards".match("ack").to_s.should == "ack"
  end

  specify "next should be a synonym for succ" do
    "abcd".succ.should == "abce"
    "THX1138".succ.should == "THX1139"
    "<<koala>>".succ.should == "<<koalb>>"
    "1999zzz".succ.should == "2000aaa"
    "ZZZ9999".succ.should == "AAAA0000"
    "***".succ.should == "**+"
  end

  specify "next! should be a synonym for succ!" do
    a = "abcd"
    a.succ!.should == "abce"
    a.should == "abce"
    b = "THX1138"
    b.succ!.should == "THX1139"
    b.should == "THX1139"
    c = "<<koala>>"
    c.succ!.should == "<<koalb>>"
    c.should == "<<koalb>>"
    d = "1999zzz"
    d.succ!.should == "2000aaa"
    d.should == "2000aaa"
    e = "ZZZ9999"
    e.succ!.should == "AAAA0000"
    e.should == "AAAA0000"
    f = "***"
    f.succ!.should == "**+"
    f.should == "**+"
  end

  specify "oct should convert the leading characters (+/- optional) to a number base 8" do
    "0845".oct.should == 0
    "7712".oct.should == 4042
    "-012345678".oct.should == -342391
  end

  specify "replace should replace the contents of self with other" do
    a = "me"
    b = "you"
    "".replace(a).should == "me"
    a.replace(b).should == "you"
    b.replace("we").should == "we"
  end
  
  specify "replace should copy the taintedness of other" do
    a = "tainted"
    a.taint
    a.should == "tainted"
    b = "pure"
    c = "replace"
    c.replace(a)
    c.should == "tainted"
    c.tainted?.should == true
    "".replace(b).should == "pure"
    b.tainted?.should == false
  end

  specify "reverse should reverse the order of the characters" do
    "0123456789".reverse.should == "9876543210"
    "M".reverse.should == "M"
    "".reverse.should == ""
  end
  
  specify "reverse! should reverse the order of the characters and return self" do
    a = "detsalb"
    b = a.reverse!
    a.should == "blasted"
    b.should == "blasted"
    a.equal?(b).should == true
  end
  
  specify "rindex with integer should return the last index of the character" do
    "babble".rindex(?b).should == 3
  end
  
  specify "rindex with string should return the last index of the substring" do
    "wobble".rindex("b").should == 3
  end

  specify "rindex with regexp should return the last index of the substring" do
    "abbadabba".rindex(/ba/).should == 7
  end
  
  specify "rjust should return a string right justified in width characters padded on the left" do
    "more".rjust(10).should == "      more"
    "less".rjust(11, '+').should == "+++++++less"
  end
  
  specify "rjust should return self if length of self is greater than width" do
    "batter".rjust(4).should == "batter"
  end

  specify "rstrip should return a string first removing trailing \\000 characters and then removing trailing spaces" do
    " hello ".rstrip.should == " hello"
    "\tgoodbye\r\n".rstrip.should == "\tgoodbye"
  end

  specify "rstrip! should modify self by first removing trailing \\000 characters and then removing trailing spaces " do
    s = " hello "
    t = "\tgoodbye\r\n"
    u = "goodbye\000"
    s.rstrip!.should == " hello"
    t.rstrip!.should == "\tgoodbye"
    u.rstrip!.should == "goodbye"
  end
  
  specify "rstrip! should return nil if no changes are made" do
    "rstrip".rstrip!.should == nil
  end

  specify "scan should return an array containing each substring matching pattern" do
    "hello, rubinius world".scan(/[aeo][ralf]/).should == ["el", "or"]
  end
  
  specify "scan with block should pass each substring matching pattern to block" do
    a = []
    "abbadabbadoo".scan(/[db]./) { |s| a << s }
    a.should == ["bb", "da", "bb", "do"]
  end

  specify "size should be a synonym for length" do
    "".size.should == 0
    "one".size.should == 3
    "two".size.should == 3
    "three".size.should == 5
    "four".size.should == 4
  end

  specify "slice should be a synonym for []" do
    "hello".slice(1).should == 101
    "hello".slice(-3, 2).should == "ll"
    "world".slice(-4..-2).should == "orl"
    "hello there".slice(/[aeiou](.)\1/).should == "ell"
    "hello there".slice(/[aeiou](.)\1/, 0).should == "ell"
    "hello".slice("bye").should == nil
    "baz.rb".slice(2, 4).should == "z.rb"
    "z.rb".slice(1, 1).should == "."
  end
  
  specify "slice! should remove and return the specified portion from self" do
    s = "hello"
    s.slice!(1).should == 101
    r = "hello"
    r.slice!(-3, 2).should == "ll"
    t = "world"
    t.slice!(-4..-2).should == "orl"
    u = "hello there"
    u.slice!(/[aeiou](.)\1/).should == "ell"
    v = "hello there"
    v.slice!(/[aeiou](.)\1/, 0).should == "ell"
    w = "hello"
    w.slice!("bye").should == nil
    x = "baz.rb"
    x.slice!(2, 4).should == "z.rb"
    y = "z.rb"
    y.slice!(1, 1).should == "."
  end
  
  specify "slice! with index should return nil if the value is out of range" do
    "two".slice!(4).should == nil
  end
  
  specify "slice! with range should return nil if the value is out of range" do
    # BUG?: how does 3..6 touch string? 
    "one".slice!(3..6).should == ""
    "one".slice!(4..6).should == nil
  end
  
  specify "split with no argument should return an array of substrings separated by $;" do
    $;.nil?.should == true
    "hello world".split.should == ["hello", "world"]
    "hello\frubinius".split.should == ["hello", "rubinius"]
    ($; = 't').should == "t"
    "splitme".split.should == ["spli", "me"]
  end
  
  specify "split with string should return an array of substrings separated by string" do
    "hello".split('l').should == ["he", "", "o"]
    "hello".split('e').should == ["h", "llo"]
    "hello".split('h').should == ["", "ello"]
    "hello".split('el').should == ["h", "lo"]
    "hello".split('o').should == ["hell"]
    "hello".split('d').should == ["hello"]
  end
  
  specify "split with regexp should return an array of substrings separated by pattern" do
    "hello".split(/l/).should == ["he", "", "o"]
    "hello".split(/[aeiou]/).should == ["h", "ll"]
    "hello".split(/h/).should == ["", "ello"]
    "hello".split(/el/).should == ["h", "lo"]
    "hello".split(/[abcde]/).should == ["h", "llo"]
    "hello".split(/def/).should == ["hello"]
  end

  specify "split with a zero-width regexp should return an array of characters" do
    "blahblahblahblah".split(//).should == ["b", "l", "a", "h", "b", "l", "a", "h", "b", "l", "a", "h", "b", "l", "a", "h"]
    "blahblahblahblah".split(//,7).should == ["b", "l", "a", "h", "b", "l", "ahblahblah"]
  end

  specify "squeeze should return a string replacing runs of characters specified by the union of arguments with a single character" do
    "yellow moon".squeeze.should == "yelow mon"
    " now is the".squeeze(" ").should == " now is the"
    "putters putt balls".squeeze("m-z").should == "puters put balls"
  end

  specify "squeeze! should modify self in place by performing squeeze" do
    s = "yellow moon"
    t = "putters putt balls"
    s.squeeze!.should == "yelow mon"
    s.should == "yelow mon"
    t.squeeze!("m-z").should == "puters put balls"
    t.should == "puters put balls"
  end
  
  specify "squeeze! should return nil if no changes are made" do
    "squeeze".squeeze!("u", "sq").should == nil
  end

  specify "strip should return a string with trailing \\000, leading and trailing spaces removed" do
    " hello ".strip.should == "hello"
    "\tgoodbye\r\n".strip.should == "goodbye"
    "goodbye \000".strip.should == "goodbye"
  end

  specify "strip! should modify self to remove trailing \\000, and leading and trailing spaces" do
    s = "  strip  \000"
    r = "  rip \000\000"
    s.strip!.should == "strip"
    s.should == "strip"
    r.strip!.should == "rip" 
    r.should == "rip"
  end

  specify "sub should return a string with the first occurrence of pattern replaced with string" do
    "abcdbab".sub(/ab/, "qrs").should == "qrscdbab"
  end
  
  specify "sub with block should return a string replacing the first occurrence of pattern with the value of the block" do
    "test this".sub(/test/) { |i| "spec" }.should == "spec this"
  end

  specify "sub! should modify self to replace the first occurrence of pattern with string" do
    s = "abbaabba"
    r = "abbaabba"
    s.sub!(/a/,"bab").should == "babbbaabba"
    s.should == "babbbaabba"
    r.sub!(/ba+b/, "cab").should == "abcabba"
    r.should == "abcabba"
  end
  
  specify "sub! should return nil if no changes are made" do
    "abby".sub!(/ca/,"ba").should == nil
    "bad".sub!("c", "b").should == nil
  end

  specify "succ should return the string that is the successor of self" do
    "abcd".succ.should == "abce"
    "THX1138".succ.should == "THX1139"
    "<<koala>>".succ.should == "<<koalb>>"
    "1999zzz".succ.should == "2000aaa"
    "ZZZ9999".succ.should == "AAAA0000"
    "***".succ.should == "**+"
  end

  specify "succ! should modify self to be its successor" do
    a = "abcd"
    a.succ!.should == "abce"
    a.should == "abce"
    b = "THX1138"
    b.succ!.should == "THX1139"
    b.should == "THX1139"
    c = "<<koala>>"
    c.succ!.should == "<<koalb>>"
    c.should == "<<koalb>>"
    d = "1999zzz"
    d.succ!.should == "2000aaa"
    d.should == "2000aaa"
    e = "ZZZ9999"
    e.succ!.should == "AAAA0000"
    e.should == "AAAA0000"
    f = "***"
    f.succ!.should == "**+"
    f.should == "**+"
  end

  specify "sum should return a basic n-bit checksum of self with default n = 16" do
    "ruby".sum.should == 450
    "ruby".sum(10).should == 450
    "rubinius".sum(23).should == 881
  end

  specify "swapcase should return a string with lowercase characters converted to uppercase and vice versa" do
    "MenTaLguY".swapcase.should == "mENtAlGUy"
  end

  specify "swapcase! should modify self to convert lowercase characters to uppercase and vice versa" do
    s = "MenTaLguY"
    s.swapcase!.should == "mENtAlGUy"
    s.should == "mENtAlGUy"
  end
  
  specify "swapcase! should return nil if no changes were made" do
    "".swapcase!.should == nil
  end

  specify "to_f should return convert the leading characters to a floating-point number" do
    "0".to_f.should == 0.0
    "0.1".to_f.should == 0.1
    ".14159".to_f.should == 0.14159
    "-3.14".to_f.should == -3.14
    "help".to_f.should == 0.0
    "1.upto".to_f.should == 1.0
  end

  specify "to_i should convert the string to an integer base (2, 8, 10, or 16)" do
    [ "12345".to_i,
      " 12345".to_i,
      "+12345".to_i,
      "-12345".to_i,
      " ".to_i,
      "hello".to_i,
      "99 red balloons".to_i,
      "0a".to_i,
      "".to_i,
      "0a".to_i(16),
      "0b1100101".to_i(0),
      "0B1100101".to_i(0),
      "0o1100101".to_i(0),
      "0O1100101".to_i(0),
      "0x1100101".to_i(0),
      "01100101".to_i(0),
      "1100101".to_i(0),
      "".to_i(0),
      "hello".to_i,
      "1100101".to_i(2),
      "0b1100101".to_i(2),
      "0B1000101".to_i(2),
      "0b".to_i(2),
      "1".to_i(2),
      "0o1110101".to_i(8),
      "0O1101101".to_i(8),
      "0o".to_i(8),
      "0".to_i(8),
      "1".to_i(8),
      "1100101".to_i(8),
      "1100101".to_i(10),
      "".to_i(10),
      "1100101".to_i(16),
      "0x1100101".to_i(16),
      "0x1100101".to_i(16),
      "0x".to_i(16),
      "1".to_i(16) ].should == [12345, 12345, 12345, -12345, 0, 0, 99, 0, 0, 10, 101, 101, 294977, 294977, 17826049, 294977, 1100101, 0, 0, 101, 101, 69, 0, 1, 299073, 295489, 0, 0, 1, 294977, 1100101, 0, 17826049, 17826049, 17826049, 0, 1]
  end
  
  specify "to_s should return self" do
    s = "self"
    s.to_s.should == "self"
    s.to_s.equal?(s).should == true
  end

  specify "to_str should be a synonym for to_s" do
    s = "self"
    s.should == "self"
    s.to_s.should == "self"
    s.to_s.equal?(s).should == true
  end

  specify "to_sym should return a symbol created from self" do
    "ruby".to_sym.should == :ruby
    "rubinius".to_sym.should == :rubinius
  end

  specify "tr should replace characters in to_string with corresponding characters in from_string" do
    "Lisp".tr("Lisp", "Ruby").should == "Ruby"
  end
  
  specify "tr should accept c1-c2 notation to denote ranges of characters" do
    "123456789".tr("2-5","abcdefg").should == "1abcd6789"
  end
  
  specify "tr should accept from_string starting with ^ to denote all characters except those listed" do
    "123456789".tr("^345", "abc").should == "cc345cccc"
    "abcdefghijk".tr("^d-g", "9131").should == "111defg1111"
  end
  
  specify "tr should pad to_string with its last character if it is short than from_string" do
    "this".tr("this","x").should == "xxxx"
  end
  
  specify "tr! should modify self in place by performing tr on it" do
    s = "abcdefghijklmnopqR"
    s.tr!("cdefg", "12")
    s.should == "ab12222hijklmnopqR"
  end
  
  specify "tr_s should return a string processed according to tr with duplicate characters removed" do
    "Lisp".tr_s("Lisp", "Ruby").should == "Ruby"
    "123456789".tr_s("2-5","abcdefg").should == "1abcd6789"
    "this".tr_s("this","x").should == "x"
    "abcdefghijklmnopqR".tr_s("cdefg", "12").should == "ab12hijklmnopqR"
  end
  
  specify "tr_s! should modify self in place by applying tr_s" do
    a = "54321"
    a.tr_s!("432", "ab").should == "5ab1"
    a.should == "5ab1"
  
    b = "Ruby"
    b.tr_s!("R", "c").should == "cuby"
    b.should == "cuby"
  
    c = "chocolate"
    c.tr_s!("oa", "").should == "chclte"
    c.should == "chclte"
  end
  
  specify "unpack should return an array by decoding self according to the format string" do
    "abc \0\0abc \0\0".unpack('A6Z6').should == ["abc", "abc "]
    "abc \0\0".unpack('a3a3').should == ["abc", " \000\000"]
    "abc \0abc \0".unpack('Z*Z*').should == ["abc ", "abc "]
    "aa".unpack('b8B8').should == ["10000110", "01100001"]
    "aaa".unpack('h2H2c').should == ["16", "61", 97]
    "\xfe\xff\xfe\xff".unpack('sS').should == [-2, 65534]
    "now=20is".unpack('M*').should == ["now is"]
    "whole".unpack('xax2aX2aX1aX2a').should == ["h", "e", "l", "l", "o"]
  end

  specify "upcase should return a string with a-z characters replaced with A-Z" do
    "123Abc456dEf".upcase.should == "123ABC456DEF"
  end

  specify "upcase! should modify self in place by applying upcase" do
    a = "MenTaLguY"
    b = "ruby"
    a.upcase!.should == "MENTALGUY"
    a.should == "MENTALGUY"
    b.upcase!.should == "RUBY"
    b.should == "RUBY"
  end
  
  specify "upcase! should return nil if no changes are made" do
    "UPCASE".upcase!.should == nil
  end

  specify "upto should use String#succ to iterate from self to other passing each string to block" do
    a = []
    "*+".upto("*3") { |s| a << s }
    a.should == ["*+", "*,", "*-", "*.", "*/", "*0", "*1", "*2", "*3"]
  end

  specify "upto calls block with self if stop equals to self" do
    a = []
    "abc".upto("abc") { |s| a << s }
    a.should == ["abc"]
  end

  # This is weird but MRI behaves like that
  specify "upto calls block with self even if self is less than stop but stop length is less than self length" do
    a = []
    "25".upto("5") { |s| a << s }
    a.should == ["25"]
  end

  specify "upto doesn't call block if stop is less than self and stop length is less than self length" do
    a = []
    "25".upto("1") { |s| a << s }
    a.should == []
  end

  specify "upto doesn't call block if stop is less than self" do
    a = []
    "5".upto("2") { |s| a << s }
    a.should == []
  end

  specify "upto stops if current value is longer than stop value" do
    a = []
    "0".upto("A") { |s| a << s }
    a.should == ["0", "1", "2", "3", "4", "5", "6", "7", "8", "9"]
  end

  specify "upto returns self" do
    ("abc".upto("abd") {  }).should == "abc"
    ("5".upto("2") {  }).should == "5"
    ("25".upto("5") {  }).should == "25"
  end

  specify "upto converts stop to string with to_str if stop is not String" do
    class A; def to_str; "abd"; end; end

    a = []
    "abc".upto(A.new) { |s| a << s }
    a.should == ["abc", "abd"]
  end

  specify "upto raises TypeError if stop is not String or respond to to_str" do
    should_raise(TypeError) { "abc".upto(123) { } }
    should_raise(TypeError) { "abc".upto(:def) { } }
    should_raise(TypeError) { "abc".upto(Object.new) { } }
  end

  specify "upto raises LocalJumpError if no block was given" do
    should_raise(LocalJumpError) { "abc".upto("def") }
  end

  # MRI compatibility
  specify "upto checks for TypeError before checking if block is given" do
    should_raise(TypeError) { "abc".upto(123) }
  end
end

context "String inherited instance method" do
  specify "instance_variable_get should return the value of the instance variable" do
    s = "this"
    s.instance_variable_set(:@c, "that")
    s.instance_variable_get(:@c).should == "that"
  end
  
  specify "instance_variable_get should return nil if the instance variable does not exist" do
    "another".instance_variable_get(:@c).should == nil
  end
  
  specify "instance_variable_get should raise NameError if the argument is not of form '@x'" do
    should_raise(NameError) { "raise".instance_variable_get(:c) }
  end
end

describe "String instance method %" do
  
  it "format with multiple expressions" do
    ("%b %x %d %s" % [10, 10, 10, 10]).should == "1010 a 10 10"
    # Sprintf::Parser.format("%b %x %d %s", [10, 10, 10, 10]).should == "1010 a 10 10"
  end
  
  it "format with expressions mid string" do
    ("hello %s!" % "world").should == "hello world!"
  end
  
  it "format correctly parses %%" do
    ("%d%% %s" % [10, "of chickens!"]).should == "10% of chickens!"
  end
  
  it "format binary values should return a string resulting from applying the format" do
    ("%b" % 10).should == "1010"
    ("% b" % 10).should == " 1010"
    ("%1$b" % [10, 20]).should == "1010"
    ("%#b" % 10).should == "0b1010"
    ("%+b" % 10).should == "+1010"
    ("%-9b" % 10).should == "1010     "
    ("%05b" % 10).should == "01010"
    ("%*b" % [10, 6]).should == "       110"
  end
  
  it "format character values should return a string resulting from applying the format" do
    ("%c" % 10).should == "\n"
    ("%2$c" % [10, 11, 14]).should == "\v"
    ("%-4c" % 10).should == "\n   "
    ("%*c" % [10, 3]).should == "         \003"
  end
  
  it "format decimal values should return a string resulting from applying the format" do
    ("%d" % 10).should == "10"
    ("% d" % 10).should == " 10"
    ("%1$d" % [10, 20]).should == "10"
    ("%+d" % 10).should == "+10"
    ("%-7d" % 10).should == "10     "
    ("%04d" % 10).should == "0010"
    ("%*d" % [10, 4]).should == "         4"
  end
  
  it "format float (E) values should return a string resulting from applying the format" do
    ("%E" % 10).should == "1.000000E+01"
    ("% E" % 10).should == " 1.000000E+01"
    ("%1$E" % 10).should == "1.000000E+01"
    ("%#E" % 10).should == "1.000000E+01"
    ("%+E" % 10).should == "+1.000000E+01"
    ("%-7E" % 10).should == "1.000000E+01"
    ("%05E" % 10).should == "1.000000E+01"
    ("%*E" % [10, 9]).should == "9.000000E+00"
  end

  it "format float (e) values should return a string resulting from applying the format" do  
    ("%e" % 10).should == "1.000000e+01"
    ("% e" % 10).should == " 1.000000e+01"
    ("%1$e" % 10).should == "1.000000e+01"
    ("%#e" % 10).should == "1.000000e+01"
    ("%+e" % 10).should == "+1.000000e+01"
    ("%-7e" % 10).should == "1.000000e+01"
    ("%05e" % 10).should == "1.000000e+01"
    ("%*e" % [10, 9]).should == "9.000000e+00"
    ("%e" % (0.0/0)).should == "nan"
  end
  
  it "format float (f) values should return a string resulting from applying the format" do
    ("%f" % 10).should == "10.000000"
    ("% f" % 10).should == " 10.000000"
    ("%1$f" % 10).should == "10.000000"
    ("%#f" % 10).should == "10.000000"
    ("%+f" % 10).should == "+10.000000"
    ("%-7f" % 10).should == "10.000000"
    ("%05f" % 10).should == "10.000000"
    ("%*f" % [10, 9]).should == "  9.000000"
  end
  
  it "format float (G) values should return a string resulting from applying the format" do
    ("%G" % 10).should == "10"
    ("% G" % 10).should == " 10"
    ("%1$G" % 10).should == "10"
    ("%#G" % 10).should == "10.0000"
    ("%+G" % 10).should == "+10"
    ("%-7G" % 10).should == "10     "
    ("%05G" % 10).should == "00010"
    ("%*G" % [10, 9]).should == "         9"
  end
  
  it "format float (g) values should return a string resulting from applying the format" do
    ("%g" % 10).should == "10"
    ("% g" % 10).should == " 10"
    ("%1$g" % 10).should == "10"
    ("%#g" % 10).should == "10.0000"
    ("%+g" % 10).should == "+10"
    ("%-7g" % 10).should == "10     "
    ("%05g" % 10).should == "00010"
    ("%*g" % [10, 9]).should == "         9"
  end
  
  it "format integer values should return a string resulting from applying the format" do
    ("%i" % 10).should == "10"
    ("% i" % 10).should == " 10"
    ("%1$i" % [10, 20]).should == "10"
    ("%+i" % 10).should == "+10"
    ("%-7i" % 10).should == "10     "
    ("%04i" % 10).should == "0010"
    ("%*i" % [10, 4]).should == "         4"
  end
  
  it "format octal values should return a string resulting from applying the format" do
    ("%o" % 10).should == "12"
    ("% o" % 10).should == " 12"
    ("%1$o" % [10, 20]).should == "12"
    ("%#o" % 10).should == "012"
    ("%+o" % 10).should == "+12"
    ("%-9o" % 10).should == "12       "
    ("%05o" % 10).should == "00012"
    ("%*o" % [10, 6]).should == "         6"
  end
  
  it "format inspect values should return a string resulting from applying the format" do
    ("%p" % 10).should == "10"
    ("%1$p" % [10, 5]).should == "10"
    ("%-22p" % 10).should == "10                    "
    ("%*p" % [10, 10]).should == "        10"
  end
  
  it "format string values should return a string resulting from applying the format" do
    ("%s" % 10).should == "10"
    ("%1$s" % [10, 8]).should == "10"
    ("%-5s" % 10).should == "10   "
    ("%*s" % [10, 9]).should == "         9"
  end
  
  it "format unsigned values should return a string resulting from applying the format" do
    ("%u" % 10).should == "10"
    ("% u" % 10).should == " 10"
    ("%1$u" % [10, 20]).should == "10"
    ("%+u" % 10).should == "+10"
    ("%-7u" % 10).should == "10     "
    ("%04u" % 10).should == "0010"
    ("%*u" % [10, 4]).should == "         4"
  end
  
  it "format hex (X) values should return a string resulting from applying the format" do
    ("%X" % 10).should == "A"
    ("% X" % 10).should == " A"
    ("%1$X" % [10, 20]).should == "A"
    ("%#X" % 10).should == "0XA"
    ("%+X" % 10).should == "+A"
    ("%-9X" % 10).should == "A        "
    ("%05X" % 10).should == "0000A"
    ("%*X" % [10, 6]).should == "         6"
  end
  
  it "format hex (x) values should return a string resulting from applying the format" do
    ("%x" % 10).should == "a"
    ("% x" % 10).should == " a"
    ("%1$x" % [10, 20]).should == "a"
    ("%#x" % 10).should == "0xa"
    ("%+x" % 10).should == "+a"
    ("%-9x" % 10).should == "a        "
    ("%05x" % 10).should == "0000a"
    ("%*x" % [10, 6]).should == "         6"
  end
end
