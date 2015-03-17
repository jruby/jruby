require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do

  require 'ftools'

  describe "File.copy" do
    before(:each) do
      File.open('copy_test', 'w+') do |f|
        f.puts('hello rubinius')
      end
      platform_is_not :windows do
        system "chmod a+x copy_test"
      end
    end

    after(:each) do
      File.unlink "copy_test"
      File.unlink "copy_test_dest" rescue nil
    end

    it "copies the file at 1st arg to the file at 2nd arg" do
      File.copy("copy_test", "copy_test_dest")
      fd = File.open("copy_test_dest")
      data = fd.read
      data.should == "hello rubinius\n"
      fd.close

      omode = File.stat("copy_test").mode
      mode = File.stat("copy_test_dest").mode

      omode.should == mode
    end
  end
end
