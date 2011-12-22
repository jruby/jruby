require 'rbconfig'

describe 'JRUBY-6291: Closing Stream in IO.popen4' do
  it 'should not error when reading from other streams' do
    if RbConfig::CONFIG['host_os'] =~ /mingw|mswin/
      command = "#{ENV['COMSPEC']} /c echo success"
    else
      command = '/bin/echo success'
    end
    output = ""
    IO.popen4(command) do |pid, stdin, stdout, stderr|
      stdin.close
      stdout.each_line { |l| output << l }
    end
    output.strip.should == 'success'
  end
end
