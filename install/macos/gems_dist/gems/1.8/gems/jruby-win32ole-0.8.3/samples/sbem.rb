require 'win32ole'

VALUES = ['CategoryString', 'Message', 'TimeGenerated', 'User', 'Type']
query = "select #{VALUES.join(',')} from Win32_NtLogEvent where Logfile = 'Application' and TimeGenerated > '20100713000000.000000-***'"

wmi = WIN32OLE.new "WbemScripting.SWbemLocator"
connection = wmi.connectserver
connection.ExecQuery(query).each do |result|
  puts VALUES.map { |v| "#{v}: #{result.__send__(v).to_s}" }.join(", ")
end

