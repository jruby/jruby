
require 'spec/profiler/profiler_spec_helpers'

describe JRuby::Profiler, "::GraphProfilePrinter" do
  include JRuby::Profiler::SpecHelpers
  
  def find_row(rows, name)
    rows.detect {|r| r[:name] == name}
  end
  
  context "empty profile data" do
    before do
      @profile_data = JRuby::Profiler.profile {}
    end
    
    it "should print total time 0" do
      graph_output.should match(/Total time: 0.00/)
    end
    
    describe "the row for the top level" do
      before do
        @top_line = line_for(graph_output, "(top)")
      end
      
      it "should have calls 1" do
        @top_line.should_not be_nil
        @top_line[:calls].should == 1
      end
      
      it "should all times zero" do
        @top_line[:total].should == 0
        @top_line[:self].should == 0
        @top_line[:children].should == 0
      end
      
      it "should have all percentages 100%" do
        @top_line[:total_pc].should == "100%"
        @top_line[:self_pc].should == "100%"
      end
    end
    
    it "should not have a row for any JRuby::Profiler methods" do
      lines_for(graph_output, "JRuby::Profiler").should be_empty
    end
  end
  
  context "with profiling" do
    
    before do
      obj = ProfilerTest.new
      @profile_data = JRuby::Profiler.profile do
        obj.wait(0.01)
        obj.test_instance_method
      end
    end
    
    it "should have a main row for each method" do
      main_names = decode_graph(graph_output).map {|r| r[:name]}
      main_names.should include("ProfilerTest#wait")
      main_names.should include("ProfilerTest#test_instance_method")
      main_names.should include("Kernel.sleep")
    end
    
    it "each method should have the correct children" do
      graph = decode_graph(graph_output)
      
      wait_children = find_row(graph, "ProfilerTest#wait")[:children]
      wait_children.length.should == 1
      wait_children.first[:name].should == "Kernel.sleep"
      wait_children.first[:calls].should == [1, 1]
      
      find_row(graph, "ProfilerTest#test_instance_method")[:children].length.should == 0
      find_row(graph, "Kernel.sleep")[:children].length.should == 0
    end
    
    it "each method should have the correct parents" do
      graph = decode_graph(graph_output)
      
      wait_parents = find_row(graph, "ProfilerTest#wait")[:parents]
      wait_parents.length.should == 1
      wait_parents.first[:name].should == "(top)"
      
      parents = find_row(graph, "ProfilerTest#test_instance_method")[:parents]
      parents.length.should == 1
      parents.first[:name].should == "(top)"

      sleep_parents = find_row(graph, "Kernel.sleep")[:parents]
      sleep_parents.length.should == 1
      sleep_parents.first[:name].should == "ProfilerTest#wait"
      sleep_parents.first[:calls].should == [1, 1]
    end
  end
  
  context "with recursive profiling" do
    describe "calls, children and parents" do
      before do
        obj = ProfilerTest.new
        @profile_data = JRuby::Profiler.profile do
          obj.recurse(3)
        end
      end
      
      def recurse_rows
        graph = decode_graph(graph_output)
        graph.select {|r| r[:name].include?("recurse")}
      end
      
      it "should have only one row for the recursive method" do
        recurse_rows.length.should == 1
      end
      
      it "should have the right number of calls" do
        recurse_rows.first[:calls].should == 4
      end
      
      describe "the recursive methods children" do
        it "should be responsible for all the calls of itself bar one" do
          child_row = find_row(recurse_rows.first[:children], "ProfilerTest#recurse")
          child_row[:calls].should == [3, 4]
        end
      end
      
      describe "the recursive methods parent rows" do
        it "should be responsible for all the calls of itself bar one" do
          parent_row = find_row(recurse_rows.first[:parents], "ProfilerTest#recurse")
          parent_row[:calls].should == [3, 4]
        end
        
        it "and the other call should come from the top level" do
          parent_row = find_row(recurse_rows.first[:parents], "(top)")
          parent_row[:calls].should == [1, 4]
        end
      end
    end
    
    describe "durations" do
      before do
        obj = ProfilerTest.new
        @profile_data = JRuby::Profiler.profile do
          obj.recurse_wait(3, 0.05)
        end
      end
      
      it "the time for the main row should not be zero" do
        graph = decode_graph(graph_output)
        find_row(graph, "ProfilerTest#recurse_wait")[:total].should > 0
      end
      
      it "the recursive parent should have zero time" do
        graph = decode_graph(graph_output)
        main = find_row(graph, "ProfilerTest#recurse_wait")
        parent = find_row(main[:parents], "ProfilerTest#recurse_wait")[:total].should == 0
      end
      
      it "the recursive child should have zero time" do
        graph = decode_graph(graph_output)
        main = find_row(graph, "ProfilerTest#recurse_wait")
        parent = find_row(main[:children], "ProfilerTest#recurse_wait")[:total].should == 0
      end
    end
  end
  
  context "with recursive methods where the profiling is started inside the recursion" do
    before do
      obj = ProfilerTest.new
      obj.recurse_and_start_profiling(3)
      @profile_data = JRuby::Profiler.stop
    end
    
    it "should have the correct call info" do
      recursive_method = "ProfilerTest#recurse_and_start_profiling"
      graph = decode_graph(graph_output)
      
      row = find_row(graph, recursive_method)
      row[:calls].should == 4
      
      parent = find_row(row[:parents], recursive_method)
      parent[:calls].should == [3, 4]
      
      child = find_row(row[:children], recursive_method)
      child[:calls].should == [3, 4]
    end
  end
end








