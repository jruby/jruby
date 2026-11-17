# frozen_string_literal: false
require_relative '../../spec_helper'

describe 'String#-@' do
  it 'returns self if the String is frozen' do
    input  = 'foo'.freeze
    output = -input

    output.should.equal?(input)
    output.should.frozen?
  end

  it 'returns a frozen copy if the String is not frozen' do
    input  = 'foo'
    output = -input

    output.should.frozen?
    output.should_not.equal?(input)
    output.should == 'foo'
  end

  it "returns the same object for equal unfrozen strings" do
    origin = "this is a string"
    dynamic = %w(this is a string).join(' ')

    origin.should_not.equal?(dynamic)
    (-origin).should.equal?(-dynamic)
  end

  it "returns the same object when it's called on the same String literal" do
    (-"unfrozen string").should.equal?(-"unfrozen string")
    (-"unfrozen string").should_not.equal?(-"another unfrozen string")
  end

  it "deduplicates frozen strings" do
    dynamic = %w(this string is frozen).join(' ').freeze

    dynamic.should_not.equal?("this string is frozen".freeze)

    (-dynamic).should.equal?("this string is frozen".freeze)
    (-dynamic).should.equal?((-"this string is frozen").freeze)
  end

  it "does not deduplicate a frozen string when it has instance variables" do
    dynamic = %w(this string is frozen).join(' ')
    dynamic.instance_variable_set(:@a, 1)
    dynamic.freeze

    (-dynamic).should_not.equal?("this string is frozen".freeze)
    (-dynamic).should_not.equal?((-"this string is frozen").freeze)
    (-dynamic).should.equal?(-dynamic)
  end

  it "returns a frozen copy and does not freeze self if the String is not frozen and has instance variables" do
    input = %w(this string has an ivar).join(' ')
    input.instance_variable_set(:@a, 1)
    output = -input

    output.should.frozen?
    output.should_not.equal?(input)
    output.should == 'this string has an ivar'
    input.should_not.frozen?
  end

  it "returns a frozen copy and does not freeze self if the String is not frozen and is an instance of a subclass" do
    subclass = Class.new(String)
    input = subclass.new('foo')
    output = -input

    output.should.frozen?
    output.should_not.equal?(input)
    output.should == 'foo'
    input.should_not.frozen?
  end
end
