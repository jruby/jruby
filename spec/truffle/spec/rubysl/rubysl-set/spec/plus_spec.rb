require File.expand_path('../shared/union', __FILE__)
require 'set'

describe "Set#+" do
  it_behaves_like :set_union, :+
end
