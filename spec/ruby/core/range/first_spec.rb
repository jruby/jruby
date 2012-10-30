require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/begin', __FILE__)

describe "Range#first" do
  it_behaves_like(:range_begin, :first)
end
