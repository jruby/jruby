specs = LangSendSpecs

describe "Invoking a method" do
  it "with lambda as block argument is ok" do

    l = lambda { 300 }
    specs.oneb(10, &l).should == [10,300]
  end

  it "allows to pass argument, a hash without curly braces and a block argument" do
    specs.twob(:abc, 'rbx' => 'cool', 'specs' => 'fail sometimes') { 500 }.should ==
      [:abc, { 'rbx' => 'cool', 'specs' => 'fail sometimes'}, 500]

    specs.twob(:abc, 'rbx' => 'cool', 'specs' => 'fail sometimes') do
      500
    end.should ==
      [:abc, { 'rbx' => 'cool', 'specs' => 'fail sometimes'}, 500]

    l = lambda { 500 }

    specs.twob(:abc, 'rbx' => 'cool', 'specs' => 'fail sometimes', &l).should ==
      [:abc, { 'rbx' => 'cool', 'specs' => 'fail sometimes'}, 500]
  end

  it "raises SyntaxError if there is ambigious arguments" do
    lambda {
      eval "f 4, f 5, 6"
    }.should raise_error(SyntaxError)
  end

  describe "with ambigious missing parens and no receiver" do
    it "arguments go with innermost call" do
      (lang_send_rest_len lang_send_rest_len 5, 6).should == 1
    end

    it "prefers a grouped expression to arguments" do
      (lang_send_rest_len (5+6)*7).should == 1
      (lang_send_rest_len(5+6)*7).should == 7
    end
  end

  describe "with ambiguous missing parens and a receiver" do
    it "arguments go with innermost call" do
      (specs.rest_len specs.rest_len 5, 6).should == 1
    end

    it "prefers arguments to a grouped expression" do
      (specs.rest_len (5+6)*7).should == 7
      (specs.rest_len(5+6)*7).should == 7
    end
  end

  it "with splat operator attempts to coerce it to an Array if the object respond_to?(:to_ary)" do
    ary = [2,3,4]
    obj = LangSendSpecs::ToAry.new ary
    specs.fooM0R(*obj).should == ary
    specs.fooM1R(1,*obj).should == [1, ary]
  end

  it "with splat operator * and non-Array value uses value unchanged if it does not respond_to?(:to_ary)" do
    obj = Object.new
    obj.should_not respond_to(:to_ary)

    specs.fooM0R(*obj).should == [obj]
    specs.fooM1R(1,*obj).should == [1, [obj]]
  end
end
