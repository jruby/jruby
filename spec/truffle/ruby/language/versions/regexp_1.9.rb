require File.expand_path('../../../spec_helper', __FILE__)

describe "Literal Regexps" do
  it "supports (?<= ) (positive lookbehind)" do
    /foo.(?<=\d)/.match("fooA foo1").to_a.should == ["foo1"]
  end

  it "supports (?<! ) (negative lookbehind)" do
    /foo.(?<!\d)/.match("foo1 fooA").to_a.should == ["fooA"]
  end

  it "supports \g (named backreference)" do
    /(?<foo>foo.)bar\g<foo>/.match("foo1barfoo2").to_a.should == ["foo1barfoo2", "foo2"]
  end

  it "supports character class composition" do
    /[a-z&&[^a-c]]+/.match("abcdef").to_a.should == ["def"]
    /[a-z&&[^d-i&&[^d-f]]]+/.match("abcdefghi").to_a.should == ["abcdef"]
  end

  it "supports possessive quantifiers" do
    /fooA++bar/.match("fooAAAbar").to_a.should == ["fooAAAbar"]

    /fooA++Abar/.match("fooAAAbar").should be_nil
    /fooA?+Abar/.match("fooAAAbar").should be_nil
    /fooA*+Abar/.match("fooAAAbar").should be_nil
  end
end
