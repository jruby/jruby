require File.expand_path('../../../shared/complex/minus', __FILE__)

ruby_version_is ""..."1.9" do
  require 'complex'

  describe "Complex#-" do
    it_behaves_like :complex_minus, :-
  end
end
