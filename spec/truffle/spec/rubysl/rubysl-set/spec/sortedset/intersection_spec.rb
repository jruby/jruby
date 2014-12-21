require File.expand_path('../shared/intersection', __FILE__)
require 'set'

describe "SortedSet#intersection" do
  it_behaves_like :sorted_set_intersection, :intersection
end

describe "SortedSet#&" do
  it_behaves_like :sorted_set_intersection, :&
end
