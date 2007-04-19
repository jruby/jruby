require 'timeout'

begin

1_000_000.times {
  begin
    Timeout.timeout(0.001) {sleep 0.001}
  rescue Timeout::Error
    puts 'timeout'
  else
    puts 'ok'
  end
}

rescue Timeout::Error
  puts "should never be here"
end
