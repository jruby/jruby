require_relative '../../spec_helper'
require_relative 'fixtures/classes'

describe "Module#ruby2_keywords" do
  class << self
    ruby2_keywords def mark(*args)
      args
    end
  end

  it "marks the final hash argument as keyword hash" do
    last = mark(1, 2, a: "a").last
    Hash.ruby2_keywords_hash?(last).should == true
  end

  it "makes a copy of the hash and only marks the copy as keyword hash" do
    obj = Object.new
    obj.singleton_class.class_exec do
      def regular(*args)
        args.last
      end
    end

    h = {a: 1}

    last = mark(**h).last
    Hash.ruby2_keywords_hash?(last).should == true
    Hash.ruby2_keywords_hash?(h).should == false

    last2 = mark(**last).last # last is already marked
    Hash.ruby2_keywords_hash?(last2).should == true
    Hash.ruby2_keywords_hash?(last).should == true
    last2.should_not.equal?(last)
    Hash.ruby2_keywords_hash?(h).should == false
  end

  it "makes a copy and unmark the Hash when calling a method taking (arg)" do
    obj = Object.new
    obj.singleton_class.class_exec do
      def single(arg)
        arg
      end
    end

    h = { a: 1 }
    args = mark(**h)
    marked = args.last
    Hash.ruby2_keywords_hash?(marked).should == true

    after_usage = obj.single(*args)
    after_usage.should == h
    after_usage.should_not.equal?(h)
    after_usage.should_not.equal?(marked)
    Hash.ruby2_keywords_hash?(after_usage).should == false
    Hash.ruby2_keywords_hash?(marked).should == true
  end

  it "makes a copy and unmark the Hash when calling a method taking (**kw)" do
    obj = Object.new
    obj.singleton_class.class_exec do
      def kwargs(**kw)
        kw
      end
    end

    h = { a: 1 }
    args = mark(**h)
    marked = args.last
    Hash.ruby2_keywords_hash?(marked).should == true

    after_usage = obj.kwargs(*args)
    after_usage.should == h
    after_usage.should_not.equal?(h)
    after_usage.should_not.equal?(marked)
    Hash.ruby2_keywords_hash?(after_usage).should == false
    Hash.ruby2_keywords_hash?(marked).should == true
  end

  it "makes a copy and unmark the Hash when calling a method taking (*args)" do
    obj = Object.new
    obj.singleton_class.class_exec do
      def splat(*args)
        args.last
      end

      def splat1(arg, *args)
        args.last
      end

      def proc_call(*args)
        -> *a { a.last }.call(*args)
      end
    end

    h = { a: 1 }
    args = mark(**h)
    marked = args.last
    Hash.ruby2_keywords_hash?(marked).should == true

    after_usage = obj.splat(*args)
    after_usage.should == h
    after_usage.should_not.equal?(h)
    after_usage.should_not.equal?(marked)
    Hash.ruby2_keywords_hash?(after_usage).should == false
    Hash.ruby2_keywords_hash?(marked).should == true

    args = mark(1, **h)
    marked = args.last
    after_usage = obj.splat1(*args)
    after_usage.should == h
    after_usage.should_not.equal?(h)
    after_usage.should_not.equal?(marked)
    Hash.ruby2_keywords_hash?(after_usage).should == false
    Hash.ruby2_keywords_hash?(marked).should == true

    args = mark(**h)
    marked = args.last
    after_usage = obj.proc_call(*args)
    after_usage.should == h
    after_usage.should_not.equal?(h)
    after_usage.should_not.equal?(marked)
    Hash.ruby2_keywords_hash?(after_usage).should == false
    Hash.ruby2_keywords_hash?(marked).should == true

    args = mark(**h)
    marked = args.last
    after_usage = obj.send(:splat, *args)
    after_usage.should == h
    after_usage.should_not.equal?(h)
    after_usage.should_not.equal?(marked)
    Hash.ruby2_keywords_hash?(after_usage).should == false
    Hash.ruby2_keywords_hash?(marked).should == true
  end

  it "applies to the underlying method and applies across aliasing" do
    obj = Object.new

    obj.singleton_class.class_exec do
      def foo(*a) a.last end
      alias_method :bar, :foo
      ruby2_keywords :foo

      def baz(*a) a.last end
      ruby2_keywords :baz
      alias_method :bob, :baz
    end

    last = obj.foo(1, 2, a: "a")
    Hash.ruby2_keywords_hash?(last).should == true

    last = obj.bar(1, 2, a: "a")
    Hash.ruby2_keywords_hash?(last).should == true

    last = obj.baz(1, 2, a: "a")
    Hash.ruby2_keywords_hash?(last).should == true

    last = obj.bob(1, 2, a: "a")
    Hash.ruby2_keywords_hash?(last).should == true
  end

  it "returns nil" do
    obj = Object.new

    obj.singleton_class.class_exec do
      def foo(*a) end

      ruby2_keywords(:foo).should == nil
    end
  end

  it "raises NameError when passed not existing method name" do
    obj = Object.new

    -> {
      obj.singleton_class.class_exec do
        ruby2_keywords :not_existing
      end
    }.should raise_error(NameError, /undefined method [`']not_existing'/)
  end

  it "accepts String as well" do
    obj = Object.new

    obj.singleton_class.class_exec do
      def foo(*a) a.last end
      ruby2_keywords "foo"
    end

    last = obj.foo(1, 2, a: "a")
    Hash.ruby2_keywords_hash?(last).should == true
  end

  it "raises TypeError when passed not Symbol or String" do
    obj = Object.new

    -> {
      obj.singleton_class.class_exec do
        ruby2_keywords Object.new
      end
    }.should raise_error(TypeError, /is not a symbol nor a string/)
  end

  it "prints warning when a method does not accept argument splat" do
    obj = Object.new
    def obj.foo(a, b, c) end

    -> {
      obj.singleton_class.class_exec do
        ruby2_keywords :foo
      end
    }.should complain(/Skipping set of ruby2_keywords flag for/)
  end

  it "prints warning when a method accepts keywords" do
    obj = Object.new
    def obj.foo(a:, b:) end

    -> {
      obj.singleton_class.class_exec do
        ruby2_keywords :foo
      end
    }.should complain(/Skipping set of ruby2_keywords flag for/)
  end

  it "prints warning when a method accepts keyword splat" do
    obj = Object.new
    def obj.foo(**a) end

    -> {
      obj.singleton_class.class_exec do
        ruby2_keywords :foo
      end
    }.should complain(/Skipping set of ruby2_keywords flag for/)
  end

  describe "used on a method that forwards rest arguments" do
    it "properly forwards to a keyword arguments-receiving method" do
      obj = Class.new do
        def foo(*a, **b)
          [a, b]
        end

        ruby2_keywords def bar(*d)
          foo(*d)
        end

        def test(...)
          bar(...)
        end
      end.new

      # Use test forwarding method to ensure bar call site is used repeatedly
      # See https://github.com/jruby/jruby/issues/8920#issuecomment-3097667358
      obj.test().should == [[], {}]
      obj.test(e: 3).should == [[], {e: 3}]
      obj.test(1).should == [[1], {}]
      obj.test(1, e: 3).should == [[1], {e: 3}]
    end
  end
end
