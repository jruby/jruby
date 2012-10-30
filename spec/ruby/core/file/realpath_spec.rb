require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "File.realpath" do
    before :each do
      @real_dir = tmp('dir_realpath_real')
      @link_dir = tmp('dir_realpath_link')

      mkdir_p @real_dir
      File.symlink(@real_dir, @link_dir)

      @file = File.join(@real_dir, 'file')
      @link = File.join(@link_dir, 'link')

      touch @file
      File.symlink(@file, @link)
    end

    after :each do
      File.unlink @link, @link_dir
      rm_r @file, @real_dir
    end

    it "returns '/' when passed '/'" do
      File.realpath('/').should == '/'
    end

    it "returns the real (absolute) pathname not containing symlinks" do
      File.realpath(@link).should == @file
    end

    it "uses base directory for interpreting relative pathname" do
      File.realpath(File.basename(@link), @link_dir).should == @file
    end

    it "uses current directory for interpreting relative pathname" do
      Dir.chdir @link_dir do
        File.realpath(File.basename(@link)).should == @file
      end
    end

    it "raises a Errno::ELOOP if symlink points itself" do
      File.unlink @link
      File.symlink(@link, @link)
      lambda { File.realpath(@link) }.should raise_error(Errno::ELOOP)
    end
  end
end
