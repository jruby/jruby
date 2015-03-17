require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/common', __FILE__)
require File.expand_path('../shared/closed', __FILE__)

describe "Dir#rewind" do
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

  it "resets the next read to start from the first entry" do
    first   = @dir.pos
    a       = @dir.read
    b       = @dir.read
    prejmp  = @dir.pos
    ret     = @dir.rewind
    second  = @dir.pos
    c       = @dir.read

    a.should_not == b
    b.should_not == c
    c.should     == a

    second.should_not == prejmp
  end

  it "returns the Dir instance" do
    @dir.rewind.should == @dir
  end

  it_behaves_like :dir_closed, :rewind
end
