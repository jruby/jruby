require File.expand_path('../../../../spec_helper', __FILE__)

describe "GC::Profiler.enable" do

  before do
    @status = GC::Profiler.enabled?
  end

  after do
    @status ? GC::Profiler.enable : GC::Profiler.disable
  end

  it "returns true iff the profiler previously disabled" do
    GC.disable
    GC.enable.should == true
    GC.enable.should == false
    GC.enable.should == false
    GC.disable
    GC.enable.should == true
    GC.enable.should == false
  end
end
