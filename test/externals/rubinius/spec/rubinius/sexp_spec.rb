require File.dirname(__FILE__) + '/../spec_helper'

context "Shotgun" do
  class String
    alias :old_to_sexp :to_sexp
    def to_sexp
      old_to_sexp('test', 1, false)
    end
  end
  
  specify "should convert a number to an sexp" do
    "834234".to_sexp.should == [:lit, 834234]
  end
  
  specify "should convert a regexp to an sexp" do
    "/blah/".to_sexp.should == [:regex, "blah", 0]
    "/blah/i".to_sexp.should == [:regex, "blah", 1]
    "/blah/u".to_sexp.should == [:regex, "blah", 64]
  end
  
  specify "should convert a string to an sexp" do
    "\"hello\"".to_sexp.should == [:str, "hello"]
  end

  specify "should convert a local var to an sexp" do
    "a = 1; a".to_sexp.should == [:block, [:lasgn, :a, 0, [:lit, 1]], [:lvar, :a, 0]]
  end
  
  specify "should convert an instance variable to an sexp" do
    "@blah".to_sexp.should == [:ivar, :@blah]
  end

  specify "should convert an instance variable assignment to an sexp" do
    "@blah = 1".to_sexp.should == [:iasgn, :@blah, [:lit, 1]]
  end

  specify "should convert a global variable to an sexp" do
    "$blah".to_sexp.should == [:gvar, :$blah]
  end

  specify "should convert a global variable assignment to an sexp" do
    "$blah = 1".to_sexp.should == [:gasgn, :$blah, [:lit, 1]]
  end

  specify "should convert a symbol to an sexp" do
    ":blah".to_sexp.should == [:lit, :blah]
  end

  specify "should convert a string expansion to an sexp" do
    'a = 1; "hello #{a}, you rock."'.to_sexp.should == 
      [:block, 
        [:lasgn, :a, 0, [:lit, 1]], 
        [:dstr, "hello ", [:evstr, [:lvar, :a, 0]], 
        [:str, ", you rock."]]]
  end

  specify "should convert a pathological string expansion to an sexp" do
    '@thing = 5; "hello #@thing, you are crazy."'.to_sexp.should == 
      [:block, 
        [:iasgn, :@thing, [:lit, 5]], 
        [:dstr, "hello ", [:evstr, [:ivar, :@thing]], 
        [:str, ", you are crazy."]]]
  end

  specify "should convert a method definition without arguments to an sexp" do
    "def name; 1; end".to_sexp.should == [:defn, :name, [:scope, [:block, [:args], [:lit, 1]], []]]
  end

  specify "should convert a method definition with arguments to an sexp" do
    "def name(a, b); 1; end".to_sexp.should == 
      [:defn, :name, 
        [:scope, [:block, [:args, [:a, :b], [], nil, nil], [:lit, 1]], [:a, :b]]]
  end

  specify "should convert a class definition to an sexp" do
    "class Blah < A::B; end".to_sexp.should == 
      [:class, 
        [:colon2, :Blah], [:colon2, [:const, :A], :B], [:scope, []]]
  end

  specify "should convert a heredoc to an sexp" do
      "a = <<-BLAH
      hello
      BLAH
      ".to_sexp.should == [:lasgn, :a, 0, [:str, "      hello\n"]]
  end
end
