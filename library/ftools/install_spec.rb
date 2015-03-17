require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do

  require 'ftools'

  # the tests below are windows-hostile
  platform_is_not :windows do
    describe "File.install" do
      before(:each) do
        system "echo 'hello rubinius' > install_test_1"
        system "chmod 0777 install_test_1"
      end

      after(:each) do
        (1..2).each { |n| File.unlink "install_test_#{n}" rescue nil }
      end

      it "changes the mode to 1st arg for files in 2nd arg" do
        `ls -l install_test_1`.should =~ /^-rwxrwxrwx/
        File.install "install_test_1", "install_test_2", 0644
        `ls -l install_test_2`.should =~ /^-rw-r--r--/
      end
    end
  end
end
