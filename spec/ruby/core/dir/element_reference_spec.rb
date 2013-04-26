require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/common', __FILE__)
require File.expand_path('../shared/glob', __FILE__)

describe "Dir.[]" do
  before :all do
    DirSpecs.create_mock_dirs
  end

  after :all do
    DirSpecs.delete_mock_dirs
  end

  it_behaves_like :dir_glob, :[]
end

describe "Dir.[]" do
  before :all do
    DirSpecs.create_mock_dirs
  end

  after :all do
    DirSpecs.delete_mock_dirs
  end

  it_behaves_like :dir_glob_recursive, :[]
end

with_feature :encoding do
  describe "Dir.[]" do
    before :all do
      DirSpecs.create_mock_dirs

      @cwd = Dir.pwd
      Dir.chdir DirSpecs.mock_dir
    end

    after :all do
      Dir.chdir @cwd

      DirSpecs.delete_mock_dirs
    end

    it "returns Strings in the encoding of the pattern" do
      a = "file_one*".force_encoding Encoding::IBM437
      b = "file_two*".force_encoding Encoding::EUC_JP
      files = Dir[a, b]

      files.first.encoding.should equal(Encoding::IBM437)
      files.last.encoding.should equal(Encoding::EUC_JP)
    end
  end
end
