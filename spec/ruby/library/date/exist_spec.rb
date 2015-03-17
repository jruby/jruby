require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/valid_civil', __FILE__)
require 'date'

ruby_version_is "" ... "1.9" do
  describe "Date.exist?" do
    it_behaves_like :date_valid_civil?, :exist?
  end
end
