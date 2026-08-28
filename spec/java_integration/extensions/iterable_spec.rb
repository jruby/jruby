require File.dirname(__FILE__) + "/../spec_helper"

describe 'java.lang.Iterable' do

  # java.nio.file.Path extends Iterable<Path>

  it 'iterates using #each' do
    file_system = java.nio.file.FileSystems.getDefault
    path = file_system.getPath(__FILE__)
    paths = []
    ret = path.each { |p| paths << p.to_s }
    expect( paths ).to_not be_empty
    expect( paths.last ).to eq 'iterable_spec.rb'
    expect( ret ).to be path
  end

  it 'iterates with an Enumerator on #each' do
    file_system = java.nio.file.FileSystems.getDefault
    path = file_system.getPath(__FILE__)
    enum = path.each
    expect( enum.next ).to_not be nil
  end

  it 'iterates using #each_with_index' do
    file_system = java.nio.file.FileSystems.getDefault
    path = file_system.getPath(__FILE__)
    paths = []
    ret = path.each_with_index { |p| paths << p }
    expect( paths ).to_not be_empty
    expect( paths[-1][0].to_s ).to eq 'iterable_spec.rb'
    expect( paths[-1][1] ).to eq paths.size - 1
    expect( ret ).to eql path

    paths = []; idxs = []
    ret = path.each_with_index { |p, i| paths << p; idxs << i }
    expect( paths ).to_not be_empty
    expect( paths[-1].to_s ).to eq 'iterable_spec.rb'
    expect( idxs[0] ).to eq 0
    expect( idxs[-1] ).to eq paths.size - 1
    expect( ret ).to be path
  end

  it 'iterates with an Enumerator on #each_with_index' do
    file_system = java.nio.file.FileSystems.getDefault
    path = file_system.getPath(__FILE__)
    enum = path.each_with_index
    n = enum.next
    expect( n[0] ).to_not be nil
    expect( n[1] ).to eql 0
  end

  it 'does #map' do
    file_system = java.nio.file.FileSystems.getDefault
    path = file_system.getPath(__FILE__)
    paths = path.map { |p| p.to_s.upcase }
    expect( paths ).to_not be_empty
    expect( paths.last ).to eq 'ITERABLE_SPEC.RB'
  end

  it 'converts #to_a' do
    file_system = java.nio.file.FileSystems.getDefault
    path = file_system.getPath(__FILE__)
    expect( path.to_a ).to_not be_empty
    expect( path.to_a ).to eql iterate_path(path)
  end

  it 'counts' do
    file_system = java.nio.file.FileSystems.getDefault
    path = file_system.getPath(__FILE__)
    expect( path.count ).to eq iterate_path(path).size

    expect( path.count { |p| p.to_s == 'iterable_spec.rb' } ).to eq 1
    a_path = iterate_path(path).last

    path = file_system.getPath(__FILE__)
    expect( path.count(nil) ).to eq 0
    expect( path.count(a_path) ).to eq 1
  end

  it 'converts #to_a' do
    file_system = java.nio.file.FileSystems.getDefault
    path = file_system.getPath(__FILE__)
    expect( path.to_a ).to_not be_empty
    expect( path.to_a ).to eql iterate_path(path)
  end

  private

  def iterate_path(path)
    res = [] ; it = path.iterator
    while it.hasNext ; res << it.next  end
    res
  end

  describe 'Ruby class' do

    class RubyIterableWrapper
      include java.lang.Iterable

      def initialize(coll) @coll = coll end

      def iterator; @coll.iterator end

    end

    it 'iterates as an Enumerable' do
      wrapper = RubyIterableWrapper.new coll = java.util.ArrayList.new([1, 2, 3])
      elems = [] ; wrapper.map { |el| elems << el + 1 }
      expect( elems ).to eql [2, 3, 4]
      expect( wrapper.to_a ).to eql [1, 2, 3]

      elems = [] ; wrapper.each_with_index { |el, i| elems << [el, i] }
      expect( elems ).to eql [[1, 0], [2, 1], [3, 2]]
    end

  end

end
