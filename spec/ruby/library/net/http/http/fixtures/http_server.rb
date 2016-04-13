require 'webrick'
require 'webrick/httpservlet/abstract'

module NetHTTPSpecs
  class NullWriter
    def <<(s) end
    def puts(*args) end
    def print(*args) end
    def printf(*args) end
  end

  class SpecServlet < WEBrick::HTTPServlet::AbstractServlet
    def handle(req, res)
      reply(req, res)
    end

    %w{ do_GET do_HEAD do_POST do_PUT do_PROPPATCH do_LOCK do_UNLOCK
        do_OPTIONS do_PROPFIND do_DELETE do_MOVE do_COPY
        do_MKCOL do_TRACE }.each do |method|
      alias_method method.to_sym, :handle
    end
  end

  class RequestServlet < SpecServlet
    def reply(req, res)
      res.content_type = "text/plain"
      res.body = "Request type: #{req.request_method}"
    end
  end

  class RequestBodyServlet < SpecServlet
    def reply(req, res)
      res.content_type = "text/plain"
      res.body = req.body
    end
  end

  class RequestHeaderServlet < SpecServlet
    def reply(req, res)
      res.content_type = "text/plain"
      res.body = req.header.inspect
    end
  end

  class << self
    @server = nil
    @server_thread = nil

    def port
      @server ? @server.config[:Port] : 3333
    end

    def start_server
      server_config = {
        BindAddress: "localhost",
        Port: 0,
        Logger: WEBrick::Log.new(NullWriter.new),
        AccessLog: [],
        ServerType: Thread
      }

      @server = WEBrick::HTTPServer.new(server_config)

      @server.mount_proc('/') do |req, res|
        res.content_type = "text/plain"
        res.body = "This is the index page."
      end
      @server.mount('/request', RequestServlet)
      @server.mount("/request/body", RequestBodyServlet)
      @server.mount("/request/header", RequestHeaderServlet)

      @server_thread = @server.start
    end

    def stop_server
      @server.shutdown if @server
      if @server_thread
        ruby_version_is "2.2" do # earlier versions can stay blocked on IO.select
          @server_thread.join
        end
      end
      timeout = WEBrick::Utils::TimeoutHandler
      timeout.terminate if timeout.respond_to?(:terminate)
    end
  end
end
