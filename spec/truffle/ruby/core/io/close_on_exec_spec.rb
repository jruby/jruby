require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "IO#close_on_exec=" do
    before :each do
      @name = tmp('io_close_on_exec.txt')
      @io = new_io @name
    end

    after :each do
      @io.close unless @io.closed?
      rm_r @name
    end

    platform_is :windows do
      it "returns false from #respond_to?" do
        @io.respond_to?(:close_on_exec=).should be_false
      end

      it "raises a NotImplementedError when called" do
        lambda { @io.close_on_exec = true }.should raise_error(NotImplementedError)
      end
    end

    platform_is_not :windows do
      it "sets the close-on-exec flag if true" do
        @io.close_on_exec = true
        @io.close_on_exec?.should == true
      end

      it "sets the close-on-exec flag if non-false" do
        @io.close_on_exec = :true
        @io.close_on_exec?.should == true
      end

      it "unsets the close-on-exec flag if false" do
        @io.close_on_exec = true
        @io.close_on_exec = false
        @io.close_on_exec?.should == false
      end

      it "unsets the close-on-exec flag if nil" do
        @io.close_on_exec = true
        @io.close_on_exec = nil
        @io.close_on_exec?.should == false
      end

      it "ensures the IO's file descriptor is closed in exec'ed processes" do
        require 'fcntl'
        @io.close_on_exec = true
        (@io.fcntl(Fcntl::F_GETFD) & Fcntl::FD_CLOEXEC).should == Fcntl::FD_CLOEXEC
      end

      it "raises IOError if called on a closed IO" do
        @io.close
        lambda { @io.close_on_exec = true }.should raise_error(IOError)
      end

      it "returns nil" do
        @io.send(:close_on_exec=, true).should be_nil
      end
    end
  end
end

ruby_version_is "1.9" do
  describe "IO#close_on_exec?" do
    before :each do
      @name = tmp('io_close_on_exec.txt')
      @io = new_io @name
    end

    after :each do
      @io.close unless @io.closed?
      rm_r @name
    end

    platform_is :windows do
      it "returns false from #respond_to?" do
        @io.respond_to?(:close_on_exec?).should be_false
      end

      it "raises a NotImplementedError when called" do
        lambda { @io.close_on_exec? }.should raise_error(NotImplementedError)
      end
    end

    platform_is_not :windows do
      ruby_version_is "1.9"..."2.0" do
        it "returns false by default" do
          @io.close_on_exec?.should == false
        end
      end

      ruby_version_is "2.0" do
        it "returns true by default" do
          @io.close_on_exec?.should == true
        end
      end

      it "returns true if set" do
        @io.close_on_exec = true
        @io.close_on_exec?.should == true
      end

      it "raises IOError if called on a closed IO" do
        @io.close
        lambda { @io.close_on_exec? }.should raise_error(IOError)
      end
    end
  end
end
