require 'jruby'

trap 'INT' do
  runtime = JRuby.runtime
  runtime.thread_service.ruby_thread_map.each do |t, rubythread|
    java.lang.System.err.println "\n#{t} profile results:"
    context = JRuby.reference(rubythread).context
    runtime.printProfileData(context.profile_data, java.lang.System.err)
  end
  exit
end
STDERR.puts "Profiling enabled; ^C shutdown will now dump profile info"
