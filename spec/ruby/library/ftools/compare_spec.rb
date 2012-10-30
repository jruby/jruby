require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do

  require 'ftools'

  describe "File.compare" do
    before(:each) do
      (1..3).to_a.each do |n|
        if n == 3
          system "echo 'hello mri' > compare_test_#{n}"
        else
          system "echo 'hello rubinius' > compare_test_#{n}"
        end
        system "chmod a+x compare_test_#{n}"
      end
    end

    after(:each) do
      (1..3).to_a.each { |n| File.unlink "compare_test_#{n}" }
    end

    it "compares the file at 1st arg to the file at 2nd arg" do
      File.compare("compare_test_1", "compare_test_2").should == true
      File.compare("compare_test_2", "compare_test_1").should == true

      File.compare("compare_test_1", "compare_test_3").should == false
      File.compare("compare_test_2", "compare_test_3").should == false
    end
  end
end
