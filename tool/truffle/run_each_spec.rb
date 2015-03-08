# This script will run 'jt untag' on each spec. If examples in that spec are untagged,
# it will then run 'jt test' to verify the untagging worked correctly. If 'jt test' fails,
# it will run 'git checkout' on the tag file that was changed.
# This still produces 'jt test' suite errors after running because of presumed test pollution
# between the specs when running as a suite.
# The 'test' and 'untag' commands are run with a 90 second timeout will kills the process if it 
# finished yet to resolve tests that hang.
# This currently takes greater than 4 hours to complete one run of this script.

require 'timeout'

files =  Dir.glob("spec/ruby/core/*/*")
files.reject! { |f| File.directory?(f) }

num = 1
total = files.size
files.each_with_index do |file, index| 
  specname = file.split('/').last.split('_')[0..-2].join('_')
  puts "specname #{specname}"
  command = "ruby tool/jt.rb untag #{file}"
  puts "starting #{num} of #{total} process #{command}"
  rout, wout = IO.pipe
  rerr, werr = IO.pipe
  pid = Process.spawn(command, :out => wout, :err => werr)
  finished = false
  begin
    Timeout.timeout(90) do
      puts "waiting for the  #{file} process to end"
      Process.wait(pid)
      finished = true
      puts "process #{file} finished in time"
    end
  rescue Timeout::Error
    puts "process #{file} with pid #{pid} not finished in time, killing it"
    Process.kill('TERM', pid)
  end
  # close write ends so we could read them
  wout.close unless wout.closed?
  werr.close unless werr.closed?
  
  @stdout = if finished
    rout.readlines.join("\n")
  else 
    ""
  end
  
  @stderr = if finished
    rerr.readlines.join("\n")
  else 
    ""
  end
 
  # dispose the read ends of the pipes
  rout.close unless rout.closed?
  rerr.close unless rerr.closed?
  
  tags_deleted = "no tags 'fails' were deleted"
  puts "finished #{finished}"
  puts "status #{$? != 0}"
  #puts @stdout
  puts "contains #{!@stdout.include?(tags_deleted)}"

  if finished && $? != 0 && !@stdout.include?(tags_deleted)
    test_command = "ruby tool/jt.rb test #{file}"
    puts "testing process #{test_command}"
    test_pid = Process.spawn(test_command)
      begin
    Timeout.timeout(90) do
      puts "waiting for the '#{test_command}' to end"
      Process.wait(test_pid)
      puts "process '#{test_command}' finished in time"
      if $? != 0
        tag_file = "spec/truffle/tags/core/#{file.split('/')[-2]}/#{specname}_tags.txt"
        print "\a"
        puts "resetting with command 'git checkout #{tag_file}"
        output = system("git checkout #{tag_file}")
        puts output
      end
    end
    rescue Timeout::Error
    puts "process #{file} with pid #{test_pid} not finished in time, killing it"
    Process.kill('TERM', test_pid)
    end
  end
  num += 1
end