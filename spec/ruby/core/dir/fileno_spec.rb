require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/common', __FILE__)

describe "Dir#fileno" do

  ruby_version_is "2.2" do
    platform_is_not :windows do
      before :each do
        @name = tmp("fileno")
        mkdir_p @name
      end

      after :each do
        rm_r @name
      end

      it "returns the file descriptor of the dir" do
        dir = Dir.new(@name)
        dir.fileno.should.be_kind_of(Fixnum)
      end
    end

    platform_is :windows do
      it "raises an error" do
        dir = Dir.new('.')
        lambda { dir.fileno }.to raise_error(NotImplementedError)
      end
    end
  end
end

