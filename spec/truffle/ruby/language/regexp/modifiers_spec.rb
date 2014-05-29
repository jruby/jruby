require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

describe "Regexps with modifers" do
  it "supports /i (case-insensitive)" do
    /foo/i.match("FOO").to_a.should == ["FOO"]
  end

  it "supports /m (multiline)" do
    /foo.bar/m.match("foo\nbar").to_a.should == ["foo\nbar"]
    /foo.bar/.match("foo\nbar").should be_nil
  end

  it "supports /x (extended syntax)" do
    /\d +/x.match("abc123").to_a.should == ["123"] # Quantifiers can be separated from the expression they apply to
  end

  it "supports /o (once)" do
    2.times do |i|
      /#{i}/o.should == /0/
    end
  end

  it "invokes substitutions for /o only once" do
    ScratchPad.record []
    to_s_callback = Proc.new do
      ScratchPad << :to_s_callback
      "class_with_to_s"
    end
    o = LanguageSpecs::ClassWith_to_s.new(to_s_callback)
    2.times { /#{o}/o }
    ScratchPad.recorded.should == [:to_s_callback]
  end

  ruby_version_is "" ... "1.9" do
    it "does not do thread synchronization for /o" do
      ScratchPad.record []

      to_s_callback2 = Proc.new do
        ScratchPad << :to_s_callback2
        "class_with_to_s2"
      end

      to_s_callback1 = Proc.new do
        ScratchPad << :to_s_callback1
        t2 = Thread.new do
          o2 = LanguageSpecs::ClassWith_to_s.new(to_s_callback2)
          ScratchPad << LanguageSpecs.get_regexp_with_substitution(o2)
        end
        t2.join
        "class_with_to_s1"
      end

      o1 = LanguageSpecs::ClassWith_to_s.new(to_s_callback1)
      ScratchPad << LanguageSpecs.get_regexp_with_substitution(o1)

      ScratchPad.recorded.should == [:to_s_callback1, :to_s_callback2, /class_with_to_s2/, /class_with_to_s2/]
    end
  end

  it "supports modifier combinations" do
    /foo/imox.match("foo").to_a.should == ["foo"]
    /foo/imoximox.match("foo").to_a.should == ["foo"]

    lambda { eval('/foo/a') }.should raise_error(SyntaxError)
  end

  it "supports (?imx-imx) (inline modifiers)" do
    /(?i)foo/.match("FOO").to_a.should == ["FOO"]
    /foo(?i)/.match("FOO").should be_nil
    # Interaction with /i
    /(?-i)foo/i.match("FOO").should be_nil
    /foo(?-i)/i.match("FOO").to_a.should == ["FOO"]
    # Multiple uses
    /foo (?i)bar (?-i)baz/.match("foo BAR baz").to_a.should == ["foo BAR baz"]
    /foo (?i)bar (?-i)baz/.match("foo BAR BAZ").should be_nil

    /(?m)./.match("\n").to_a.should == ["\n"]
    /.(?m)/.match("\n").should be_nil
    # Interaction with /m
    /(?-m)./m.match("\n").should be_nil
    /.(?-m)/m.match("\n").to_a.should == ["\n"]
    # Multiple uses
    /. (?m). (?-m)./.match(". \n .").to_a.should == [". \n ."]
    /. (?m). (?-m)./.match(". \n \n").should be_nil

    /(?x) foo /.match("foo").to_a.should == ["foo"]
    / foo (?x)/.match("foo").should be_nil
    # Interaction with /x
    /(?-x) foo /x.match("foo").should be_nil
    / foo (?-x)/x.match("foo").to_a.should == ["foo"]
    # Multiple uses
    /( foo )(?x)( bar )(?-x)( baz )/.match(" foo bar baz ").to_a.should == [" foo bar baz ", " foo ", "bar", " baz "]
    /( foo )(?x)( bar )(?-x)( baz )/.match(" foo barbaz").should be_nil

    # Parsing
    /(?i-i)foo/.match("FOO").should be_nil
    /(?ii)foo/.match("FOO").to_a.should == ["FOO"]
    /(?-)foo/.match("foo").to_a.should == ["foo"]
    lambda { eval('/(?o)/') }.should raise_error(SyntaxError)
  end

  it "supports (?imx-imx:expr) (scoped inline modifiers)" do
    /foo (?i:bar) baz/.match("foo BAR baz").to_a.should == ["foo BAR baz"]
    /foo (?i:bar) baz/.match("foo BAR BAZ").should be_nil
    /foo (?-i:bar) baz/i.match("foo BAR BAZ").should be_nil

    /. (?m:.) ./.match(". \n .").to_a.should == [". \n ."]
    /. (?m:.) ./.match(". \n \n").should be_nil
    /. (?-m:.) ./m.match("\n \n \n").should be_nil

    /( foo )(?x: bar )( baz )/.match(" foo bar baz ").to_a.should == [" foo bar baz ", " foo ", " baz "]
    /( foo )(?x: bar )( baz )/.match(" foo barbaz").should be_nil
    /( foo )(?-x: bar )( baz )/x.match("foo bar baz").to_a.should == ["foo bar baz", "foo", "baz"]

    # Parsing
    /(?i-i:foo)/.match("FOO").should be_nil
    /(?ii:foo)/.match("FOO").to_a.should == ["FOO"]
    /(?-:)foo/.match("foo").to_a.should == ["foo"]
    lambda { eval('/(?o:)/') }.should raise_error(SyntaxError)
  end

  it "supports . with /m" do
    # Basic matching
    /./m.match("\n").to_a.should == ["\n"]
  end

  ruby_version_is ""..."2.0" do
    it "raises SyntaxError for ASII/Unicode modifiers" do
      lambda { eval('/(?a)/') }.should raise_error(SyntaxError)
      lambda { eval('/(?d)/') }.should raise_error(SyntaxError)
      lambda { eval('/(?u)/') }.should raise_error(SyntaxError)
    end
  end

  ruby_version_is "2.0" do
    it "supports ASII/Unicode modifiers" do
      eval('/(?a)[[:alpha:]]+/').match("a\u3042").to_a.should == ["a"]
      eval('/(?d)[[:alpha:]]+/').match("a\u3042").to_a.should == ["a\u3042"]
      eval('/(?u)[[:alpha:]]+/').match("a\u3042").to_a.should == ["a\u3042"]
      eval('/(?a)\w+/').match("a\u3042").to_a.should == ["a"]
      eval('/(?d)\w+/').match("a\u3042").to_a.should == ["a"]
      eval('/(?u)\w+/').match("a\u3042").to_a.should == ["a\u3042"]
    end
  end
end
