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
end
