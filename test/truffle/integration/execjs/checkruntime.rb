unless defined?(Truffle) && Truffle::Interop.mime_type_supported?('application/javascript')
  puts "JavaScript doesn't appear to be available - skipping execjs test"
  exit
end

require 'execjs'
require 'truffle/execjs'

exit 1 unless ExecJS.runtime.name == 'Truffle'
