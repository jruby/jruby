require File.dirname(__FILE__) + "/../spec_helper"

describe "Classes that implement Iterable" do
  before do
    @strings = ['chunky', 'bacon', 'fox tall', 'fox small']
    @i = Java::JavaIterable.new(@strings)
  end

  it "should provide #each" do
    strings = []
    @i.each {|s| strings << s }
    expect(strings).to eq(@strings)
  end

  it "should provide #each_with_index" do
    block_ran = false

    @i.each_with_index do |string, i|
      block_ran = true
      expect(string).to eq(@strings[i])
    end

    expect(block_ran).to be(true)
  end

  it "should provide #map" do
    mapped = @i.map{|s| s.reverse }
    expect(mapped).to eq(@strings.map{|s| s.reverse })
  end
end
