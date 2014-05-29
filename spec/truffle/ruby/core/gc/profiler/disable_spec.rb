require File.expand_path('../../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "GC::Profiler.disable" do

    before do
      @status = GC::Profiler.enabled?
    end

    after do
      @status ? GC::Profiler.enable : GC::Profiler.disable
    end

    it "returns true iff the profiler previously disabled" do
      GC.enable
      GC.disable.should == false
      GC.disable.should == true
      GC.disable.should == true
      GC.enable
      GC.disable.should == false
      GC.disable.should == true
    end
  end
end
