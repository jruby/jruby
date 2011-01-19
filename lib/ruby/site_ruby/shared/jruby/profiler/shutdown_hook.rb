require 'jruby'

trap 'INT' do
  runtime = JRuby.runtime
  runtime.thread_service.ruby_thread_map.each do |t, rubythread|
    context = JRuby.reference(rubythread).context
    context.profile_data.print_profile(context, 
                                       runtime.profiled_names,
                                       runtime.profiled_methods,
                                       java.lang.System.err)
  end
  exit
end
STDERR.puts "Profiling enabled; ^C shutdown will now dump profile info"
