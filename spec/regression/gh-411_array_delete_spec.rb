#!/usr/bin/env ruby
require 'rspec'

class Foo
  attr_reader :name, :age

  def initialize name, age
    @name = name
    @age = age
  end

  def == other
    other.name == name
  end
end

describe 'Array#delete' do
  context 'when "==" is overridden' do
    before :each do
      @foo1 = Foo.new "John Shahid", 27
      @foo2 = Foo.new "John Shahid", 28
      @array = [@foo1]
    end

    it 'should return the element in the array' do
      temp = @array.delete @foo2
      temp.age.should == @foo1.age
    end
  end
end
