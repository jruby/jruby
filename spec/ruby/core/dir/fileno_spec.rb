require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/common', __FILE__)

ruby_version_is "2.2" do
  describe "Dir#fileno" do
    before :each do
      @name = tmp("fileno")
      mkdir_p @name
      @dir = Dir.new(@name)
    end

    after :each do
      @dir.close
      rm_r @name
    end

    platform_is_not :windows do
      it "returns the file descriptor of the dir" do
        @dir.fileno.should.be_kind_of(Fixnum)
      end
    end

    platform_is :windows do
      it "raises an error on Windows" do
        lambda { @dir.fileno }.to raise_error(NotImplementedError)
      end
    end
  end
end
