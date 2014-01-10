require File.expand_path('../../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "GC::Profiler.total_time" do
    it "returns an float" do
      GC::Profiler.total_time.should be_kind_of(Float)
    end
  end
end
