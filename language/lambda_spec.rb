require File.expand_path('../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "A lambda literal -> () { }" do
  SpecEvaluate.desc = "for definition"

  it "returns a Proc object when used in a BasicObject method" do
    klass = Class.new(BasicObject) do
      def create_lambda
        -> () { }
      end
    end

    klass.new.create_lambda.should be_an_instance_of(Proc)
  end

  it "does not execute the block" do
    ->() { fail }.should be_an_instance_of(Proc)
  end

  it "returns a lambda" do
    -> () { }.lambda?.should be_true
  end

  context "assigns no local variables" do
    evaluate <<-ruby do
        @a = -> { }
        @b = ->() { }
        @c = -> () { }
        @d = -> do end
      ruby

      @a.().should be_nil
      @b.().should be_nil
      @c.().should be_nil
      @d.().should be_nil
    end
  end

  context "assigns variables from parameters" do
    evaluate <<-ruby do
        @a = -> (a) { a }
      ruby

      @a.(1).should == 1
    end

    evaluate <<-ruby do
        @a = -> ((a)) { a }
      ruby

      @a.(1).should == 1
      @a.([1, 2, 3]).should == 1
    end

    evaluate <<-ruby do
        @a = -> ((*a, b)) { [a, b] }
      ruby

      @a.(1).should == [[], 1]
      @a.([1, 2, 3]).should == [[1, 2], 3]
    end

    evaluate <<-ruby do
        @a = -> (a={}) { a }
      ruby

      @a.().should == {}
      @a.(2).should == 2
    end

    evaluate <<-ruby do
        @a = -> (*) { }
      ruby

      @a.().should be_nil
      @a.(1).should be_nil
      @a.(1, 2, 3).should be_nil
    end

    evaluate <<-ruby do
        @a = -> (*a) { a }
      ruby

      @a.().should == []
      @a.(1).should == [1]
      @a.(1, 2, 3).should == [1, 2, 3]
    end

    ruby_version_is "2.1" do
      evaluate <<-ruby do
          @a = -> (a:) { a }
        ruby

        lambda { @a.() }.should raise_error(ArgumentError)
        @a.(a: 1).should == 1
      end
    end

    evaluate <<-ruby do
        @a = -> (a: 1) { a }
      ruby

      @a.().should == 1
      @a.(a: 2).should == 2
    end

    evaluate <<-ruby do
        @a = -> (**) {  }
      ruby

      @a.().should be_nil
      @a.(a: 1, b: 2).should be_nil
      lambda { @a.(1) }.should raise_error(ArgumentError)
    end

    evaluate <<-ruby do
        @a = -> (**k) { k }
      ruby

      @a.().should == {}
      @a.(a: 1, b: 2).should == {a: 1, b: 2}
    end

    evaluate <<-ruby do
        @a = -> (&b) { b  }
      ruby

      @a.().should be_nil
      @a.() { }.should be_an_instance_of(Proc)
    end

    evaluate <<-ruby do
        @a = -> (a, b) { [a, b] }
      ruby

      @a.(1, 2).should == [1, 2]
      lambda { @a.() }.should raise_error(ArgumentError)
      lambda { @a.(1) }.should raise_error(ArgumentError)
    end

    evaluate <<-ruby do
        @a = -> ((a, b, *c, d), (*e, f, g), (*h)) do
          [a, b, c, d, e, f, g, h]
        end
      ruby

      @a.(1, 2, 3).should == [1, nil, [], nil, [], 2, nil, [3]]
      result = @a.([1, 2, 3], [4, 5, 6, 7, 8], [9, 10])
      result.should == [1, 2, [], 3, [4, 5, 6], 7, 8, [9, 10]]
    end

    evaluate <<-ruby do
        @a = -> (a, (b, (c, *d, (e, (*f)), g), (h, (i, j)))) do
          [a, b, c, d, e, f, g, h, i, j]
        end
      ruby

      @a.(1, 2).should == [1, 2, nil, [], nil, [nil], nil, nil, nil, nil]
      result = @a.(1, [2, [3, 4, 5, [6, [7, 8]], 9], [10, [11, 12]]])
      result.should == [1, 2, 3, [4, 5], 6, [7, 8], 9, 10, 11, 12]
    end

    evaluate <<-ruby do
        @a = -> (*, **k) { k }
      ruby

      @a.().should == {}
      @a.(1, 2, 3, a: 4, b: 5).should == {a: 4, b: 5}

      h = mock("keyword splat")
      h.should_receive(:to_hash).and_return({a: 1})
      @a.(h).should == {a: 1}
    end

    evaluate <<-ruby do
        @a = -> (*, &b) { b }
      ruby

      @a.().should be_nil
      @a.(1, 2, 3, 4).should be_nil
      @a.(&(l = ->{})).should equal(l)
    end

    ruby_version_is "2.1" do
      evaluate <<-ruby do
          @a = -> (a:, b:) { [a, b] }
        ruby

        @a.(a: 1, b: 2).should == [1, 2]
      end

      evaluate <<-ruby do
          @a = -> (a:, b: 1) { [a, b] }
        ruby

        @a.(a: 1).should == [1, 1]
        @a.(a: 1, b: 2).should == [1, 2]
      end

      evaluate <<-ruby do
          @a = -> (a: 1, b:) { [a, b] }
        ruby

        @a.(b: 0).should == [1, 0]
        @a.(b: 2, a: 3).should == [3, 2]
      end

      evaluate <<-ruby do
          @a = -> (a: @a = -> (a: 1) { a }, b:) do
            [a, b]
          end
        ruby

        @a.(a: 2, b: 3).should == [2, 3]
        @a.(b: 1).should == [@a, 1]

        # Note the default value of a: in the original method.
        @a.().should == 1
      end
    end

    evaluate <<-ruby do
        @a = -> (a: 1, b: 2) { [a, b] }
      ruby

      @a.().should == [1, 2]
      @a.(b: 3, a: 4).should == [4, 3]
    end

    ruby_version_is "2.1" do
      evaluate <<-ruby do
          @a = -> (a, b=1, *c, (*d, (e)), f: 2, g:, h:, **k, &l) do
            [a, b, c, d, e, f, g, h, k, l]
          end
        ruby

        result = @a.(9, 8, 7, 6, f: 5, g: 4, h: 3, &(l = ->{}))
        result.should == [9, 8, [7], [], 6, 5, 4, 3, {}, l]
      end

      evaluate <<-ruby do
          @a = -> a, b=1, *c, d, e:, f: 2, g:, **k, &l do
            [a, b, c, d, e, f, g, k, l]
          end
        ruby

        result = @a.(1, 2, e: 3, g: 4, h: 5, i: 6, &(l = ->{}))
        result.should == [1, 1, [], 2, 3, 2, 4, { h: 5, i: 6 }, l]
      end
    end
  end
end

describe "A lambda expression 'lambda { ... }'" do
  SpecEvaluate.desc = "for definition"

  it "calls the #lambda method" do
    obj = mock("lambda definition")
    obj.should_receive(:lambda).and_return(obj)

    def obj.define
      lambda { }
    end

    obj.define.should equal(obj)
  end

  it "does not execute the block" do
    lambda { fail }.should be_an_instance_of(Proc)
  end

  it "returns a lambda" do
    lambda { }.lambda?.should be_true
  end

  context "assigns no local variables" do
    evaluate <<-ruby do
        @a = lambda { }
        @b = lambda { || }
      ruby

      @a.().should be_nil
      @b.().should be_nil
    end
  end

  context "assigns variables from parameters" do
    evaluate <<-ruby do
        @a = lambda { |a| a }
      ruby

      @a.(1).should == 1
    end

    evaluate <<-ruby do
        def m(*a) yield(*a) end
        @a = lambda { |a| a }
      ruby

      lambda { m(&@a) }.should raise_error(ArgumentError)
      lambda { m(1, 2, &@a) }.should raise_error(ArgumentError)
    end

    evaluate <<-ruby do
        @a = lambda { |a, | a }
      ruby

      @a.(1).should == 1
      @a.([1, 2]).should == [1, 2]

      lambda { @a.() }.should raise_error(ArgumentError)
      lambda { @a.(1, 2) }.should raise_error(ArgumentError)
    end

    evaluate <<-ruby do
        def m(a) yield a end
        def m2() yield end

        @a = lambda { |a, | a }
      ruby

      m(1, &@a).should == 1
      m([1, 2], &@a).should == [1, 2]

      lambda { m2(&@a) }.should raise_error(ArgumentError)
    end

    evaluate <<-ruby do
        @a = lambda { |(a)| a }
      ruby

      @a.(1).should == 1
      @a.([1, 2, 3]).should == 1
    end

    evaluate <<-ruby do
        @a = lambda { |(*a, b)| [a, b] }
      ruby

      @a.(1).should == [[], 1]
      @a.([1, 2, 3]).should == [[1, 2], 3]
    end

    evaluate <<-ruby do
        @a = lambda { |a={}| a }
      ruby

      @a.().should == {}
      @a.(2).should == 2
    end

    evaluate <<-ruby do
        @a = lambda { |*| }
      ruby

      @a.().should be_nil
      @a.(1).should be_nil
      @a.(1, 2, 3).should be_nil
    end

    evaluate <<-ruby do
        @a = lambda { |*a| a }
      ruby

      @a.().should == []
      @a.(1).should == [1]
      @a.(1, 2, 3).should == [1, 2, 3]
    end

    ruby_version_is "2.1" do
      evaluate <<-ruby do
          @a = lambda { |a:| a }
        ruby

        lambda { @a.() }.should raise_error(ArgumentError)
        @a.(a: 1).should == 1
      end
    end

    evaluate <<-ruby do
        @a = lambda { |a: 1| a }
      ruby

      @a.().should == 1
      @a.(a: 2).should == 2
    end

    evaluate <<-ruby do
        @a = lambda { |**|  }
      ruby

      @a.().should be_nil
      @a.(a: 1, b: 2).should be_nil
      lambda { @a.(1) }.should raise_error(ArgumentError)
    end

    evaluate <<-ruby do
        @a = lambda { |**k| k }
      ruby

      @a.().should == {}
      @a.(a: 1, b: 2).should == {a: 1, b: 2}
    end

    evaluate <<-ruby do
        @a = lambda { |&b| b  }
      ruby

      @a.().should be_nil
      @a.() { }.should be_an_instance_of(Proc)
    end

    evaluate <<-ruby do
        @a = lambda { |a, b| [a, b] }
      ruby

      @a.(1, 2).should == [1, 2]
    end

    evaluate <<-ruby do
        @a = lambda do |(a, b, *c, d), (*e, f, g), (*h)|
          [a, b, c, d, e, f, g, h]
        end
      ruby

      @a.(1, 2, 3).should == [1, nil, [], nil, [], 2, nil, [3]]
      result = @a.([1, 2, 3], [4, 5, 6, 7, 8], [9, 10])
      result.should == [1, 2, [], 3, [4, 5, 6], 7, 8, [9, 10]]
    end

    evaluate <<-ruby do
        @a = lambda do |a, (b, (c, *d, (e, (*f)), g), (h, (i, j)))|
          [a, b, c, d, e, f, g, h, i, j]
        end
      ruby

      @a.(1, 2).should == [1, 2, nil, [], nil, [nil], nil, nil, nil, nil]
      result = @a.(1, [2, [3, 4, 5, [6, [7, 8]], 9], [10, [11, 12]]])
      result.should == [1, 2, 3, [4, 5], 6, [7, 8], 9, 10, 11, 12]
    end

    evaluate <<-ruby do
        @a = lambda { |*, **k| k }
      ruby

      @a.().should == {}
      @a.(1, 2, 3, a: 4, b: 5).should == {a: 4, b: 5}

      h = mock("keyword splat")
      h.should_receive(:to_hash).and_return({a: 1})
      @a.(h).should == {a: 1}
    end

    evaluate <<-ruby do
        @a = lambda { |*, &b| b }
      ruby

      @a.().should be_nil
      @a.(1, 2, 3, 4).should be_nil
      @a.(&(l = ->{})).should equal(l)
    end

    ruby_version_is "2.1" do
      evaluate <<-ruby do
          @a = lambda { |a:, b:| [a, b] }
        ruby

        @a.(a: 1, b: 2).should == [1, 2]
      end

      evaluate <<-ruby do
          @a = lambda { |a:, b: 1| [a, b] }
        ruby

        @a.(a: 1).should == [1, 1]
        @a.(a: 1, b: 2).should == [1, 2]
      end

      evaluate <<-ruby do
          @a = lambda { |a: 1, b:| [a, b] }
        ruby

        @a.(b: 0).should == [1, 0]
        @a.(b: 2, a: 3).should == [3, 2]
      end

      evaluate <<-ruby do
          @a = lambda do |a: (@a = -> (a: 1) { a }), b:|
            [a, b]
          end
        ruby

        @a.(a: 2, b: 3).should == [2, 3]
        @a.(b: 1).should == [@a, 1]

        # Note the default value of a: in the original method.
        @a.().should == 1
      end
    end

    evaluate <<-ruby do
        @a = lambda { |a: 1, b: 2| [a, b] }
      ruby

      @a.().should == [1, 2]
      @a.(b: 3, a: 4).should == [4, 3]
    end

    ruby_version_is "2.1" do
      evaluate <<-ruby do
          @a = lambda do |a, b=1, *c, (*d, (e)), f: 2, g:, h:, **k, &l|
            [a, b, c, d, e, f, g, h, k, l]
          end
        ruby

        result = @a.(9, 8, 7, 6, f: 5, g: 4, h: 3, &(l = ->{}))
        result.should == [9, 8, [7], [], 6, 5, 4, 3, {}, l]
      end

      evaluate <<-ruby do
          @a = lambda do |a, b=1, *c, d, e:, f: 2, g:, **k, &l|
            [a, b, c, d, e, f, g, k, l]
          end
        ruby

        result = @a.(1, 2, e: 3, g: 4, h: 5, i: 6, &(l = ->{}))
        result.should == [1, 1, [], 2, 3, 2, 4, { h: 5, i: 6 }, l]
      end
    end
  end
end
