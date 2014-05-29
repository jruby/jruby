require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../shared/__method__', __FILE__)

describe "Kernel.__callee__" do
  ruby_version_is '1.9' do
    it_behaves_like(:kernel___method__, :__callee__)
  end
end
