require File.expand_path('../../../spec_helper', __FILE__)

require 'rbconfig'

# Generate a version.h file for specs to use
File.open File.expand_path("../ext/rubyspec_version.h", __FILE__), "w" do |f|
  # Yes, I know CONFIG variables exist for these, but
  # who knows when those could be removed without warning.
  major, minor, teeny = RUBY_VERSION.split(".")
  f.puts "#define RUBY_VERSION_MAJOR  #{major}"
  f.puts "#define RUBY_VERSION_MINOR  #{minor}"
  f.puts "#define RUBY_VERSION_TEENY  #{teeny}"
end

CAPI_RUBY_SIGNATURE = "#{RUBY_NAME}-#{RUBY_VERSION}"

def compile_extension(name)
  path = extension_path

  # TODO use rakelib/ext_helper.rb?
  arch_hdrdir = nil
  ruby_hdrdir = nil

  if RUBY_NAME == 'rbx'
    hdrdir = RbConfig::CONFIG["rubyhdrdir"]
  elsif RUBY_NAME =~ /^ruby/
    if hdrdir = RbConfig::CONFIG["rubyhdrdir"]
      arch_hdrdir = File.join hdrdir, RbConfig::CONFIG["arch"]
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

  ext       = File.join(path, "#{name}_spec")
  source    = "#{ext}.c"
  obj       = "#{ext}.o"
  lib       = "#{ext}.#{RbConfig::CONFIG['DLEXT']}"
  signature = "#{ext}.sig"

  ruby_header     = File.join(hdrdir, "ruby.h")
  rubyspec_header = File.join(path, "rubyspec.h")
  mri_header      = File.join(path, "mri.h")

  return lib if File.exists?(signature) and
                IO.read(signature).chomp == CAPI_RUBY_SIGNATURE and
                File.exists?(lib) and File.mtime(lib) > File.mtime(source) and
                File.mtime(lib) > File.mtime(ruby_header) and
                File.mtime(lib) > File.mtime(rubyspec_header) and
                File.mtime(lib) > File.mtime(mri_header)

  # avoid problems where compilation failed but previous shlib exists
  File.delete lib if File.exists? lib

  cc        = RbConfig::CONFIG["CC"]
  cflags    = (ENV["CFLAGS"] || RbConfig::CONFIG["CFLAGS"]).dup
  cflags   += " #{RbConfig::CONFIG["ARCH_FLAG"]}" if RbConfig::CONFIG["ARCH_FLAG"]
  cflags   += " -fPIC" unless cflags.include?("-fPIC")
  incflags  = "-I#{path} -I#{hdrdir}"
  incflags << " -I#{arch_hdrdir}" if arch_hdrdir
  incflags << " -I#{ruby_hdrdir}" if ruby_hdrdir

  output = `#{cc} #{incflags} #{cflags} -c #{source} -o #{obj}`

  if $?.exitstatus != 0 or !File.exists?(obj)
    puts "ERROR:\n#{output}"
    raise "Unable to compile \"#{source}\""
  end

  ldshared  = RbConfig::CONFIG["LDSHARED"]
  ldshared += " #{RbConfig::CONFIG["ARCH_FLAG"]}" if RbConfig::CONFIG["ARCH_FLAG"]
  libpath   = "-L#{path}"
  libs      = RbConfig::CONFIG["LIBS"]
  dldflags  = "#{RbConfig::CONFIG["LDFLAGS"]} #{RbConfig::CONFIG["DLDFLAGS"]}"
  dldflags.sub!(/-Wl,-soname,\S+/, '')

  output = `#{ldshared} #{obj} #{libpath} #{dldflags} #{libs} -o #{lib}`

  if $?.exitstatus != 0
    puts "ERROR:\n#{output}"
    raise "Unable to link \"#{source}\""
  end

  File.open(signature, "w") { |f| f.puts CAPI_RUBY_SIGNATURE }

  lib
end

def extension_path
  File.expand_path("../ext", __FILE__)
end

def load_extension(name)
  require compile_extension(name)
end

# Constants
CAPI_SIZEOF_LONG = [0].pack('l!').size
