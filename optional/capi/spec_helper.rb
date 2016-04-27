require File.expand_path('../../../spec_helper', __FILE__)
$extmk = false

require 'rbconfig'
require 'fileutils'

OBJDIR ||= File.expand_path("../../../ext/#{RUBY_NAME}/#{RUBY_VERSION}", __FILE__)
FileUtils.makedirs(OBJDIR)

def extension_path
  File.expand_path("../ext", __FILE__)
end

def object_path
  OBJDIR
end

def compile_extension(name)
  preloadenv = RbConfig::CONFIG["PRELOADENV"] || "LD_PRELOAD"
  preload, ENV[preloadenv] = ENV[preloadenv], nil if preloadenv

  path = extension_path
  objdir = object_path

  # TODO use rakelib/ext_helper.rb?
  arch_hdrdir = nil
  ruby_hdrdir = nil

  if RUBY_NAME == 'rbx'
    hdrdir = RbConfig::CONFIG["rubyhdrdir"]
  elsif RUBY_NAME =~ /^ruby/
    if hdrdir = RbConfig::CONFIG["rubyhdrdir"]
      arch_hdrdir = RbConfig::CONFIG["rubyarchhdrdir"] ||
                    File.join(hdrdir, RbConfig::CONFIG["arch"])
      ruby_hdrdir = File.join hdrdir, "ruby"
    else
      hdrdir = RbConfig::CONFIG["archdir"]
    end
  elsif RUBY_NAME == 'jruby'
    require 'mkmf'
    hdrdir = $hdrdir
  elsif RUBY_NAME == "maglev"
    require 'mkmf'
    hdrdir = $hdrdir
  else
    raise "Don't know how to build C extensions with #{RUBY_NAME}"
  end

  ext       = "#{name}_spec"
  source    = File.join(path, "#{ext}.c")
  obj       = File.join(objdir, "#{ext}.#{RbConfig::CONFIG['OBJEXT']}")
  lib       = File.join(objdir, "#{ext}.#{RbConfig::CONFIG['DLEXT']}")

  ruby_header     = File.join(hdrdir, "ruby.h")
  rubyspec_header = File.join(path, "rubyspec.h")

  return lib if File.exist?(lib) and File.mtime(lib) > File.mtime(source) and
                File.mtime(lib) > File.mtime(ruby_header) and
                File.mtime(lib) > File.mtime(rubyspec_header) and
                true            # sentinel

  # avoid problems where compilation failed but previous shlib exists
  File.delete lib if File.exist? lib

  cc        = RbConfig::CONFIG["CC"]
  cflags    = (ENV["CFLAGS"] || RbConfig::CONFIG["CFLAGS"]).dup
  cflags   += " #{RbConfig::CONFIG["ARCH_FLAG"]}" if RbConfig::CONFIG["ARCH_FLAG"]
  cflags   += " #{RbConfig::CONFIG["CCDLFLAGS"]}" if RbConfig::CONFIG["CCDLFLAGS"]
  incflags  = "-I#{path} -I#{hdrdir}"
  incflags << " -I#{arch_hdrdir}" if arch_hdrdir
  incflags << " -I#{ruby_hdrdir}" if ruby_hdrdir

  output = `#{cc} #{incflags} #{cflags} -c #{source} -o #{obj}`

  unless $?.success? and File.exist?(obj)
    puts "ERROR:\n#{output}"
    puts "incflags=#{incflags}"
    puts "cflags=#{cflags}"
    raise "Unable to compile \"#{source}\""
  end

  ldshared  = RbConfig::CONFIG["LDSHARED"]
  ldshared += " #{RbConfig::CONFIG["ARCH_FLAG"]}" if RbConfig::CONFIG["ARCH_FLAG"]
  libpath   = "-L#{path}"
  libs      = RbConfig::CONFIG["LIBS"]
  dldflags  = "#{RbConfig::CONFIG["LDFLAGS"]} #{RbConfig::CONFIG["DLDFLAGS"]}"
  dldflags.sub!(/-Wl,-soname,\S+/, '')

  link_cmd = "#{ldshared} #{obj} #{libpath} #{dldflags} #{libs} -o #{lib}"
  output = `#{link_cmd}`

  unless $?.success?
    puts "ERROR:\n#{link_cmd}\n#{output}"
    raise "Unable to link \"#{source}\""
  end

  lib
ensure
  ENV[preloadenv] = preload if preloadenv
end

def load_extension(name)
  require compile_extension(name)
end

# Constants
CAPI_SIZEOF_LONG = [0].pack('l!').size
