#
# Rakefile for JRuby.
#
# At this time, most JRuby build tasks still use build.xml and Apache
# Ant. This Rakefile has some additional tasks. We hope to migrate
# more out of build.xml and into Rake in the future.
#
# See also rakelib/*.rake for more tasks and details.

require File.dirname(__FILE__) + '/rakelib/helpers.rb'

# Suppress .java lines from non-traced backtrace
begin
  Rake.application.options.suppress_backtrace_pattern =
    /(#{Rake::Backtrace::SUPPRESS_PATTERN})|(^org\/jruby)/
rescue
end
