require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do

  require 'ftools'

  describe "File.move" do
    before(:each) do
      File.open('move_test', 'w+') do |f|
        f.puts('hello rubinius')
      end
      platform_is_not :windows do
        system "chmod a+x move_test"
      end
    end

    after(:each) do
      File.unlink "move_test_dest"
      File.unlink "move_test" rescue nil
    end

    it "moves the file at 1st arg to the file at 2nd arg" do
      omode = File.stat("move_test").mode
      File.move("move_test", "move_test_dest")
      fd = File.open("move_test_dest")
      data = fd.read
      data.should == "hello rubinius\n"
      fd.close
      mode = File.stat("move_test_dest").mode

      omode.should == mode
    end
  end
end
