require 'webrick'

root = File.expand_path '~/public_html'
server = WEBrick::HTTPServer.new :Port => 8080, :DocumentRoot => root

trap 'INT' do server.shutdown end

server.mount_proc '/' do |req, res|
  res.body = "Hello, world!\n"
end

server.start
