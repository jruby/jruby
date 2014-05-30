require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/kind_of', __FILE__)

describe "Object#is_a?" do
  it_behaves_like(:object_kind_of , :is_a?)
end
