
workaround_header = <<-HEREDOC
==========================================
WORKING AROUND BUNDLE INSTALL ISSUES
Instructions:
  1. Run bundle install command with workarounds. E.g.:
      GEM_HOME=/Users/brandonfish/Documents/truffle_gem_home ../../jruby/bin/jruby -X+T -rbundler-workarounds -S bundle install
  2. Try running gem tests, e.g.:
      GEM_HOME=/Users/brandonfish/Documents/truffle_gem_home ../../jruby/bin/jruby -X+T -rbundler-workarounds -S bundle exec rake
HEREDOC
puts workaround_header if $VERBOSE



have_extensions = <<-HEREDOC
==========================================
Workaround: Add nil check Gem::BasicSpecification.have_extensions?
Error:
  undefined method `empty?' for nil:NilClass
Called here:
  lib/ruby/stdlib/rubygems/basic_specification.rb:313
HEREDOC
puts have_extensions if $VERBOSE
class Gem::BasicSpecification
  def have_extensions?; !extensions.nil? && !extensions.empty?; end
end


remote_fetcher = <<-HEREDOC
==========================================
Workaround: Use wget in remote fetcher
HEREDOC
puts remote_fetcher if $VERBOSE

require 'rubygems/remote_fetcher'
class Gem::RemoteFetcher
  def download(spec, source_uri, install_dir = Gem.dir)
    cache_dir =
        if Dir.pwd == install_dir then # see fetch_command
          install_dir
        elsif File.writable? install_dir then
          File.join install_dir, "cache"
        else
          File.join Gem.user_dir, "cache"
        end

    gem_file_name = File.basename spec.cache_file
    local_gem_path = File.join cache_dir, gem_file_name

    FileUtils.mkdir_p cache_dir rescue nil unless File.exist? cache_dir

    # Always escape URI's to deal with potential spaces and such
    # It should also be considered that source_uri may already be
    # a valid URI with escaped characters. e.g. "{DESede}" is encoded
    # as "%7BDESede%7D". If this is escaped again the percentage
    # symbols will be escaped.
    unless source_uri.is_a?(URI::Generic)
      begin
        source_uri = URI.parse(source_uri)
      rescue
        source_uri = URI.parse(URI.const_defined?(:DEFAULT_PARSER) ?
                                   URI::DEFAULT_PARSER.escape(source_uri.to_s) :
                                   URI.escape(source_uri.to_s))
      end
    end

    scheme = source_uri.scheme

    # URI.parse gets confused by MS Windows paths with forward slashes.
    scheme = nil if scheme =~ /^[a-z]$/i

    # REFACTOR: split this up and dispatch on scheme (eg download_http)
    # REFACTOR: be sure to clean up fake fetcher when you do this... cleaner
    case scheme
      when 'http', 'https', 's3' then
        unless File.exist? local_gem_path then
          begin
            verbose "Downloading gem #{gem_file_name}"

            remote_gem_path = source_uri + "gems/#{gem_file_name}"

            # WORKAROUND STARTS HERE
            cmd = "wget -q #{remote_gem_path} -O #{local_gem_path}"
            `#{cmd}`
              # self.cache_update_path remote_gem_path, local_gem_path
            # WORKAROUND ENDS HERE
          rescue Gem::RemoteFetcher::FetchError
            raise if spec.original_platform == spec.platform

            alternate_name = "#{spec.original_name}.gem"

            verbose "Failed, downloading gem #{alternate_name}"

            remote_gem_path = source_uri + "gems/#{alternate_name}"

            self.cache_update_path remote_gem_path, local_gem_path
          end
        end
      when 'file' then
        begin
          path = source_uri.path
          path = File.dirname(path) if File.extname(path) == '.gem'

          remote_gem_path = correct_for_windows_path(File.join(path, 'gems', gem_file_name))

          FileUtils.cp(remote_gem_path, local_gem_path)
        rescue Errno::EACCES
          local_gem_path = source_uri.to_s
        end

        verbose "Using local gem #{local_gem_path}"
      when nil then # TODO test for local overriding cache
        source_path = if Gem.win_platform? && source_uri.scheme &&
            !source_uri.path.include?(':') then
                        "#{source_uri.scheme}:#{source_uri.path}"
                      else
                        source_uri.path
                      end

        source_path = Gem::UriFormatter.new(source_path).unescape

        begin
          FileUtils.cp source_path, local_gem_path unless
              File.identical?(source_path, local_gem_path)
        rescue Errno::EACCES
          local_gem_path = source_uri.to_s
        end

        verbose "Using local gem #{local_gem_path}"
      else
        raise ArgumentError, "unsupported URI scheme #{source_uri.scheme}"
    end

    local_gem_path
  end
end

remote_fetcher_api = <<-HEREDOC
==========================================
Workaround: Hardcode DNS Resolution to rubygems.org for gem install
HEREDOC
puts remote_fetcher_api if $VERBOSE

class Gem::RemoteFetcher
  def api_endpoint(uri)
    host = uri.host

    begin
      res = if host != "rubygems.org" && host != "api.rubygems.org"
              @dns.getresource "_rubygems._tcp.#{host}", Resolv::DNS::Resource::IN::SRV
            else
              nil
            end
    rescue Resolv::ResolvError => e
      verbose "Getting SRV record failed: #{e}"
      uri
    else
      target = if host != "rubygems.org" && host != "api.rubygems.org"
                  res.target.to_s.strip
               else
                 'api.rubygems.org'
               end

      if /\.#{Regexp.quote(host)}\z/ =~ target
        return URI.parse "#{uri.scheme}://#{target}#{uri.path}"
      end

      uri
    end
  end
end


ruby_version = <<-HEREDOC
==========================================
Workaround: Set RUBY_ENGINE to 'ruby'
Error:
  RUBY_ENGINE value jruby+truffle is not recognized
Called here:
  lib/bundler/ruby_version.rb:98
HEREDOC
puts ruby_version if $VERBOSE
RUBY_ENGINE = "ruby"


curl_https = <<-HEREDOC
==========================================
Workaround: Use curl when URIs are https
HEREDOC
puts curl_https if $VERBOSE
class CurlResponse < Net::HTTPOK
  def body
    @body
  end
end
require 'rubygems/request'
class Gem::Request
  def fetch
    request = @request_class.new @uri.request_uri

    unless @uri.nil? || @uri.user.nil? || @uri.user.empty? then
      request.basic_auth Gem::UriFormatter.new(@uri.user).unescape,
                         Gem::UriFormatter.new(@uri.password).unescape
    end

    request.add_field 'User-Agent', @user_agent
    request.add_field 'Connection', 'keep-alive'
    request.add_field 'Keep-Alive', '30'

    if @last_modified then
      request.add_field 'If-Modified-Since', @last_modified.httpdate
    end

    yield request if block_given?

    # WORKAROUND START
    if @uri.scheme == "https"
      resp = CurlResponse.new("1.1", 200, "OK")
      resp_raw = `curl -i -s #{@uri}`
      blank_line_idx = resp_raw.index("\r\n\r\n")
      header = resp_raw[0, blank_line_idx]
      resp.body = resp_raw[(blank_line_idx+4)..-1]
      if m = /ETag: (\"[[:alnum:]]*\")/.match(header)
        resp["ETag"] = m[1]
      end
      resp
    else
      perform_request request
    end
    # WORKAROUND END
  end
end

rubygems_package = <<-HEREDOC
      ==========================================
      Workarounds:
        Gem::Package
        - shell to tar for untarring
        - skip some checksum/digest verification
HEREDOC
puts rubygems_package if $VERBOSE
require 'rubygems/package'
class Gem::Package
  def extract_files destination_dir, pattern = "*"
    verify unless @spec

    FileUtils.mkdir_p destination_dir

    # WORKAROUND START
    extr_to = File.dirname(@gem.path) + '/' + File.basename(@gem.path, File.extname(@gem.path))
    Dir.mkdir(extr_to)
    `tar -C #{extr_to} -xf #{@gem.path}`
    # WORKAROUND END

    @gem.with_read_io do |io|
      reader = Gem::Package::TarReader.new io

      reader.each do |entry|
        next unless entry.full_name == 'data.tar.gz'

        # WORKAROUND START
        `tar -C #{destination_dir} -xzf #{extr_to + '/' + entry.full_name}`
        #extract_tar_gz entry, destination_dir, pattern
        FileUtils.remove_dir(extr_to)
        # WORKAROUND END

        return # ignore further entries
      end
    end
  end

  def verify
    @files     = []
    @spec      = nil

    @gem.with_read_io do |io|
      Gem::Package::TarReader.new io do |reader|
        read_checksums reader

        verify_files reader
      end
    end

    # WORKAROUND START
    #verify_checksums @digests, @checksums
    # WORKAROUND END

    @security_policy.verify_signatures @spec, @digests, @signatures if
        @security_policy

    true
  rescue Gem::Security::Exception
    @spec = nil
    @files = []
    raise
  rescue Errno::ENOENT => e
    raise Gem::Package::FormatError.new e.message
  rescue Gem::Package::TarInvalidError => e
    raise Gem::Package::FormatError.new e.message, @gem
  end

  def verify_entry entry
    file_name = entry.full_name
    @files << file_name

    case file_name
      when /\.sig$/ then
        @signatures[$`] = entry.read if @security_policy
        return
      else
        # WORKAROUND START
        #digest entry
        # WORKAROUND END
    end

    case file_name
      when /^metadata(.gz)?$/ then
        load_spec entry
      when 'data.tar.gz' then
        # WORKAROUND START
        #verify_gz entry
        # WORKAROUND END
    end
  rescue => e
    message = "package is corrupt, exception while verifying: " +
        "#{e.message} (#{e.class})"
    raise Gem::Package::FormatError.new message, @gem
  end

end


zlib_crc = <<-HEREDOC
==========================================
Workaround: Disable zlib crc check
HEREDOC
puts zlib_crc if $VERBOSE
require "zlib"
module Zlib
  class GzipFile
    def gzfile_check_footer()
      @gz.z.flags |= GZFILE_FLAG_FOOTER_FINISHED

      if (!gzfile_read_raw_ensure(8))
        raise NoFooter, "footer is not found"
      end
      crc = gzfile_get32(@gz.z.input)
      length = gzfile_get32(@gz.z.input[4,4])
      @gz.z.stream.total_in += 8
      @gz.z.zstream_discard_input(8)
      # WORKAROUND START
      # if (@gz.crc != crc)
      #   raise CRCError, "invalid compressed data -- crc error"
      # end
      # WORKAROUND END
      if (@gz.z.stream.total_out != length)
        raise LengthError, "invalid compressed data -- length error"
      end
    end
  end
end


bundler_updater = <<-HEREDOC
==========================================
Workaround: Shell to gunzip in bundler updater
HEREDOC
puts bundler_updater if $VERBOSE
bundler_loaded = false
begin
  require "bundler"
  require "bundler/vendor/compact_index_client/lib/compact_index_client"
  bundler_loaded = true
rescue LoadError
end
if bundler_loaded
  class Bundler::CompactIndexClient
    class Updater
      def update(local_path, remote_path, retrying = nil)
        headers = {}

        Dir.mktmpdir(local_path.basename.to_s, local_path.dirname) do |local_temp_dir|

          local_temp_path = Pathname.new(local_temp_dir).join(local_path.basename)


          # download new file if retrying
          if retrying.nil? && local_path.file?
            FileUtils.cp local_path, local_temp_path
            headers["If-None-Match"] = etag_for(local_temp_path)
            headers["Range"] = "bytes=#{local_temp_path.size}-"
          else
            # Fastly ignores Range when Accept-Encoding: gzip is set
            headers["Accept-Encoding"] = "gzip"
          end

          response = @fetcher.call(remote_path, headers)
          return if response.is_a?(Net::HTTPNotModified)

          content = response.body

          if response["Content-Encoding"] == "gzip"
            IO.binwrite("#{local_temp_dir}/gzipped_versions", content)

            #content = Zlib::GzipReader.new(StringIO.new(content)).read
            content = `gunzip -c #{local_temp_dir}/gzipped_versions`
          end

          mode = response.is_a?(Net::HTTPPartialContent) ? "a" : "w"
          local_temp_path.open(mode) { |f| f << content }


          response_etag = response["ETag"]

          if etag_for(local_temp_path) == response_etag
            FileUtils.mv(local_temp_path, local_path)
            return
          end

          if retrying.nil?
            update(local_path, remote_path, :retrying)
          else
            # puts "ERROR: #{remote_path},#{local_path},#{local_temp_path}"
            raise MisMatchedChecksumError.new(remote_path, response_etag, etag_for(local_temp_path))
          end
        end
      end
    end
  end
end


module OpenSSL

  module Cipher

    cipher_new = <<-HEREDOC
      ==========================================
      Workaround: Stub OpenSSL::Cipher.new
      Called here:
        lib/ruby/stdlib/rubygems/security.rb:372
    HEREDOC
    puts cipher_new if $VERBOSE

    def self.new(enc)

    end

  end

  verify_peer = <<-HEREDOC
      ==========================================
      Workaround: Stub OpenSSL::SSL::VERIFY_PEER
      Error:
        uninitialized constant OpenSSL::SSL::VERIFY_PEER
      Called here:
        bundler/vendor/net/http/persistent.rb:519
  HEREDOC
  puts verify_peer if $VERBOSE
  module SSL
    VERIFY_PEER = 1
  end

  verify_none = <<-HEREDOC
      ==========================================
      Workaround: Stub OpenSSL::SSL::VERIFY_NONE
      Error:
        uninitialized constant OpenSSL::SSL::VERIFY_NONE
      Called here:
        bundler/vendor/net/http/persistent.rb:1142
  HEREDOC
  puts verify_none if $VERBOSE
  module SSL
    VERIFY_NONE = 0
  end

  #   module Digest
  #     def self.new(enc)
  #
  #     end
  #   end

  module X509
    class Store
      def set_default_paths

      end
      def add_file(f)

      end
    end

  end

end

bundler_fetcher = <<-HEREDOC
==========================================
Workaround: Use curl in bundler downloader for https requests
HEREDOC
puts bundler_fetcher if $VERBOSE

require "openssl-stubs"

if bundler_loaded
  require "bundler/fetcher/downloader"

  module Bundler
    class Fetcher
      class Downloader
        def fetch(uri, options = {}, counter = 0)
          raise HTTPError, "Too many redirects" if counter >= redirect_limit

          # WORKAROUND START
          response = if uri.scheme == "https"
                       resp = CurlResponse.new("1.1", 200, "OK")
                       resp_raw = `curl -i -s #{uri}`
                       blank_line_idx = resp_raw.index("\r\n\r\n")
                       header = resp_raw[0, blank_line_idx]
                       resp.body = resp_raw[(blank_line_idx+4)..-1]
                       if m = /ETag: (\"[[:alnum:]]*\")/.match(header)
                         resp["ETag"] = m[1]
                       end
                       resp
                     else
                       response = request(uri, options)
                     end
          # WORKAROUND END

          Bundler.ui.debug("HTTP #{response.code} #{response.message}")

          case response
            when Net::HTTPSuccess, Net::HTTPNotModified
              response
            when Net::HTTPRedirection
              new_uri = URI.parse(response["location"])
              if new_uri.host == uri.host
                new_uri.user = uri.user
                new_uri.password = uri.password
              end
              fetch(new_uri, options, counter + 1)
            when Net::HTTPRequestEntityTooLarge
              raise FallbackError, response.body
            when Net::HTTPUnauthorized
              raise AuthenticationRequiredError, uri.host
            when Net::HTTPNotFound
              raise FallbackError, "Net::HTTPNotFound"
            else
              raise HTTPError, "#{response.class}#{": #{response.body}" unless response.body.empty?}"
          end
        end

      end
    end
  end
end

resolver_search_for = <<-HEREDOC
==========================================
Workaround: Change =~ to == for resolver#search_for
Error:  type mismatch: String given
        stdlib/rubygems/resolver.rb:237:in `block in search_for'
HEREDOC
puts resolver_search_for if $VERBOSE
require "rubygems/resolver"
class Gem::Resolver
  def search_for(dependency)
    possibles, all = find_possible(dependency)
    if !@soft_missing && possibles.empty?
      @missing << dependency
      exc = Gem::UnsatisfiableDependencyError.new dependency, all
      exc.errors = @set.errors
      raise exc
    end
    possibles.sort_by { |s| [s.source, s.version, Gem::Platform.local == s.platform ? 1 : 0] }.
        map { |s| ActivationRequest.new s, dependency, [] }
  end
end

native_extensions = <<-HEREDOC
==========================================
Workaround: Ignore native extensions
Error:  Gem::Ext::BuildError: ERROR: Failed to build gem native extension.
        This can be used to ignore building gem extensions until this is working correctly.
HEREDOC
puts native_extensions if $VERBOSE
require "rubygems/ext"

class Gem::Ext::Builder
  def build_extensions
    return if @spec.extensions.empty?

    if @build_args.empty?
      say "Building native extensions.  This could take a while..."
    else
      say "Building native extensions with: '#{@build_args.join ' '}'"
      say "This could take a while..."
    end

    dest_path = @spec.extension_dir

    FileUtils.rm_f @spec.gem_build_complete_path

    @ran_rake = false # only run rake once

    @spec.extensions.each do |extension|
      break if @ran_rake
      puts "WORKAROUND: Skipping build extension:#{extension}, dest_path:#{dest_path}"
      #build_extension extension, dest_path
    end

    # This doesn't exist when we skip building
    #FileUtils.touch @spec.gem_build_complete_path
  end
end

puts "==========================================" if $VERBOSE
