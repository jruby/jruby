require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do

  require 'ftools'

  describe "File.safe_unlink" do
    before(:each) do
      (1..2).each do |n|
        system "echo 'hello rubinius' > safe_unlink_test_#{n}"
        system "chmod 0777 safe_unlink_test_#{n}"
      end
    end

    after(:each) do
      (1..2).each { |n| File.unlink "safe_unlink_test_#{n}" rescue nil }
    end

    it "deletes the files in arg and returns an array of files deleted" do
      File.exist?("safe_unlink_test_1").should == true
      File.exist?("safe_unlink_test_2").should == true
      File.safe_unlink("safe_unlink_test_1", "safe_unlink_test_2").should == ["safe_unlink_test_1", "safe_unlink_test_2"]
      File.exist?("safe_unlink_test_1").should == false
      File.exist?("safe_unlink_test_2").should == false
    end
  end
end
