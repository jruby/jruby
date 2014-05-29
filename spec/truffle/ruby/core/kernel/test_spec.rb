require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Kernel#test" do
  before :all do
    @file = File.dirname(__FILE__) + '/fixtures/classes.rb'
    @dir = File.dirname(__FILE__) + '/fixtures'

    @link = tmp("file_symlink.lnk")
    rm_r @link

    File.symlink(@file, @link)
  end

  after :all do
    rm_r @link
  end

  it "is a private method" do
    Kernel.should have_private_instance_method(:test)
  end

  it "returns true when passed ?f if the argument is a regular file" do
    Kernel.test(?f, @file).should == true
  end

  it "returns true when passed ?e if the argument is a file" do
    Kernel.test(?e, @file).should == true
  end

  it "returns true when passed ?d if the argument is a directory" do
    Kernel.test(?d, @dir).should == true
  end

  it "returns true when passed ?l if the argument is a symlink" do
    Kernel.test(?l, @link).should be_true
  end

  ruby_version_is "1.9" do
    it "calls #to_path on second argument when passed ?f and a filename" do
      p = mock('path')
      p.should_receive(:to_path).and_return @file
      Kernel.test(?f, p)
    end

    it "calls #to_path on second argument when passed ?e and a filename" do
      p = mock('path')
      p.should_receive(:to_path).and_return @file
      Kernel.test(?e, p)
    end

    it "calls #to_path on second argument when passed ?d and a directory" do
      p = mock('path')
      p.should_receive(:to_path).and_return @dir
      Kernel.test(?d, p)
    end
  end
end

describe "Kernel.test" do
  it "needs to be reviewed for spec completeness"
end
