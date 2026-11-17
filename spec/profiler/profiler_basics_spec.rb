
require 'spec/profiler/profiler_spec_helpers'

describe "JRuby's profiling mode" do
  include JRuby::Profiler::SpecHelpers
  
  it "reports unimplemented methods like fork as unimplemented" do
    Kernel.respond_to?(:fork).should == false
  end
end

