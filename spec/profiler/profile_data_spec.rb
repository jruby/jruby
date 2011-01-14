

require 'spec/profiler/profiler_spec_helpers'

describe JRuby::Profiler, "::ProfileData" do
  include JRuby::Profiler::SpecHelpers
  
  context "before any profiling" do
    it "can clear the current thread's data" do
      JRuby::Profiler.clear
    end
    
    it "has a top invocation" do
      top.should_not be_nil
    end
    
    it "has a current invocation" do
      current.should_not be_nil
    end
    
    describe "the top invocation" do
      it "has method serial 0" do
        top.method_serial_number.should == 0
      end
      
      it "is also the current invocation" do
        top.should == current
      end
      
      it "has no children" do
        top.children.size.should == 0
      end
    end
  end
  
  context "after profiling an empty block" do
    before do
      clear
      JRuby::Profiler.profile {}
    end
    
    it "should have one invocation from the stop profiling method" do
      top.children.size.should == 1
      method_name(top.children.values.to_a.first).should == "JRuby::Profiler.stop"
    end
  end
  
  context "after profiling method calls at the top level" do
    def profile
      obj = ProfilerTest.new
      JRuby::Profiler.profile do
        obj.test_instance_method
        ProfilerTest.test_static_method
        ProfilerTest.test_static_method
        ProfilerTest.test_metaclass_method
        ProfilerTest.test_metaclass_method
        ProfilerTest.test_metaclass_method
      end
    end
    
    before do
      clear
      profile
    end
    
    it "should have invocations for each method called plus stop" do
      top.children.size.should == 4
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
    def profile
      obj = ProfilerTest.new
      JRuby::Profiler.profile do
        obj.level1
      end
    end
    
    before do
      clear
      profile
    end
    
    it "should should the correct number of invocations at the top level" do
      top.children.size.should == 2
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
  end
  
  context "after profiling recursive methods" do
    def profile
      obj = ProfilerTest.new
      JRuby::Profiler.profile do
        obj.recurse(3)
      end
    end
    
    before do
      clear
      profile
    end
    
    it "should have the correct invocations with recursiveDepths" do
      current = top
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
      clear
      obj = ProfilerTest.new
      obj.start
      1 + 2
      obj.stop
      puts graph_output
    end
    
    it "methods that are lower in the tree that the highest level should not be attached to top" do
      get_inv("ProfilerTest#test_instance_method", top).should be_nil
    end
    
  end
end



