require File.expand_path('../../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "GC::Profiler.enabled?" do

    before do
      @status = GC::Profiler.enabled?
    end

    after do
      @status ? GC::Profiler.enable : GC::Profiler.disable
    end

    ruby_bug "#6821", "1.9" do
      it "reports as enabled when enabled" do
        GC::Profiler.enable
        GC::Profiler.enabled?.should be_true
      end
    end

    it "reports as disabled when disabled" do
      GC::Profiler.disable
      GC::Profiler.enabled?.should be_false
    end
  end
end
