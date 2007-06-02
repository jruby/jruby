require 'profiler'

at_exit {
  Profiler__::print_profile(STDERR)
}
Profiler__::start_profile
