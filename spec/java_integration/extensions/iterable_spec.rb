require File.dirname(__FILE__) + "/../spec_helper"

describe 'java.lang.Iterable' do

  # java.nio.file.Path extends Iterable<Path>

  it 'iterates using #each' do
    file_system = java.nio.file.FileSystems.getDefault
    path = file_system.getPath(__FILE__)
    paths = []
    path.each { |p| paths << p.to_s }
    expect( paths ).to_not be_empty
    expect( paths.last ).to eq 'iterable_spec.rb'
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
    paths = []; idxs = []
    path.each_with_index { |p| paths << p }
    expect( paths ).to_not be_empty
    expect( paths[-1][0].to_s ).to eq 'iterable_spec.rb'
    expect( paths[-1][1] ).to eq paths.size - 1
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

end
