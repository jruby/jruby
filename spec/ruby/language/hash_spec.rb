require File.expand_path('../../spec_helper', __FILE__)

describe "Hash literal" do
  it "{} should return an empty hash" do
    {}.size.should == 0
    {}.should == {}
  end

  it "{} should return a new hash populated with the given elements" do
    h = {:a => 'a', 'b' => 3, 44 => 2.3}
    h.size.should == 3
    h.should == {:a => "a", "b" => 3, 44 => 2.3}
  end

  it "treats empty expressions as nils" do
    h = {() => ()}
    h.keys.should == [nil]
    h.values.should == [nil]
    h[nil].should == nil

    h = {() => :value}
    h.keys.should == [nil]
    h.values.should == [:value]
    h[nil].should == :value

    h = {:key => ()}
    h.keys.should == [:key]
    h.values.should == [nil]
    h[:key].should == nil
  end

  it "freezes string keys on initialization" do
    key = "foo"
    h = {key => "bar"}
    key.reverse!
    h["foo"].should == "bar"
    h.keys.first.should == "foo"
    h.keys.first.frozen?.should == true
    key.should == "oof"
  end

  it "checks duplicated keys on initialization" do
    h = {:foo => :bar, :foo => :foo}
    h.keys.size.should == 1
    h.should == {:foo => :foo}
  end

  it "accepts a hanging comma" do
    h = {:a => 1, :b => 2,}
    h.size.should == 2
    h.should == {:a => 1, :b => 2}
  end

  it "recognizes '=' at the end of the key" do
    eval("{:a==>1}").should   == {:"a=" => 1}
    eval("{:a= =>1}").should  == {:"a=" => 1}
    eval("{:a= => 1}").should == {:"a=" => 1}
  end

  it "with '==>' in the middle raises SyntaxError" do
    lambda { eval("{:a ==> 1}") }.should raise_error(SyntaxError)
  end

  it "constructs a new hash with the given elements" do
    {foo: 123}.should == {:foo => 123}
    h = {:rbx => :cool, :specs => 'fail_sometimes'}
    {rbx: :cool, specs: 'fail_sometimes'}.should == h
  end

  it "ignores a hanging comma" do
    {foo: 123,}.should == {:foo => 123}
    h = {:rbx => :cool, :specs => 'fail_sometimes'}
    {rbx: :cool, specs: 'fail_sometimes',}.should == h
  end

  it "accepts mixed 'key: value' and 'key => value' syntax" do
    h = {:a => 1, :b => 2, "c" => 3}
    {a: 1, :b => 2, "c" => 3}.should == h
  end

  it "expands an '**{}' element into the containing Hash literal initialization" do
    {a: 1, **{b: 2}, c: 3}.should == {a: 1, b: 2, c: 3}
  end

  it "expands an '**obj' element into the containing Hash literal initialization" do
    h = {b: 2, c: 3}
    {**h, a: 1}.should == {b: 2, c: 3, a: 1}
    {a: 1, **h}.should == {a: 1, b: 2, c: 3}
    {a: 1, **h, c: 4}.should == {a: 1, b: 2, c: 4}
  end

  ruby_version_is "2.0"..."2.2" do
    it "expands an '**{}' element with containing Hash literal keys taking precedence" do
      {a: 1, **{a: 2, b: 3, c: 1}, c: 3}.should == {a: 1, b: 3, c: 3}
    end

    it "merges multiple nested '**obj' in Hash literals" do
      h = {a: 1, **{a: 2, **{b: 3, **{c: 4}}, **{d: 5}, }, **{d: 6}}
      h.should == {a: 1, b: 3, c: 4, d: 5}
    end
  end

  ruby_version_is "2.2" do
    it "expands an '**{}' element with the last key/value pair taking precedence" do
      {a: 1, **{a: 2, b: 3, c: 1}, c: 3}.should == {a: 2, b: 3, c: 3}
    end

    it "merges multiple nested '**obj' in Hash literals" do
      h = {a: 1, **{a: 2, **{b: 3, **{c: 4}}, **{d: 5}, }, **{d: 6}}
      h.should == {a: 2, b: 3, c: 4, d: 6}
    end
  end

  it "calls #to_hash to expand an '**obj' element" do
    obj = mock("hash splat")
    obj.should_receive(:to_hash).and_return({b: 2, d: 4})

    {a: 1, **obj, c: 3}.should == {a:1, b: 2, c: 3, d: 4}
  end

  it "raises a TypeError if #to_hash does not return a Hash" do
    obj = mock("hash splat")
    obj.should_receive(:to_hash).and_return(obj)

    lambda { {**obj} }.should raise_error(TypeError)
  end

end
