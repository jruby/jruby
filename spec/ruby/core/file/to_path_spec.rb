require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "File#to_path" do
    before :each do
      @name = "file_to_path"
      @path = encode tmp(@name), "euc-jp"
      touch @path
    end

    after :each do
      @file.close if @file and !@file.closed?
      rm_r @path
    end

    it "returns a String" do
      @file = File.new @path
      @file.to_path.should be_an_instance_of(String)
    end

    it "does not normalise the path it returns" do
      Dir.chdir(tmp("")) do
        unorm = "./#{@name}"
        @file = File.new unorm
        @file.to_path.should == unorm
      end
    end

    it "does not canonicalize the path it returns" do
      dir = File.basename tmp("")
      path = "#{tmp("")}../#{dir}/#{@name}"
      @file = File.new path
      @file.to_path.should == path
    end

    it "does not absolute-ise the path it returns" do
      Dir.chdir(tmp("")) do
        @file = File.new @name
        @file.to_path.should == @name
      end
    end

    it "preserves the encoding of the path" do
      @file = File.new @path
      @file.to_path.encoding.should == Encoding.find("euc-jp")
    end
  end
end
