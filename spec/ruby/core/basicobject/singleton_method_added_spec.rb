require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../../../shared/kernel/singleton_method_added', __FILE__)

describe "BasicObject#singleton_method_added" do
  it_behaves_like(:singleton_method_added, :singleton_method_added, BasicObject)
end
