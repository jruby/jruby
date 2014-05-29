require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do

  require 'ftools'

  describe "File.makedirs" do
    before :each do
      @dir = tmp "file_makedirs"
    end

    after(:each) do
      rm_r @dir
    end

    it "creates the dirs from arg" do
      File.exist?(@dir).should == false
      File.makedirs("#{@dir}/second_dir")
      File.exist?(@dir).should == true
      File.directory?(@dir).should == true
      File.exist?("#{@dir}/second_dir").should == true
      File.directory?("#{@dir}/second_dir").should == true
    end
  end
end
