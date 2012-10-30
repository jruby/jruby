require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/common', __FILE__)
require File.expand_path('../shared/closed', __FILE__)

describe "Dir#close" do
  before :all do
    DirSpecs.create_mock_dirs
  end

  after :all do
    DirSpecs.delete_mock_dirs
  end

  platform_is_not :windows do
    it "closes the stream and fd and returns nil" do
      # This is a bit convoluted but we are trying to ensure the file gets closed.
      # To do that, we peek to see what the next FD number is and then probe that
      # to see whether it has been closed.
      peek = IO.sysopen DirSpecs.mock_dir
      File.for_fd(peek).close

      dir = Dir.open DirSpecs.mock_dir
      File.for_fd(peek).close                   # Should be open here

      dir.close.should == nil
      lambda { File.for_fd(peek).close }.should raise_error(SystemCallError)  # And closed here
    end
  end
end

describe "Dir#close" do
  before :all do
    DirSpecs.create_mock_dirs
  end

  after :all do
    DirSpecs.delete_mock_dirs
  end

  it_behaves_like :dir_closed, :close
end
