
require 'java'

module JRuby
  module Profiler
    import org.jruby.runtime.profile.AbstractProfilePrinter
    import org.jruby.runtime.profile.GraphProfilePrinter
    import org.jruby.runtime.profile.FlatProfilePrinter
    import org.jruby.Ruby
    
    def self.start
      current_thread_context.start_profiling
    end
    
    def self.stop
      current_thread_context.stop_profiling
    end
    
    def self.results
      current_thread_context.profile_data
    end
    
    def self.profile
      start
      yield
      stop
    end
    
    def self.clear
      current_thread_context.profile_data.clear
    end
    
    private
    
    def self.runtime
      JRuby::Profiler::Ruby.getGlobalRuntime
    end
    
    def self.current_thread_context
      runtime.get_thread_service.get_current_context
    end
  end
end