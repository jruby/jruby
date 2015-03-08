# This script runs 'jt test' on each spec subdirectory of the spec/ruby/core directory.
# Each is run with a 90 second timeout which kills the test process to resolve
# tests that hang.

require 'timeout'

files =  Dir.glob("spec/ruby/core/*/")
files.map! {|x| x[0..-2] }

files.each do |file| 
  command = "ruby tool/jt.rb untag #{file}"
  puts "starting process #{command}"
  pid = Process.spawn(command)
  begin
    Timeout.timeout(90) do
      puts "waiting for the  #{file} process to end"
      Process.wait(pid)
      puts 'process #{file} finished in time'
    end
  rescue Timeout::Error
    puts "process #{file} not finished in time, killing it"
    Process.kill('TERM', pid)
  end
end