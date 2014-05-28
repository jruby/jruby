require File.expand_path('../../spec_helper', __FILE__)
require File.expand_path('../fixtures/block', __FILE__)

describe "A block" do
  before :each do
    @y = BlockSpecs::Yielder.new
  end

  it "captures locals from the surrounding scope" do
    var = 1
    @y.z { var }.should == 1
  end

  it "allows for a leading space before the arguments" do
    res = @y.s (:a){ 1 }
    res.should == 1
  end

  it "allows to define a block variable with the same name as the enclosing block" do
    o = BlockSpecs::OverwriteBlockVariable.new
    o.z { 1 }.should == 1
  end

  it "does not capture a local when an argument has the same name" do
    var = 1
    @y.s(2) { |var| var }.should == 2
    var.should == 1
  end

  describe "taking zero arguments" do
    it "does not raise an exception when no values are yielded" do
      @y.z { 1 }.should == 1
    end

    it "does not raise an exception when values are yielded" do
      @y.s(0) { 1 }.should == 1
    end
  end

  describe "taking || arguments" do
    it "does not raise an exception when no values are yielded" do
      @y.z { || 1 }.should == 1
    end

    it "does not raise an exception when values are yielded" do
      @y.s(0) { || 1 }.should == 1
    end
  end

  describe "taking |a| arguments" do
    it "assigns nil to the argument when no values are yielded" do
      @y.z { |a| a }.should be_nil
    end

    it "assigns the value yielded to the argument" do
      @y.s(1) { |a| a }.should == 1
    end

    it "does not call #to_ary to convert a single yielded object to an Array" do
      obj = mock("block yield to_ary")
      obj.should_not_receive(:to_ary)

      @y.s(obj) { |a| a }.should equal(obj)
    end

    it "assigns the first value yielded to the argument" do
      @y.m(1, 2) { |a| a }.should == 1
    end

    it "does not destructure a single Array value" do
      @y.s([1, 2]) { |a| a }.should == [1, 2]
    end
  end

  describe "taking |a, b| arguments" do
    it "assgins nil to the arguments when no values are yielded" do
      @y.z { |a, b| [a, b] }.should == [nil, nil]
    end

    it "assigns one value yielded to the first argument" do
      @y.s(1) { |a, b| [a, b] }.should == [1, nil]
    end

    it "assigns the first two values yielded to the arguments" do
      @y.m(1, 2, 3) { |a, b| [a, b] }.should == [1, 2]
    end

    it "does not destructure an Array value as one of several values yielded" do
      @y.m([1, 2], 3, 4) { |a, b| [a, b] }.should == [[1, 2], 3]
    end

    it "assigns 'nil' and 'nil' to the arguments when a single, empty Array is yielded" do
      @y.s([]) { |a, b| [a, b] }.should == [nil, nil]
    end

    it "assigns the element of a single element Array to the first argument" do
      @y.s([1]) { |a, b| [a, b] }.should == [1, nil]
      @y.s([nil]) { |a, b| [a, b] }.should == [nil, nil]
      @y.s([[]]) { |a, b| [a, b] }.should == [[], nil]
    end

    it "destructures a single Array value yielded" do
      @y.s([1, 2, 3]) { |a, b| [a, b] }.should == [1, 2]
    end

    it "destructures a splatted Array" do
      @y.r([[]]) { |a, b| [a, b] }.should == [nil, nil]
      @y.r([[1]]) { |a, b| [a, b] }.should == [1, nil]
    end

    it "calls #to_ary to convert a single yielded object to an Array" do
      obj = mock("block yield to_ary")
      obj.should_receive(:to_ary).and_return([1, 2])

      @y.s(obj) { |a, b| [a, b] }.should == [1, 2]
    end

    it "does not call #to_ary if the single yielded object is an Array" do
      obj = [1, 2]
      obj.should_not_receive(:to_ary)

      @y.s(obj) { |a, b| [a, b] }.should == [1, 2]
    end

    it "does not call #to_ary if the object does not respond to #to_ary" do
      obj = mock("block yield no to_ary")

      @y.s(obj) { |a, b| [a, b] }.should == [obj, nil]
    end

    it "raises a TypeError if #to_ary does not return an Array" do
      obj = mock("block yield to_ary invalid")
      obj.should_receive(:to_ary).and_return(1)

      lambda { @y.s(obj) { |a, b| } }.should raise_error(TypeError)
    end

    it "raises the original exception if #to_ary raises an exception" do
      obj = mock("block yield to_ary raising an exception")
      obj.should_receive(:to_ary).and_raise(ZeroDivisionError)

      lambda { @y.s(obj) { |a, b| } }.should raise_error(ZeroDivisionError)
    end

  end

  describe "taking |a, *b| arguments" do
    it "assigns 'nil' and '[]' to the arguments when no values are yielded" do
      @y.z { |a, *b| [a, b] }.should == [nil, []]
    end

    it "assigns all yielded values after the first to the rest argument" do
      @y.m(1, 2, 3) { |a, *b| [a, b] }.should == [1, [2, 3]]
    end

    it "assigns 'nil' and '[]' to the arguments when a single, empty Array is yielded" do
      @y.s([]) { |a, *b| [a, b] }.should == [nil, []]
    end

    it "assigns the element of a single element Array to the first argument" do
      @y.s([1]) { |a, *b| [a, b] }.should == [1, []]
      @y.s([nil]) { |a, *b| [a, b] }.should == [nil, []]
      @y.s([[]]) { |a, *b| [a, b] }.should == [[], []]
    end

    it "destructures a splatted Array" do
      @y.r([[]]) { |a, *b| [a, b] }.should == [nil, []]
      @y.r([[1]]) { |a, *b| [a, b] }.should == [1, []]
    end

    it "destructures a single Array value assigning the remaining values to the rest argument" do
      @y.s([1, 2, 3]) { |a, *b| [a, b] }.should == [1, [2, 3]]
    end

    it "calls #to_ary to convert a single yielded object to an Array" do
      obj = mock("block yield to_ary")
      obj.should_receive(:to_ary).and_return([1, 2])

      @y.s(obj) { |a, *b| [a, b] }.should == [1, [2]]
    end

    it "does not call #to_ary if the single yielded object is an Array" do
      obj = [1, 2]
      obj.should_not_receive(:to_ary)

      @y.s(obj) { |a, *b| [a, b] }.should == [1, [2]]
    end

    it "does not call #to_ary if the object does not respond to #to_ary" do
      obj = mock("block yield no to_ary")

      @y.s(obj) { |a, *b| [a, b] }.should == [obj, []]
    end

    it "raises a TypeError if #to_ary does not return an Array" do
      obj = mock("block yield to_ary invalid")
      obj.should_receive(:to_ary).and_return(1)

      lambda { @y.s(obj) { |a, *b| } }.should raise_error(TypeError)
    end
  end

  describe "taking |*| arguments" do
    it "does not raise an exception when no values are yielded" do
      @y.z { |*| 1 }.should == 1
    end

    it "does not raise an exception when values are yielded" do
      @y.s(0) { |*| 1 }.should == 1
    end

    it "does not call #to_ary if the single yielded object is an Array" do
      obj = [1, 2]
      obj.should_not_receive(:to_ary)

      @y.s(obj) { |*| 1 }.should == 1
    end

    it "does not call #to_ary if the object does not respond to #to_ary" do
      obj = mock("block yield no to_ary")

      @y.s(obj) { |*| 1 }.should == 1
    end

    it "does not call #to_ary to convert a single yielded object to an Array" do
      obj = mock("block yield to_ary")
      obj.should_not_receive(:to_ary)

      @y.s(obj) { |*| 1 }.should == 1
    end
  end

  describe "taking |*a| arguments" do
    it "assigns '[]' to the argument when no values are yielded" do
      @y.z { |*a| a }.should == []
    end

    it "assigns a single value yielded to the argument as an Array" do
      @y.s(1) { |*a| a }.should == [1]
    end

    it "assigns all the values passed to the argument as an Array" do
      @y.m(1, 2, 3) { |*a| a }.should == [1, 2, 3]
    end

    it "assigns '[[]]' to the argument when passed an empty Array" do
      @y.s([]) { |*a| a }.should == [[]]
    end

    it "assigns a single Array value passed to the argument by wrapping it in an Array" do
      @y.s([1, 2, 3]) { |*a| a }.should == [[1, 2, 3]]
    end

    it "does not call #to_ary if the single yielded object is an Array" do
      obj = [1, 2]
      obj.should_not_receive(:to_ary)

      @y.s(obj) { |*a| a }.should == [[1, 2]]
    end

    it "does not call #to_ary if the object does not respond to #to_ary" do
      obj = mock("block yield no to_ary")

      @y.s(obj) { |*a| a }.should == [obj]
    end

    it "does not call #to_ary to convert a single yielded object to an Array" do
      obj = mock("block yield to_ary")
      obj.should_not_receive(:to_ary)

      @y.s(obj) { |*a| a }.should == [obj]
    end
  end

  describe "taking |a, | arguments" do
    it "assigns nil to the argument when no values are yielded" do
      @y.z { |a, | a }.should be_nil
    end

    it "assgins the argument a single value yielded" do
      @y.s(1) { |a, | a }.should == 1
    end

    it "assigns the argument the first value yielded" do
      @y.m(1, 2) { |a, | a }.should == 1
    end

    it "assigns the argument the first of several values yielded when it is an Array" do
      @y.m([1, 2], 3) { |a, | a }.should == [1, 2]
    end

    it "assigns nil to the argument when passed an empty Array" do
      @y.s([]) { |a, | a }.should be_nil
    end

    it "assigns the argument the first element of the Array when passed a single Array" do
      @y.s([1, 2]) { |a, | a }.should == 1
    end

    it "calls #to_ary to convert a single yielded object to an Array" do
      obj = mock("block yield to_ary")
      obj.should_receive(:to_ary).and_return([1, 2])

      @y.s(obj) { |a, | a }.should == 1
    end

    it "does not call #to_ary if the single yielded object is an Array" do
      obj = [1, 2]
      obj.should_not_receive(:to_ary)

      @y.s(obj) { |a, | a }.should == 1
    end

    it "does not call #to_ary if the object does not respond to #to_ary" do
      obj = mock("block yield no to_ary")

      @y.s(obj) { |a, | a }.should == obj
    end

    it "raises a TypeError if #to_ary does not return an Array" do
      obj = mock("block yield to_ary invalid")
      obj.should_receive(:to_ary).and_return(1)

      lambda { @y.s(obj) { |a, | } }.should raise_error(TypeError)
    end
  end

  describe "taking |(a, b)| arguments" do
    it "assigns nil to the arguments when yielded no values" do
      @y.z { |(a, b)| [a, b] }.should == [nil, nil]
    end

    it "destructures a single Array value yielded" do
      @y.s([1, 2]) { |(a, b)| [a, b] }.should == [1, 2]
    end

    it "destructures a single Array value yielded when shadowing an outer variable" do
      a = 9
      @y.s([1, 2]) { |(a, b)| [a, b] }.should == [1, 2]
    end

    it "calls #to_ary to convert a single yielded object to an Array" do
      obj = mock("block yield to_ary")
      obj.should_receive(:to_ary).and_return([1, 2])

      @y.s(obj) { |(a, b)| [a, b] }.should == [1, 2]
    end

    it "does not call #to_ary if the single yielded object is an Array" do
      obj = [1, 2]
      obj.should_not_receive(:to_ary)

      @y.s(obj) { |(a, b)| [a, b] }.should == [1, 2]
    end

    it "does not call #to_ary if the object does not respond to #to_ary" do
      obj = mock("block yield no to_ary")

      @y.s(obj) { |(a, b)| [a, b] }.should == [obj, nil]
    end

    it "raises a TypeError if #to_ary does not return an Array" do
      obj = mock("block yield to_ary invalid")
      obj.should_receive(:to_ary).and_return(1)

      lambda { @y.s(obj) { |(a, b)| } }.should raise_error(TypeError)
    end
  end

  describe "taking |(a, b), c| arguments" do
    it "assigns nil to the arguments when yielded no values" do
      @y.z { |(a, b), c| [a, b, c] }.should == [nil, nil, nil]
    end

    it "destructures a single one-level Array value yielded" do
      @y.s([1, 2]) { |(a, b), c| [a, b, c] }.should == [1, nil, 2]
    end

    it "destructures a single multi-level Array value yielded" do
      @y.s([[1, 2, 3], 4]) { |(a, b), c| [a, b, c] }.should == [1, 2, 4]
    end

    it "calls #to_ary to convert a single yielded object to an Array" do
      obj = mock("block yield to_ary")
      obj.should_receive(:to_ary).and_return([1, 2])

      @y.s(obj) { |(a, b), c| [a, b, c] }.should == [1, nil, 2]
    end

    it "does not call #to_ary if the single yielded object is an Array" do
      obj = [1, 2]
      obj.should_not_receive(:to_ary)

      @y.s(obj) { |(a, b), c| [a, b, c] }.should == [1, nil, 2]
    end

    it "does not call #to_ary if the object does not respond to #to_ary" do
      obj = mock("block yield no to_ary")

      @y.s(obj) { |(a, b), c| [a, b, c] }.should == [obj, nil, nil]
    end

    it "raises a TypeError if #to_ary does not return an Array" do
      obj = mock("block yield to_ary invalid")
      obj.should_receive(:to_ary).and_return(1)

      lambda { @y.s(obj) { |(a, b), c| } }.should raise_error(TypeError)
    end
  end

  describe "taking nested |a, (b, (c, d))|" do
    it "assigns nil to the arguments when yielded no values" do
      @y.m { |a, (b, (c, d))| [a, b, c, d] }.should == [nil, nil, nil, nil]
    end

    it "destructures separate yielded values" do
      @y.m(1, 2) { |a, (b, (c, d))| [a, b, c, d] }.should == [1, 2, nil, nil]
    end

    it "destructures a single multi-level Array value yielded" do
      @y.m(1, [2, 3]) { |a, (b, (c, d))| [a, b, c, d] }.should == [1, 2, 3, nil]
    end

    it "destructures a single multi-level Array value yielded" do
      @y.m(1, [2, [3, 4]]) { |a, (b, (c, d))| [a, b, c, d] }.should == [1, 2, 3, 4]
    end
  end

  describe "taking nested |a, ((b, c), d)|" do
    it "assigns nil to the arguments when yielded no values" do
      @y.m { |a, ((b, c), d)| [a, b, c, d] }.should == [nil, nil, nil, nil]
    end

    it "destructures separate yielded values" do
      @y.m(1, 2) { |a, ((b, c), d)| [a, b, c, d] }.should == [1, 2, nil, nil]
    end

    it "destructures a single multi-level Array value yielded" do
      @y.m(1, [2, 3]) { |a, ((b, c), d)| [a, b, c, d] }.should == [1, 2, nil, 3]
    end

    it "destructures a single multi-level Array value yielded" do
      @y.m(1, [[2, 3], 4]) { |a, ((b, c), d)| [a, b, c, d] }.should == [1, 2, 3, 4]
    end
  end

  describe "arguments with _" do
    it "extracts arguments with _" do
      @y.m([[1, 2, 3], 4]) { |(_, a, _), _| a }.should == 2
      @y.m([1, [2, 3, 4]]) { |_, (_, a, _)| a }.should == 3
    end

    it "assigns the first variable named" do
      @y.m(1, 2) { |_, _| _ }.should == 1
    end
  end
end

describe "Block-local variables" do
  # Examples phrased so the concatenation of the describe and it blocks make
  # grammatical sense.
  it "are introduced with a semi-colon in the parameter list" do
    lambda { [1].each {|one; bl| } }.should_not raise_error(SyntaxError)
  end

  it "can be specified in a comma-separated list after the semi-colon" do
    lambda { [1].each {|one; bl, bl2| } }.should_not raise_error(SyntaxError)
  end

  it "can not have the same name as one of the standard parameters" do
    lambda { eval "[1].each {|foo; foo| }" }.should raise_error(SyntaxError)
    lambda { eval "[1].each {|foo, bar; glark, bar| }" }.should raise_error(SyntaxError)
  end

  it "can not be prefixed with an asterisk" do
    lambda { eval "[1].each {|foo; *bar| }" }.should raise_error(SyntaxError)
    lambda do
      eval "[1].each {|foo, bar; glark, *fnord| }"
    end.should raise_error(SyntaxError)
  end

  it "can not be prefixed with an ampersand" do
    lambda { eval "[1].each {|foo; &bar| }" }.should raise_error(SyntaxError)
    lambda do
      eval "[1].each {|foo, bar; glark, &fnord| }"
    end.should raise_error(SyntaxError)
  end

  it "can not be assigned default values" do
    lambda { eval "[1].each {|foo; bar=1| }" }.should raise_error(SyntaxError)
    lambda do
      eval "[1].each {|foo, bar; glark, fnord=:fnord| }"
    end.should raise_error(SyntaxError)
  end

  it "need not be preceeded by standard parameters" do
    lambda { [1].each {|; foo| } }.should_not raise_error(SyntaxError)
    lambda { [1].each {|; glark, bar| } }.should_not raise_error(SyntaxError)
  end

  it "only allow a single semi-colon in the parameter list" do
    lambda { eval "[1].each {|foo; bar; glark| }" }.should raise_error(SyntaxError)
    lambda { eval "[1].each {|; bar; glark| }" }.should raise_error(SyntaxError)
  end

  it "override shadowed variables from the outer scope" do
    out = :out
    [1].each {|; out| out = :in }
    out.should == :out

    a = :a
    b = :b
    c = :c
    d = :d
    {:ant => :bee}.each_pair do |a, b; c, d|
      a = :A
      b = :B
      c = :C
      d = :D
    end
    a.should == :a
    b.should == :b
    c.should == :c
    d.should == :d
  end

  it "are not automatically instantiated in the outer scope" do
    defined?(glark).should be_nil
    [1].each {|;glark| 1}
    defined?(glark).should be_nil
  end

  it "are automatically instantiated in the block" do
    [1].each do |;glark|
      glark.should be_nil
    end
  end
end

describe "Post-args" do
  it "appear after a splat" do
    proc do |*a, b|
      [a, b]
    end.call(1, 2, 3).should == [[1, 2], 3]

    proc do |*a, b, c|
      [a, b, c]
    end.call(1, 2, 3).should == [[1], 2, 3]

    proc do |*a, b, c, d|
      [a, b, c, d]
    end.call(1, 2, 3).should == [[], 1, 2, 3]
  end

  it "are required" do
    lambda {
      lambda do |*a, b|
        [a, b]
      end.call
    }.should raise_error(ArgumentError)
  end

  describe "with required args" do

    it "gathers remaining args in the splat" do
      proc do |a, *b, c|
        [a, b, c]
      end.call(1, 2, 3).should == [1, [2], 3]
    end

    it "has an empty splat when there are no remaining args" do
      proc do |a, b, *c, d|
        [a, b, c, d]
      end.call(1, 2, 3).should == [1, 2, [], 3]

      proc do |a, *b, c, d|
        [a, b, c, d]
      end.call(1, 2, 3).should == [1, [], 2, 3]
    end
  end

  describe "with optional args" do

    it "gathers remaining args in the splat" do
      proc do |a=5, *b, c|
        [a, b, c]
      end.call(1, 2, 3).should == [1, [2], 3]
    end

    it "overrides the optional arg before gathering in the splat" do
      proc do |a=5, *b, c|
        [a, b, c]
      end.call(2, 3).should == [2, [], 3]

      proc do |a=5, b=6, *c, d|
        [a, b, c, d]
      end.call(1, 2, 3).should == [1, 2, [], 3]

      proc do |a=5, *b, c, d|
        [a, b, c, d]
      end.call(1, 2, 3).should == [1, [], 2, 3]
    end

    it "uses the required arg before the optional and the splat" do
      proc do |a=5, *b, c|
        [a, b, c]
      end.call(3).should == [5, [], 3]

      proc do |a=5, b=6, *c, d|
        [a, b, c, d]
      end.call(3).should == [5, 6, [], 3]

      proc do |a=5, *b, c, d|
        [a, b, c, d]
      end.call(2, 3).should == [5, [], 2, 3]
    end

    it "overrides the optional args from left to right before gathering the splat" do
      proc do |a=5, b=6, *c, d|
        [a, b, c, d]
      end.call(2, 3).should == [2, 6, [], 3]
    end
  end

  describe "with pattern matching" do
    it "extracts matched blocks with post arguments" do
      proc do |(a, *b, c), d, e|
        [a, b, c, d, e]
      end.call([1, 2, 3, 4], 5, 6).should == [1, [2, 3], 4, 5, 6]
    end

    it "allows empty splats" do
      proc do |a, (*), b|
        [a, b]
      end.call([1, 2, 3]).should == [1, 3]
    end
  end
end
