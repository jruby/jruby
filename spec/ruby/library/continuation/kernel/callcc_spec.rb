require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/callcc', __FILE__)

with_feature :continuation_library do
  require 'continuation'

  describe :kernel_callcc, shared: true do
    it "is possible to exit a loop like a break" do
      i = 0
      @object.callcc do |x|
        loop do
          i += 1
          x.call() if i == 5
        end
      end.should == nil
      i.should == 5
    end

    it "is possible to call a continuation multiple times" do
      i = 0
      @cont = nil
      @object.callcc {|c| @cont = c}
      i += 1
      @cont.call() if i < 5
      i.should == 5
    end

    it "returns the results of a block if continuation is not called" do
      a = @object.callcc {|c| 0}
      a.should == 0
    end

    it "returns the results of continuation once called" do
      @cont = nil
      a = @object.callcc {|c| @cont = c; 0}
      @cont.call(1) if a == 0
      a.should == 1
    end

    it "returns the arguments to call" do
      @object.callcc {|cont| cont.call }.should == nil
      @object.callcc {|cont| cont.call 1 }.should == 1
      @object.callcc {|cont| cont.call 1,2,3 }.should == [1,2,3]
    end

    it "preserves changes to block-local scope" do
      i = "before"
      cont = @object.callcc { |c| c }
      if cont # nil the second time
        i = "after"
        cont.call
      end
      i.should == "after"
    end

    it "preserves changes to method-local scope" do
      # This spec tests that a continuation shares the same locals
      # tuple as the scope that created it.
      KernelSpecs.before_and_after.should == "after"
    end

    it "raises a LocalJumpError if callcc is not given a block" do
      lambda { @object.callcc }.should raise_error(LocalJumpError)
    end
  end

  describe "Kernel#callcc" do
    it "is a private method" do
      Kernel.should have_private_instance_method(:callcc)
    end

    it_behaves_like :kernel_callcc, :callcc, KernelSpecs::Method.new
  end

  describe "Kernel.callcc" do
    it_behaves_like :kernel_callcc, :callcc, Kernel
  end
end
