require_relative '../../spec_helper'
require_relative 'fixtures/classes'

describe "Enumerable#to_h" do
  it "converts empty enumerable to empty hash" do
    enum = EnumerableSpecs::EachDefiner.new
    enum.to_h.should == {}
  end

  it "converts yielded [key, value] pairs to a hash" do
    enum = EnumerableSpecs::EachDefiner.new([:a, 1], [:b, 2])
    enum.to_h.should == { a: 1, b: 2 }
  end

  it "uses the last value of a duplicated key" do
    enum = EnumerableSpecs::EachDefiner.new([:a, 1], [:b, 2], [:a, 3])
    enum.to_h.should == { a: 3, b: 2 }
  end

  it "calls #to_ary on contents" do
    pair = mock('to_ary')
    pair.should_receive(:to_ary).and_return([:b, 2])
    enum = EnumerableSpecs::EachDefiner.new([:a, 1], pair)
    enum.to_h.should == { a: 1, b: 2 }
  end

  it "forwards arguments to #each" do
    enum = Object.new
    def enum.each(*args)
      yield(*args)
      yield([:b, 2])
    end
    enum.extend Enumerable
    enum.to_h(:a, 1).should == { a: 1, b: 2 }
  end

  it "raises TypeError if an element is not an array" do
    enum = EnumerableSpecs::EachDefiner.new(:x)
    lambda { enum.to_h }.should raise_error(TypeError)
  end

  it "raises ArgumentError if an element is not a [key, value] pair" do
    enum = EnumerableSpecs::EachDefiner.new([:x])
    lambda { enum.to_h }.should raise_error(ArgumentError)
  end

  ruby_version_is "2.6" do
    it "converts [key, value] pairs returned by the block to a hash" do
      enum = EnumerableSpecs::EachDefiner.new(:a, :b)
      i = 0
      enum.to_h {|k| [k, i += 1]}.should == { a: 1, b: 2 }
    end

    it "converts the block's result to an array using to_ary" do
      pair = mock('to_ary')
      pair.should_receive(:to_ary).exactly(3).times.and_return([:b, 2])
      enum = EnumerableSpecs::EachDefiner.new(1, 2, 3)
      enum.to_h { pair }.should == { b: 2 }
    end

    it "stops calling the block after the first wrong result" do
      enum = EnumerableSpecs::EachDefiner.new([:ok, 1], [:not_ok], :not_called)
      called = 0
      lambda {
        enum.to_h { |x| called += 1; x }
      }.should raise_error(ArgumentError)
      called.should == 2
    end

    it "doesn't convert the block's result to an array using to_a" do
      pair = mock('to_a')
      pair.should_not_receive(:to_a)
      enum = EnumerableSpecs::EachDefiner.new(1, 2, 3)
      lambda { enum.to_h { pair } }.should raise_error(TypeError)
    end
  end
end
