require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/new', __FILE__)
require File.expand_path('../fixtures/classes.rb', __FILE__)

ruby_version_is ''...'2.0' do 
  describe "Iconv.new" do
    it_behaves_like :iconv_new, :new
  end
end
