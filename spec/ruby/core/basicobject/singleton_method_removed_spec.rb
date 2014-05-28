require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../shared/kernel/singleton_method_removed', __FILE__)

describe "BasicObject#singleton_method_removed" do
  it_behaves_like(:singleton_method_removed, :singleton_method_removed, BasicObject)
end
