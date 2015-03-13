require 'set'
require File.expand_path('../shared/difference', __FILE__)

describe "Set#difference" do
  it_behaves_like :set_difference, :difference
end
