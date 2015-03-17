require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do
  require 'enumerator'
  require File.expand_path('../../../shared/enumerator/new', __FILE__)

  describe "Enumerator.new" do
    it "requires an argument" do
      lambda {enumerator_class.new}.should raise_error(ArgumentError)
    end

    it_behaves_like(:enum_new, :new)
  end
end
