require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes.rb', __FILE__)

describe "String#sub with pattern, replacement" do
  it "returns a copy of self with all occurrences of pattern replaced with replacement" do
    "hello".sub(/[aeiou]/, '*').should == "h*llo"
    "hello".sub(//, ".").should == ".hello"
  end

  it "ignores a block if supplied" do
    "food".sub(/f/, "g") { "w" }.should == "good"
  end

  it "supports \\G which matches at the beginning of the string" do
    "hello world!".sub(/\Ghello/, "hi").should == "hi world!"
  end

  it "supports /i for ignoring case" do
    "Hello".sub(/h/i, "j").should == "jello"
    "hello".sub(/H/i, "j").should == "jello"
  end

  it "doesn't interpret regexp metacharacters if pattern is a string" do
    "12345".sub('\d', 'a').should == "12345"
    '\d'.sub('\d', 'a').should == "a"
  end

  it "replaces \\1 sequences with the regexp's corresponding capture" do
    str = "hello"

    str.sub(/([aeiou])/, '<\1>').should == "h<e>llo"
    str.sub(/(.)/, '\1\1').should == "hhello"

    str.sub(/.(.?)/, '<\0>(\1)').should == "<he>(e)llo"

    str.sub(/.(.)+/, '\1').should == "o"

    str = "ABCDEFGHIJKL"
    re = /#{"(.)" * 12}/
    str.sub(re, '\1').should == "A"
    str.sub(re, '\9').should == "I"
    # Only the first 9 captures can be accessed in MRI
    str.sub(re, '\10').should == "A0"
  end

  it "treats \\1 sequences without corresponding captures as empty strings" do
    str = "hello!"

    str.sub("", '<\1>').should == "<>hello!"
    str.sub("h", '<\1>').should == "<>ello!"

    str.sub(//, '<\1>').should == "<>hello!"
    str.sub(/./, '\1\2\3').should == "ello!"
    str.sub(/.(.{20})?/, '\1').should == "ello!"
  end

  it "replaces \\& and \\0 with the complete match" do
    str = "hello!"

    str.sub("", '<\0>').should == "<>hello!"
    str.sub("", '<\&>').should == "<>hello!"
    str.sub("he", '<\0>').should == "<he>llo!"
    str.sub("he", '<\&>').should == "<he>llo!"
    str.sub("l", '<\0>').should == "he<l>lo!"
    str.sub("l", '<\&>').should == "he<l>lo!"

    str.sub(//, '<\0>').should == "<>hello!"
    str.sub(//, '<\&>').should == "<>hello!"
    str.sub(/../, '<\0>').should == "<he>llo!"
    str.sub(/../, '<\&>').should == "<he>llo!"
    str.sub(/(.)./, '<\0>').should == "<he>llo!"
  end

  it "replaces \\` with everything before the current match" do
    str = "hello!"

    str.sub("", '<\`>').should == "<>hello!"
    str.sub("h", '<\`>').should == "<>ello!"
    str.sub("l", '<\`>').should == "he<he>lo!"
    str.sub("!", '<\`>').should == "hello<hello>"

    str.sub(//, '<\`>').should == "<>hello!"
    str.sub(/..o/, '<\`>').should == "he<he>!"
  end

  it "replaces \\' with everything after the current match" do
    str = "hello!"

    str.sub("", '<\\\'>').should == "<hello!>hello!"
    str.sub("h", '<\\\'>').should == "<ello!>ello!"
    str.sub("ll", '<\\\'>').should == "he<o!>o!"
    str.sub("!", '<\\\'>').should == "hello<>"

    str.sub(//, '<\\\'>').should == "<hello!>hello!"
    str.sub(/../, '<\\\'>').should == "<llo!>llo!"
  end

  it "replaces \\\\\\+ with \\\\+" do
    "x".sub(/x/, '\\\+').should == "\\+"
  end

  it "replaces \\+ with the last paren that actually matched" do
    str = "hello!"

    str.sub(/(.)(.)/, '\+').should == "ello!"
    str.sub(/(.)(.)+/, '\+').should == "!"
    str.sub(/(.)()/, '\+').should == "ello!"
    str.sub(/(.)(.{20})?/, '<\+>').should == "<h>ello!"

    str = "ABCDEFGHIJKL"
    re = /#{"(.)" * 12}/
    str.sub(re, '\+').should == "L"
  end

  it "treats \\+ as an empty string if there was no captures" do
    "hello!".sub(/./, '\+').should == "ello!"
  end

  it "maps \\\\ in replacement to \\" do
    "hello".sub(/./, '\\\\').should == '\\ello'
  end

  it "leaves unknown \\x escapes in replacement untouched" do
    "hello".sub(/./, '\\x').should == '\\xello'
    "hello".sub(/./, '\\y').should == '\\yello'
  end

  it "leaves \\ at the end of replacement untouched" do
    "hello".sub(/./, 'hah\\').should == 'hah\\ello'
  end

  it "taints the result if the original string or replacement is tainted" do
    hello = "hello"
    hello_t = "hello"
    a = "a"
    a_t = "a"
    empty = ""
    empty_t = ""

    hello_t.taint; a_t.taint; empty_t.taint

    hello_t.sub(/./, a).tainted?.should == true
    hello_t.sub(/./, empty).tainted?.should == true

    hello.sub(/./, a_t).tainted?.should == true
    hello.sub(/./, empty_t).tainted?.should == true
    hello.sub(//, empty_t).tainted?.should == true

    hello.sub(//.taint, "foo").tainted?.should == false
  end

  it "tries to convert pattern to a string using to_str" do
    pattern = mock('.')
    pattern.should_receive(:to_str).and_return(".")

    "hello.".sub(pattern, "!").should == "hello!"
  end

  it "raises a TypeError when pattern can't be converted to a string" do
    lambda { "hello".sub(:woot, "x")     }.should raise_error(TypeError)
    lambda { "hello".sub([], "x")        }.should raise_error(TypeError)
    lambda { "hello".sub(Object.new, nil)}.should raise_error(TypeError)
  end

  it "tries to convert replacement to a string using to_str" do
    replacement = mock('hello_replacement')
    replacement.should_receive(:to_str).and_return("hello_replacement")

    "hello".sub(/hello/, replacement).should == "hello_replacement"
  end

  it "raises a TypeError when replacement can't be converted to a string" do
    lambda { "hello".sub(/[aeiou]/, []) }.should raise_error(TypeError)
    lambda { "hello".sub(/[aeiou]/, 99) }.should raise_error(TypeError)
  end

  it "returns subclass instances when called on a subclass" do
    StringSpecs::MyString.new("").sub(//, "").should be_kind_of(StringSpecs::MyString)
    StringSpecs::MyString.new("").sub(/foo/, "").should be_kind_of(StringSpecs::MyString)
    StringSpecs::MyString.new("foo").sub(/foo/, "").should be_kind_of(StringSpecs::MyString)
    StringSpecs::MyString.new("foo").sub("foo", "").should be_kind_of(StringSpecs::MyString)
  end

  it "sets $~ to MatchData of match and nil when there's none" do
    'hello.'.sub('hello', 'x')
    $~[0].should == 'hello'

    'hello.'.sub('not', 'x')
    $~.should == nil

    'hello.'.sub(/.(.)/, 'x')
    $~[0].should == 'he'

    'hello.'.sub(/not/, 'x')
    $~.should == nil
  end

  it "replaces \\\1 with \1" do
    "ababa".sub(/(b)/, '\\\1').should == "a\\1aba"
  end

  it "replaces \\\\1 with \\1" do
    "ababa".sub(/(b)/, '\\\\1').should == "a\\1aba"
  end

  it "replaces \\\\\1 with \\" do
    "ababa".sub(/(b)/, '\\\\\1').should == "a\\baba"
  end

end

describe "String#sub with pattern and block" do
  it "returns a copy of self with the first occurrences of pattern replaced with the block's return value" do
    "hi".sub(/./) { |s| s + ' ' }.should == "h i"
    "hi!".sub(/(.)(.)/) { |*a| a.inspect }.should == '["hi"]!'
  end

  it "sets $~ for access from the block" do
    str = "hello"
    str.sub(/([aeiou])/) { "<#{$~[1]}>" }.should == "h<e>llo"
    str.sub(/([aeiou])/) { "<#{$1}>" }.should == "h<e>llo"
    str.sub("l") { "<#{$~[0]}>" }.should == "he<l>lo"

    offsets = []

    str.sub(/([aeiou])/) do
       md = $~
       md.string.should == str
       offsets << md.offset(0)
       str
    end.should == "hhellollo"

    offsets.should == [[1, 2]]
  end

  # The conclusion of bug #1749 was that this example was version-specific...
  ruby_version_is "".."1.9" do
    it "restores $~ after leaving the block" do
      [/./, "l"].each do |pattern|
        old_md = nil
        "hello".sub(pattern) do
          old_md = $~
          "ok".match(/./)
          "x"
        end

        $~.should == old_md
        $~.string.should == "hello"
      end
    end
  end

  it "sets $~ to MatchData of last match and nil when there's none for access from outside" do
    'hello.'.sub('l') { 'x' }
    $~.begin(0).should == 2
    $~[0].should == 'l'

    'hello.'.sub('not') { 'x' }
    $~.should == nil

    'hello.'.sub(/.(.)/) { 'x' }
    $~[0].should == 'he'

    'hello.'.sub(/not/) { 'x' }
    $~.should == nil
  end

  it "doesn't raise a RuntimeError if the string is modified while substituting" do
    str = "hello"
    str.sub(//) { str[0] = 'x' }.should == "xhello"
    str.should == "xello"
  end

  it "doesn't interpolate special sequences like \\1 for the block's return value" do
    repl = '\& \0 \1 \` \\\' \+ \\\\ foo'
    "hello".sub(/(.+)/) { repl }.should == repl
  end

  it "converts the block's return value to a string using to_s" do
    obj = mock('hello_replacement')
    obj.should_receive(:to_s).and_return("hello_replacement")
    "hello".sub(/hello/) { obj }.should == "hello_replacement"

    obj = mock('ok')
    obj.should_receive(:to_s).and_return("ok")
    "hello".sub(/.+/) { obj }.should == "ok"
  end

  it "taints the result if the original string or replacement is tainted" do
    hello = "hello"
    hello_t = "hello"
    a = "a"
    a_t = "a"
    empty = ""
    empty_t = ""

    hello_t.taint; a_t.taint; empty_t.taint

    hello_t.sub(/./) { a }.tainted?.should == true
    hello_t.sub(/./) { empty }.tainted?.should == true

    hello.sub(/./) { a_t }.tainted?.should == true
    hello.sub(/./) { empty_t }.tainted?.should == true
    hello.sub(//) { empty_t }.tainted?.should == true

    hello.sub(//.taint) { "foo" }.tainted?.should == false
  end
end

describe "String#sub! with pattern, replacement" do
  it "modifies self in place and returns self" do
    a = "hello"
    a.sub!(/[aeiou]/, '*').should equal(a)
    a.should == "h*llo"
  end

  it "taints self if replacement is tainted" do
    a = "hello"
    a.sub!(/./.taint, "foo").tainted?.should == false
    a.sub!(/./, "foo".taint).tainted?.should == true
  end

  it "returns nil if no modifications were made" do
    a = "hello"
    a.sub!(/z/, '*').should == nil
    a.sub!(/z/, 'z').should == nil
    a.should == "hello"
  end

  ruby_version_is ""..."1.9" do
    it "raises a TypeError when self is frozen" do
      s = "hello"
      s.freeze

      s.sub!(/ROAR/, "x") # ok
      lambda { s.sub!(/e/, "e")       }.should raise_error(TypeError)
      lambda { s.sub!(/[aeiou]/, '*') }.should raise_error(TypeError)
    end
  end

  ruby_version_is "1.9" do
    it "raises a RuntimeError when self is frozen" do
      s = "hello"
      s.freeze

      lambda { s.sub!(/ROAR/, "x")    }.should raise_error(RuntimeError)
      lambda { s.sub!(/e/, "e")       }.should raise_error(RuntimeError)
      lambda { s.sub!(/[aeiou]/, '*') }.should raise_error(RuntimeError)
    end
  end
end

describe "String#sub! with pattern and block" do
  it "modifies self in place and returns self" do
    a = "hello"
    a.sub!(/[aeiou]/) { '*' }.should equal(a)
    a.should == "h*llo"
  end

  it "sets $~ for access from the block" do
    str = "hello"
    str.dup.sub!(/([aeiou])/) { "<#{$~[1]}>" }.should == "h<e>llo"
    str.dup.sub!(/([aeiou])/) { "<#{$1}>" }.should == "h<e>llo"
    str.dup.sub!("l") { "<#{$~[0]}>" }.should == "he<l>lo"

    offsets = []

    str.dup.sub!(/([aeiou])/) do
       md = $~
       md.string.should == str
       offsets << md.offset(0)
       str
    end.should == "hhellollo"

    offsets.should == [[1, 2]]
  end

  it "taints self if block's result is tainted" do
    a = "hello"
    a.sub!(/./.taint) { "foo" }.tainted?.should == false
    a.sub!(/./) { "foo".taint }.tainted?.should == true
  end

  it "returns nil if no modifications were made" do
    a = "hello"
    a.sub!(/z/) { '*' }.should == nil
    a.sub!(/z/) { 'z' }.should == nil
    a.should == "hello"
  end

  not_compliant_on :rubinius do
    it "raises a RuntimeError if the string is modified while substituting" do
      str = "hello"
      lambda { str.sub!(//) { str << 'x' } }.should raise_error(RuntimeError)
    end
  end

  ruby_version_is ""..."1.9" do
    deviates_on :rubinius do
      # MRI 1.8.x is inconsistent here, raising a TypeError when not passed
      # a block and a RuntimeError when passed a block. This is arguably a
      # bug in MRI. In 1.9, both situations raise a RuntimeError.
      it "raises a TypeError when self is frozen" do
        s = "hello"
        s.freeze

        s.sub!(/ROAR/) { "x" } # ok
        lambda { s.sub!(/e/) { "e" }       }.should raise_error(TypeError)
        lambda { s.sub!(/[aeiou]/) { '*' } }.should raise_error(TypeError)
      end
    end

    not_compliant_on :rubinius do
      it "raises a RuntimeError when self is frozen" do
        s = "hello"
        s.freeze

        s.sub!(/ROAR/) { "x" } # ok
        lambda { s.sub!(/e/) { "e" }       }.should raise_error(RuntimeError)
        lambda { s.sub!(/[aeiou]/) { '*' } }.should raise_error(RuntimeError)
      end
    end
  end

  ruby_version_is "1.9" do
    it "raises a RuntimeError when self is frozen" do
      s = "hello"
      s.freeze

      lambda { s.sub!(/ROAR/) { "x" }    }.should raise_error(RuntimeError)
      lambda { s.sub!(/e/) { "e" }       }.should raise_error(RuntimeError)
      lambda { s.sub!(/[aeiou]/) { '*' } }.should raise_error(RuntimeError)
    end
  end
end
