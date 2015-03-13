require File.expand_path('../shared/intersection', __FILE__)
require 'set'

describe "Set#intersection" do
  it_behaves_like :set_intersection, :intersection
end

describe "Set#&" do
  it_behaves_like :set_intersection, :&
end
