require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do

  # the tests below are windows-hostile
  platform_is_not :windows do
    require 'ftools'

    describe "File.chmod" do
      before(:each) do
        (1..2).each do |n|
          system "echo 'hello rubinius' > chmod_test_#{n}"
          system "chmod 0777 chmod_test_#{n}"
        end
      end

      after(:each) do
        (1..2).each { |n| File.unlink "chmod_test_#{n}" rescue nil }
      end

      it "changes the mode to 1st arg for files in 2nd arg" do
        `ls -l chmod_test_1`.should =~ /^-rwxrwxrwx/
        `ls -l chmod_test_2`.should =~ /^-rwxrwxrwx/
        File.chmod 0644, "chmod_test_1", "chmod_test_2"
        `ls -l chmod_test_1`.should =~ /^-rw-r--r--/
        `ls -l chmod_test_2`.should =~ /^-rw-r--r--/
      end
    end
  end
end
