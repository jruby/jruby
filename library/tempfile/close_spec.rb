require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/common', __FILE__)
require 'tempfile'

describe "Tempfile#close when passed no argument or [false]" do
  before(:each) do
    @tempfile = Tempfile.new("specs", tmp(""))
  end

  after(:each) do
    TempfileSpecs.cleanup(@tempfile)
  end

  it "closes self" do
    @tempfile.close
    @tempfile.closed?.should be_true
  end
end

describe "Tempfile#close when passed [true]" do
  before(:each) do
    @tempfile = Tempfile.new("specs", tmp(""))
  end

  after(:each) do
    TempfileSpecs.cleanup(@tempfile)
  end

  it "closes self" do
    @tempfile.close(true)
    @tempfile.closed?.should be_true
  end

  it "unlinks self" do
    path = @tempfile.path
    @tempfile.close(true)
    File.exist?(path).should be_false
  end
end

describe "Tempfile#close!" do
  before(:each) do
    @tempfile = Tempfile.new("specs", tmp(""))
  end

  after(:each) do
    @tempfile.unlink if @tempfile.path
  end

  it "closes self" do
    @tempfile.close!
    @tempfile.closed?.should be_true
  end

  it "unlinks self" do
    path =  @tempfile.path
    @tempfile.close!
    File.exist?(path).should be_false
  end
end
