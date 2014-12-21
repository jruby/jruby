require File.expand_path('../shared/union', __FILE__)
require 'set'

describe "SortedSet#+" do
  it_behaves_like :sorted_set_union, :+
end
