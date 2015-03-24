require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/common', __FILE__)
require File.expand_path('../shared/pwd', __FILE__)

describe "Dir.pwd" do
  before :all do
    DirSpecs.create_mock_dirs
  end

  after :all do
    DirSpecs.delete_mock_dirs
  end

  it_behaves_like :dir_pwd, :pwd

  it "correctly displays dirs with unicode characters in them" do
    DirSpecs.clear_dirs

    begin
      str = [0xe9].pack 'U' # Unicode é
      Dir.mkdir str
      File.exist?(str).should == true

      old_pwd = Dir.pwd
      Dir.chdir(str) { Dir.pwd.force_encoding('UTF-8').should == File.join(old_pwd, str) }
    ensure
      DirSpecs.clear_dirs
    end
  end
end
