require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/civil', __FILE__)
require 'date'

ruby_version_is "" ... "1.9" do
  describe "Date.new3" do
    it_behaves_like :date_civil, :new3
  end
end
