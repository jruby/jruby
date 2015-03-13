require 'spec/profiler/profiler_spec_helpers'

describe JRuby::Profiler, "::JsonProfilePrinter" do
  include JRuby::Profiler::SpecHelpers

  context 'when printing an empty profile' do
    before do
      @profile_data = JRuby::Profiler.profile {}
    end
    
    it 'contains only the top invocation' do
      json_output['methods'].size.should == 1
      json_output['methods'].first['name'].should == '(top)'
    end

    it 'contains the total duration' do
      json_output['total_time'].should == 0.0
    end

    it 'outputs the name of the thread' do
      json_output['thread_name'].should == 'main'
    end
  end

  context 'when printing a profile' do
    before do
      obj = ProfilerTest.new
      @profile_data = JRuby::Profiler.profile do
        obj.wait(0.01)
        obj.test_instance_method
      end
    end

    it 'outputs the total duration' do
      json_output['total_time'].should > 0.0
    end

    it 'outputs the name of the thread' do
      json_output['thread_name'].should == 'main'
    end

    context 'outputs method data which' do
      let :method_invocation do
        json_output['methods'].find { |m| m['name'] == 'ProfilerTest#test_instance_method' }
      end

      let :top_invocation do
        json_output['methods'].find { |m| m['name'] == '(top)' }
      end

      it 'contains the number of calls, total, self and child time' do
        method_invocation.should include('total_calls' => 1, 'total_time' => anything, 'self_time' => anything, 'child_time' => anything)
      end

      it 'contains data on the calls from parents, including calls, total, self and child time' do
        method_invocation['parents'].size.should == 1
        call_data = method_invocation['parents'].find { |c| c['id'] == top_invocation['id'] }
        call_data.should include('total_calls' => 1, 'total_time' => anything, 'self_time' => anything, 'child_time' => anything)
      end

      it 'contains data on the calls to children' do
        method1_invocation = json_output['methods'].find { |m| m['name'] == 'ProfilerTest#wait' }
        method2_invocation = json_output['methods'].find { |m| m['name'] == 'ProfilerTest#test_instance_method' }
        call1_data = top_invocation['children'].find { |c| c['id'] == method1_invocation['id'] }
        call1_data.should include('total_calls' => 1, 'total_time' => anything, 'self_time' => anything, 'child_time' => anything)
        call2_data = top_invocation['children'].find { |c| c['id'] == method2_invocation['id'] }
        call2_data.should include('total_calls' => 1, 'total_time' => anything, 'self_time' => anything, 'child_time' => anything)
      end
    end
  end
end








