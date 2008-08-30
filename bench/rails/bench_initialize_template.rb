require File.dirname(__FILE__) + '/config/environment.rb'
require 'action_controller/integration'
require 'benchmark'

env = {"QUERY_STRING"=>nil, "REQUEST_METHOD"=>"GET", "REQUEST_URI"=>"/no_session/do_something", "HTTP_HOST"=>"www.example.com", "REMOTE_ADDR"=>"127.0.0.1", "SERVER_PORT"=>"80", "CONTENT_TYPE"=>"application/x-www-form-urlencoded", "CONTENT_LENGTH"=>nil, "HTTP_COOKIE"=>"", "HTTPS"=>"off", "HTTP_ACCEPT"=>"text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5"}
data = nil

cgi = ActionController::Integration::Session::StubCGI.new(env, data)
session_options = {:database_manager=>CGI::Session::CookieStore, :prefix=>"ruby_sess.", :session_path=>"/", :session_key=>"_session_id", :cookie_only=>true, :tmpdir=>"tmp/sessions/"}
request = ActionController::CgiRequest.new(cgi, session_options)
response = ActionController::CgiResponse.new(cgi)

(ARGV[0] || 10).to_i.times {
  Benchmark.bm(30) {|bm|
    controller = ActionController::Routing::Routes.recognize(request).new
    class << controller; public :initialize_template_class; end
    bm.report("initialize_template_class") { 1000.times { controller.initialize_template_class(response) } }
  }
}