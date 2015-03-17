require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/valid_commercial', __FILE__)
require 'date'

ruby_version_is "" ... "1.9" do
  describe "Date.existw?" do
    it_behaves_like :date_valid_commercial?, :existw?
  end
end
