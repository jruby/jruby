#
# Rakefile for JRuby.
#
# See also rakelib/*.rake for more tasks and details.

require File.dirname(__FILE__) + '/rakelib/helpers.rb'

# Suppress .java lines from non-traced backtrace
begin
  Rake.application.options.suppress_backtrace_pattern =
    /(#{Rake::Backtrace::SUPPRESS_PATTERN})|(^org\/jruby)/
rescue
end
