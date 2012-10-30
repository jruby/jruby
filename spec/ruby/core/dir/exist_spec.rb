require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/common', __FILE__)
require File.expand_path('../shared/exists', __FILE__)

ruby_version_is "1.9" do
  describe "Dir.exist?" do
    before :all do
      DirSpecs.create_mock_dirs
    end

    after :all do
      DirSpecs.delete_mock_dirs
    end

    it_behaves_like(:dir_exists, :exist?)
  end
end
