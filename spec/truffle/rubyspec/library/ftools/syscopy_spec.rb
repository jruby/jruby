require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do

  require 'ftools'

  describe "File.syscopy" do
    before(:each) do
      File.open('syscopy_test', 'w+') do |f|
        f.puts('hello rubinius')
      end
      platform_is_not :windows do
        system "chmod a+x syscopy_test"
      end
    end

    after(:each) do
      File.unlink "syscopy_test"
      File.unlink "syscopy_test_dest" rescue nil
    end

    it "copies the file at 1st arg to the file at 2nd arg" do
      File.syscopy("syscopy_test", "syscopy_test_dest")
      fd = File.open("syscopy_test_dest")
      data = fd.read
      data.should == "hello rubinius\n"
      fd.close

      omode = File.stat("syscopy_test").mode
      mode = File.stat("syscopy_test_dest").mode

      omode.should == mode
    end
  end
end
