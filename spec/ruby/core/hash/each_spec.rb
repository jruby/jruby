require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../shared/iteration', __FILE__)
require File.expand_path('../shared/each', __FILE__)

describe "Hash#each" do
  it_behaves_like(:hash_each, :each)
  it_behaves_like(:hash_iteration_no_block, :each)
end
