require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../shared/send', __FILE__)

describe "Kernel#__send__" do
  it_behaves_like(:kernel_send, :__send__)
end
