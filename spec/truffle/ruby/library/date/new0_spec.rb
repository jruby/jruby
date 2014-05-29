require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/new_bang', __FILE__)
require 'date'

ruby_version_is "" ... "1.9" do
  describe "Date.new0" do
    it_behaves_like :date_new_bang, :new0
  end
end
