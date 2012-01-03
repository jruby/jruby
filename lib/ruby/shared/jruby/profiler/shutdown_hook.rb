require 'jruby'

trap 'INT' do
  runtime = JRuby.runtime
  runtime.thread_service.ruby_thread_map.each do |t, rubythread|
    java.lang.System.err.println "\n#{t} profile results:"
    context = JRuby.reference(rubythread).context
    profile_data = context.profile_data
    printer = JRuby.runtime.instance_config.make_default_profile_printer(profile_data)
    printer.printProfile(java.lang.System.err)
  end
  exit
end
STDERR.puts "Profiling enabled; ^C shutdown will now dump profile info"
