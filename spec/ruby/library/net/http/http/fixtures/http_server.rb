require 'webrick'
require 'webrick/httpservlet/abstract'

module NetHTTPSpecs
 class NullWriter
    def <<(s) end
    def puts(*args) end
    def print(*args) end
    def printf(*args) end
  end

  class RequestServlet < WEBrick::HTTPServlet::AbstractServlet
    def do_GET(req, res)
      res.content_type = "text/plain"
      res.body = "Request type: #{req.request_method}"
    end

    %w{ do_HEAD do_POST do_PUT do_PROPPATCH do_LOCK do_UNLOCK
        do_OPTIONS do_PROPFIND do_DELETE do_MOVE do_COPY
        do_MKCOL do_TRACE }.each do |method|
      alias_method method.to_sym, :do_GET
    end
  end

  class RequestBodyServlet < WEBrick::HTTPServlet::AbstractServlet
    def do_GET(req, res)
      res.content_type = "text/plain"
      res.body = req.body
    end

    %w{ do_HEAD do_POST do_PUT do_PROPPATCH do_LOCK do_UNLOCK
        do_OPTIONS do_PROPFIND do_DELETE do_MOVE do_COPY
        do_MKCOL do_TRACE }.each do |method|
      alias_method method.to_sym, :do_GET
    end
  end

  class RequestHeaderServlet < WEBrick::HTTPServlet::AbstractServlet
    def do_GET(req, res)
      res.content_type = "text/plain"
      res.body = req.header.inspect
    end

    %w{ do_HEAD do_POST do_PUT do_PROPPATCH do_LOCK do_UNLOCK
        do_OPTIONS do_PROPFIND do_DELETE do_MOVE do_COPY
        do_MKCOL do_TRACE }.each do |method|
      alias_method method.to_sym, :do_GET
    end
  end

  class << self
    @server = nil

    def start_server
      unless @server
        server_config = {
          :BindAddress => "localhost",
          :Port => 3333,
          :Logger => WEBrick::Log.new(NullWriter.new),
          :AccessLog => [],
          :ShutdownSocketWithoutClose => true,
          :ServerType => Thread
        }

        @server = WEBrick::HTTPServer.new(server_config)

        @server.mount_proc('/') do |req, res|
          res.content_type = "text/plain"
          res.body = "This is the index page."
        end
        @server.mount('/request', RequestServlet)
        @server.mount("/request/body", RequestBodyServlet)
        @server.mount("/request/header", RequestHeaderServlet)

        @server.start
      end

      # On initial startup or if we re-enter, we wait until the
      # server is really running.
      Thread.pass until @server.status == :Running
    end

    def shutdown_server
      @server.shutdown if @server
    end

    def stop_server
      # The specs initially started and stopped the server for every
      # describe block. This method is now a noop. The server is shutdown
      # automatically when the spec process exits.
    end
  end

  at_exit do
    shutdown_server
  end
end
