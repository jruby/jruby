require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/common', __FILE__)
require File.expand_path('../shared/closed', __FILE__)

describe "Dir#each" do
  before :all do
    DirSpecs.create_mock_dirs
  end

  after :all do
    DirSpecs.delete_mock_dirs
  end

  before :each do
    @dir = Dir.open DirSpecs.mock_dir
  end

  after :each do
    @dir.close
  end

  it "yields each directory entry in succession" do
    a = []
    @dir.each {|dir| a << dir}

    a.sort.should == DirSpecs.expected_paths
  end

  it "returns the directory which remains open" do
    # an FS does not necessarily impose order
    ls = Dir.entries(DirSpecs.mock_dir)
    @dir.each {}.should == @dir
    @dir.read.should == nil
    @dir.rewind
    ls.should include(@dir.read)
  end

  ruby_version_is '' ... '1.8.7' do
    it "raises a LocalJumpError if no block given" do
      lambda{ @dir.each }.should raise_error(LocalJumpError)
    end
  end

  ruby_version_is '1.8.7' do
    it "returns an Enumerator if no block given" do
      @dir.each.should be_an_instance_of(enumerator_class)
      @dir.each.to_a.sort.should == DirSpecs.expected_paths
    end
  end

end

describe "Dir#each" do
  before :all do
    DirSpecs.create_mock_dirs
  end

  after :all do
    DirSpecs.delete_mock_dirs
  end

  it_behaves_like :dir_closed, :each
end
