
require 'java'

module JRuby
  module Profiler
    java_import org.jruby.runtime.profile.AbstractProfilePrinter
    java_import org.jruby.runtime.profile.GraphProfilePrinter
    java_import org.jruby.runtime.profile.FlatProfilePrinter
    java_import org.jruby.Ruby
    
    def self.profile(&block)
      start
      profiled_code(&block)
      stop
    end
    
    def self.profiled_code
      yield
    end
    
    def self.clear
      current_thread_context.profile_data.clear
    end
    
    private
    
    def self.start
      current_thread_context.start_profiling
    end
    
    def self.stop
      current_thread_context.stop_profiling
      result = current_thread_context.profile_data.results
      clear
      result
    end
    
    def self.runtime
      JRuby::Profiler::Ruby.getGlobalRuntime
    end
    
    def self.current_thread_context
      runtime.get_thread_service.get_current_context
    end
  end
end