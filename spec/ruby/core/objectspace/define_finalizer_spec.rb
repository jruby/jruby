require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures', __FILE__)

# NOTE: A call to define_finalizer does not guarantee that the
# passed proc or callable will be called at any particular time.
# It is highly questionable whether these aspects of ObjectSpace
# should be spec'd at all.
describe "ObjectSpace.define_finalizer" do
  it "raises an ArgumentError if the action does not respond to call" do
    lambda {
      ObjectSpace.define_finalizer("", 3)
    }.should raise_error(ArgumentError)
  end

  it "accepts an object and a proc" do
    handler = lambda { |obj| obj }
    ObjectSpace.define_finalizer("garbage", handler).should == [0, handler]
  end

  it "accepts an object and a callable" do
    handler = mock("callable")
    def handler.call(obj) end
    ObjectSpace.define_finalizer("garbage", handler).should == [0, handler]
  end

  ruby_version_is ""..."2.1" do
    it "raises ArgumentError trying to define a finalizer on a non-reference" do
      lambda {
        ObjectSpace.define_finalizer(:blah) { 1 }
      }.should raise_error(ArgumentError)
    end
  end

  ruby_version_is "2.1"..."2.2" do
    it "raises RuntimeError trying to define a finalizer on a non-reference" do
      lambda {
        ObjectSpace.define_finalizer(:blah) { 1 }
      }.should raise_error(RuntimeError)
    end
  end

  ruby_version_is "2.2" do
    it "raises ArgumentError trying to define a finalizer on a non-reference" do
      lambda {
        ObjectSpace.define_finalizer(:blah) { 1 }
      }.should raise_error(ArgumentError)
    end
  end

  # see [ruby-core:24095]
  with_feature :fork do
    it "calls finalizer on process termination" do
      rd, wr = IO.pipe
      if Kernel::fork then
        wr.close
        rd.read.should == "finalized"
        rd.close
      else
        rd.close
        handler = ObjectSpaceFixtures.scoped(wr)
        obj = "Test"
        ObjectSpace.define_finalizer(obj, handler)
        exit 0
      end
    end

    it "calls finalizer at exit even if it is self-referencing" do
      rd, wr = IO.pipe
      if Kernel::fork then
        wr.close
        rd.read.should == "finalized"
        rd.close
      else
        rd.close
        obj = "Test"
        handler = Proc.new { wr.write "finalized"; wr.close }
        ObjectSpace.define_finalizer(obj, handler)
        exit 0
      end
    end
  end
end
