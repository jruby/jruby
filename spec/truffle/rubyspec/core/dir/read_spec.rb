require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/common', __FILE__)
require File.expand_path('../shared/closed', __FILE__)

describe "Dir#read" do
  before :all do
    DirSpecs.create_mock_dirs
  end

  after :all do
    DirSpecs.delete_mock_dirs
  end

  it "returns the file name in the current seek position" do
    # an FS does not necessarily impose order
    ls = Dir.entries DirSpecs.mock_dir
    dir = Dir.open DirSpecs.mock_dir
    ls.should include(dir.read)
    dir.close
  end

  it_behaves_like :dir_closed, :read
end
