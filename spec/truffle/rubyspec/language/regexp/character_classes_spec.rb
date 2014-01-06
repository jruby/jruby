require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

describe "Regexp with character classes" do
  it "supports \w (word character)" do
    /\w/.match("a").to_a.should == ["a"]
    /\w/.match("1").to_a.should == ["1"]
    /\w/.match("_").to_a.should == ["_"]

    # Non-matches
    /\w/.match(LanguageSpecs.white_spaces).should be_nil
    /\w/.match(LanguageSpecs.non_alphanum_non_space).should be_nil
    /\w/.match("\0").should be_nil
  end

  it "supports \W (non-word character)" do
    /\W+/.match(LanguageSpecs.white_spaces).to_a.should == [LanguageSpecs.white_spaces]
    /\W+/.match(LanguageSpecs.non_alphanum_non_space).to_a.should == [LanguageSpecs.non_alphanum_non_space]
    /\W/.match("\0").to_a.should == ["\0"]

    # Non-matches
    /\W/.match("a").should be_nil
    /\W/.match("1").should be_nil
    /\W/.match("_").should be_nil
  end

  it "supports \s (space character)" do
    /\s+/.match(LanguageSpecs.white_spaces).to_a.should == [LanguageSpecs.white_spaces]

    # Non-matches
    /\s/.match("a").should be_nil
    /\s/.match("1").should be_nil
    /\s/.match(LanguageSpecs.non_alphanum_non_space).should be_nil
    /\s/.match("\0").should be_nil
  end

  it "supports \S (non-space character)" do
    /\S/.match("a").to_a.should == ["a"]
    /\S/.match("1").to_a.should == ["1"]
    /\S+/.match(LanguageSpecs.non_alphanum_non_space).to_a.should == [LanguageSpecs.non_alphanum_non_space]
    /\S/.match("\0").to_a.should == ["\0"]

    # Non-matches
    /\S/.match(LanguageSpecs.white_spaces).should be_nil
  end

  it "supports \d (numeric digit)" do
    /\d/.match("1").to_a.should == ["1"]

    # Non-matches
    /\d/.match("a").should be_nil
    /\d/.match(LanguageSpecs.white_spaces).should be_nil
    /\d/.match(LanguageSpecs.non_alphanum_non_space).should be_nil
    /\d/.match("\0").should be_nil
  end

  it "supports \D (non-digit)" do
    /\D/.match("a").to_a.should == ["a"]
    /\D+/.match(LanguageSpecs.white_spaces).to_a.should == [LanguageSpecs.white_spaces]
    /\D+/.match(LanguageSpecs.non_alphanum_non_space).to_a.should == [LanguageSpecs.non_alphanum_non_space]
    /\D/.match("\0").to_a.should == ["\0"]

    # Non-matches
    /\D/.match("1").should be_nil
  end

  it "supports [] (character class)" do
    /[a-z]+/.match("fooBAR").to_a.should == ["foo"]
    /[\b]/.match("\b").to_a.should == ["\b"] # \b inside character class is backspace
  end

  it "supports [[:alpha:][:digit:][:etc:]] (predefined character classes)" do
    /[[:alnum:]]+/.match("a1").to_a.should == ["a1"]
    /[[:alpha:]]+/.match("Aa1").to_a.should == ["Aa"]
    /[[:blank:]]+/.match(LanguageSpecs.white_spaces).to_a.should == [LanguageSpecs.blanks]
    # /[[:cntrl:]]/.match("").to_a.should == [""] # TODO: what should this match?
    /[[:digit:]]/.match("1").to_a.should == ["1"]
    # /[[:graph:]]/.match("").to_a.should == [""] # TODO: what should this match?
    /[[:lower:]]+/.match("Aa1").to_a.should == ["a"]
    /[[:print:]]+/.match(LanguageSpecs.white_spaces).to_a.should == [" "]     # include all of multibyte encoded characters
    /[[:punct:]]+/.match(LanguageSpecs.punctuations).to_a.should == [LanguageSpecs.punctuations]
    /[[:space:]]+/.match(LanguageSpecs.white_spaces).to_a.should == [LanguageSpecs.white_spaces]
    /[[:upper:]]+/.match("123ABCabc").to_a.should == ["ABC"]
    /[[:xdigit:]]+/.match("xyz0123456789ABCDEFabcdefXYZ").to_a.should == ["0123456789ABCDEFabcdef"]

    # Parsing
    /[[:lower:][:digit:]A-C]+/.match("a1ABCDEF").to_a.should == ["a1ABC"] # can be composed with other constructs in the character class
    /[^[:lower:]A-C]+/.match("abcABCDEF123def").to_a.should == ["DEF123"] # negated character class
    /[:alnum:]+/.match("a:l:n:u:m").to_a.should == ["a:l:n:u:m"] # should behave like regular character class composed of the individual letters
    /[\[:alnum:]+/.match("[:a:l:n:u:m").to_a.should == ["[:a:l:n:u:m"] # should behave like regular character class composed of the individual letters
    lambda { eval('/[[:alpha:]-[:digit:]]/') }.should raise_error(SyntaxError) # can't use character class as a start value of range
  end

  language_version __FILE__, "character_classes"
end

