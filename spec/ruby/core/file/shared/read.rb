require File.expand_path('../../../dir/fixtures/common', __FILE__)

describe :file_read_directory, :shared => true do
  platform_is :darwin, :linux do
    it "raises a Errno::EISDIR when passed a path that is a directory" do
      lambda { @object.send(@method, ".") }.should raise_error(Errno::EISDIR)
    end
  end

  platform_is :bsd do
    it "does not raises any exception when passed a path that is a directory" do
      lambda { @object.send(@method, ".") }.should_not raise_error
    end
  end

  platform_is :windows do
    it "raises a Errno::EACCES when passed a path that is a directory" do
      lambda { @object.send(@method, ".") }.should raise_error(Errno::EACCES)
    end
  end
end
