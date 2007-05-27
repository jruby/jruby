require File.dirname(__FILE__) + '/../spec_helper'

# class methods
# compile, escape, last_match, new, quote, union

# ==, ===, =~, casefold?, eql?, hash, inspect, kcode, match, options,
# source, to_s, ~

context "Regexp class method" do
  specify "compile should be a synonym for new" do
    Regexp.compile('').is_a?(Regexp).should == true
  end
  
  specify "escape should escape any characters with special meaning in a regular expression" do
    Regexp.escape('\*?{}.+^[]()-').should == '\\\\\*\?\{\}\.\+\^\[\]\(\)\-'
  end
  
  specify "last_match with no argument should return MatchData" do
    /c(.)t/ =~ 'cat'
    Regexp.last_match.is_a?(MatchData).should == true
  end
  
  specify "last_match with a fixnum argument should return the nth field in this MatchData" do
    /c(.)t/ =~ 'cat'
    Regexp.last_match(1).should == 'a'
  end
  
  specify "new should create a new regular expression object" do
    Regexp.new('').is_a?(Regexp).should == true
  end
  
  specify "quote should be a synonym for escape" do
    Regexp.escape('\*?{}.+^[]()-').should == '\\\\\*\?\{\}\.\+\^\[\]\(\)\-'
  end
  
  specify "union with no arguments should return /(?!)/" do
    Regexp.union.should == /(?!)/
  end
  
  specify "union with arguments should return a regular expression that will match any part" do
    Regexp.union("penzance").should == /penzance/
    Regexp.union("skiing", "sledding").should == /skiing|sledding/
    Regexp.union(/dogs/, /cats/i).should == /(?-mix:dogs)|(?i-mx:cats)/
  end
end

context "Regexp instance method" do
  specify "== should be true if self and other have the same pattern, character set code, and casefold? values" do
    (/abc/ == /abc/).should == true
    (/abc/ == /abc/x).should == false
    (/abc/ == /abc/i).should == false
    (/abc/u == /abc/n).should == false
  end
  
  specify "=== should be true if there is a match" do
    (/abc/ === "aabcc").should == true
  end
  
  specify "=== should be false if there is no match" do
    (/abc/ === "xyz").should == false
  end
  
  specify "=~ should return the first position of the match" do
    (/(.)(.)(.)/ =~ "abc").should == 0
    $~.begin(0).should == 0
  end
  
  specify "=~ should return nil if there is no match" do
    /xyz/ =~ "abxyc"
    $~.should == nil
  end
  
  specify "casefold? should return the value of the case-insensitive flag" do
    /abc/i.casefold?.should == true
    /xyz/.casefold?.should == false
  end
  
  specify "eql? should be a synonym for ==" do
    /abc/.eql?(/abc/).should == true
    /abc/.eql?(/abc/x).should == false
    /abc/.eql?(/abc/i).should == false
    /abc/u.eql?(/abc/n).should == false
  end
  
  specify "should provide hash" do
    Regexp.new('').respond_to?(:hash).should == true
  end

  specify "hash is based on the text and options of Regexp" do
    (/cat/ix.hash == /cat/ixn.hash).should == true
    (/dog/m.hash  == /dog/m.hash).should == true
    (/cat/.hash   == /cat/ix.hash).should == false
    (/cat/.hash   == /dog/.hash).should == false
  end

  specify "inspect should produce a formatted string" do
    /ab+c/ix.inspect.should == "/ab+c/ix"
    /a(.)+s/n.inspect.should == "/a(.)+s/n"
  end

  specify "kcode should return the character set code" do
    /ab+c/s.kcode.should == "sjis"
    /a(.)+s/n.kcode.should == "none"
    /xyz/e.kcode.should == "euc"
    /cars/u.kcode.should == "utf8"
  end

  specify "match should be a synonym for =~" do
    /(.)(.)(.)/.match("abc")[2].should == 'b'
    /xyz/.match("abxyc").should == nil
  end

  specify "options should return set of bits for options used" do
    /cat/.options.should == 0
    /cat/ix.options.should == 3
    Regexp.new('cat', true).options.should == 1
    Regexp.new('cat', 0, 's').options.should == 48
  end

  specify "source should return the original string of the pattern" do
    /ab+c/ix.source.should == "ab+c"
    /x(.)xz/.source.should == "x(.)xz"
  end

  specify "to_s should return a string in (?xxx:yyy) notation" do
    /ab+c/ix.to_s.should == "(?ix-m:ab+c)"
    /xyz/.to_s.should == "(?-mix:xyz)"
    /jis/s.to_s.should == "(?-mix:jis)"
  end

  specify "~ should match against the contents of $_" do
    $_ = "input data"
    (~ /at/).should == 7
  end
end
