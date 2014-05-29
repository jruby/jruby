require File.expand_path('../../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "GC::Profiler.result" do
    it "returns a string" do
      GC::Profiler.result.should be_kind_of(String)
    end
  end
end
