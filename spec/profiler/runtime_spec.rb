
describe Java::OrgJruby::Ruby do

  before do
    @config = Java::OrgJruby::Ruby.get_global_runtime.instance_config
  end
  
  context "runtime with rspec" do
    
    before do
      @runtime = new_runtime(Java::OrgJruby::RubyInstanceConfig.new(@config))
      @runtime.evalScriptlet("require 'rubygems'")
      # setup rspec but disable autorun - we'll run ourselves :
      @runtime.evalScriptlet("require 'rspec/core'")
      @runtime.evalScriptlet("RSpec::Core::Runner.disable_autorun!")
      # helper OUT/ERR streams to use when running specs :
      @runtime.evalScriptlet("require 'stringio'")
      @runtime.evalScriptlet("ERR_IO = StringIO.new")
      @runtime.evalScriptlet("OUT_IO = StringIO.new")
    end
    
    after do
      @runtime.tear_down
    end

    it "should pass profile_data_spec" do
      check_passed_spec @runtime.evalScriptlet("RSpec::Core::Runner.run([ 'spec/profiler/profile_data_spec.rb' ], ERR_IO, OUT_IO)")
    end

    it "should pass profiler_basics_spec" do
      check_passed_spec @runtime.evalScriptlet("RSpec::Core::Runner.run([ 'spec/profiler/profiler_basics_spec.rb' ], ERR_IO, OUT_IO)")
    end

    it "should pass graph_profile_printer_spec" do
      check_passed_spec @runtime.evalScriptlet("RSpec::Core::Runner.run([ 'spec/profiler/graph_profile_printer_spec.rb' ], ERR_IO, OUT_IO)")
    end
    
    def check_passed_spec(outcome)
      # print any errors if occured :
      @runtime.evalScriptlet("puts ERR_IO.string unless ERR_IO.string.empty?")
      expect(outcome.to_s).to eq(0.to_s)
    ensure
      unless outcome.to_s == 0.to_s
        puts "spec not passed, output: \n"
        @runtime.evalScriptlet("puts OUT_IO.string")
      end
    end
    
  end
  
  def new_runtime(config = Java::OrgJruby::RubyInstanceConfig.new)
    config.processArguments(['--profile.api'])
    Java::OrgJruby::Ruby.newInstance(config)
  end
  
end
