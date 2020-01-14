
require 'spec/profiler/profiler_spec_helpers'

describe JRuby::Profiler, "::ProfileData" do
  include JRuby::Profiler::SpecHelpers
  
  context "after profiling an empty block" do
    before do
      @profile_data = JRuby::Profiler.profile {}
    end
    
    it "should have no invocations" do
      top_invocation.children.size.should == 0
    end
    
    it "the top invocation should be named (top)" do
      method_name(top_invocation).should == "(top)"
    end
  end
  
  context "after profiling method calls at the top level" do
    before do
      obj = ProfilerTest.new
      @profile_data = JRuby::Profiler.profile do
        obj.test_instance_method
        ProfilerTest.test_static_method
        ProfilerTest.test_static_method
        ProfilerTest.test_metaclass_method
        ProfilerTest.test_metaclass_method
        ProfilerTest.test_metaclass_method
      end
    end
    
    it "should have invocations for each method called" do
      top_invocation.children.size.should == 3
    end
    
    it "the instance method should have a correct invocation" do
      inv = get_inv("ProfilerTest#test_instance_method")
      inv.count.should == 1
      inv.recursive_depth.should == 1
    end
    
    it "the static method should have a correct invocation" do
      inv = get_inv("ProfilerTest.test_static_method")
      inv.count.should == 2
      inv.recursive_depth.should == 1
    end
    
    it "the metaclass method should have a correct invocation" do
      inv = get_inv("ProfilerTest.test_metaclass_method")
      inv.count.should == 3
      inv.recursive_depth.should == 1
    end
  end
  
  context "after profiling methods on several levels" do
    before do
      obj = ProfilerTest.new
      @profile_data = JRuby::Profiler.profile do
        obj.level1
      end
    end
    
    it "should should the correct number of invocations at the top level" do
      top_invocation.children.size.should == 1
    end
    
    it "should have each level correct" do
      level1 = get_inv("ProfilerTest#level1")
      level1.count.should == 1
      level1.recursive_depth.should == 1
      level2 = get_inv("ProfilerTest#level2", level1)
      level2.count.should == 1
      level2.recursive_depth.should == 1
      level3 = get_inv("ProfilerTest#level3", level2)
      level3.count.should == 2
      level3.recursive_depth.should == 1
    end
    
    it "should use new data when profiling again" do
      profile_data = JRuby::Profiler.profile_data
      profile_data.compute_results.children.size.should == 1
      
      new_profile_data = JRuby::Profiler.profile { }
      new_profile_data.should_not == profile_data
      new_profile_data.compute_results.children.size.should == 0
      profile_data.compute_results.children.size.should == 1
    end
    
  end

  context "after profiling recursive methods" do
    def profile
      obj = ProfilerTest.new
      @profile_data = JRuby::Profiler.profile do
        obj.recurse(3)
      end
    end

    before do
      profile
    end

    it "should have the correct invocations with recursiveDepths" do
      current = top_invocation
      4.times do |i|
        current = get_inv("ProfilerTest#recurse", current)
        current.count.should == 1
        current.recursive_depth.should == i + 1
      end
      get_inv("ProfilerTest#recurse", current).should be_nil
    end
  end
  
  context "after profiling that starts and stops in different methods" do
    before do
      obj = ProfilerTest.new
      obj.start
      1 + 2
      @profile_data = obj.stop
    end
    
    it "methods that are lower in the tree that the highest level should not be attached to top" do
      get_inv("ProfilerTest#test_instance_method").should be_nil
    end
    
  end

  context "when profiling refined methods" do
    def profile
      refinement = Module.new do
        refine(String) do
          def to_s
            super
          end
        end
      end

      @profile_data = JRuby::Profiler.profile do
        @call_to_s_result = "string".to_s
      end
    end

    before do
      profile
    end

    it "should complete successfully" do
      @call_to_s_result.should == "string"
    end
  end
end



