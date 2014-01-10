#
# This file is part of ruby-ffi.
# For licensing, see LICENSE.SPECS
#
require 'rbconfig'

FFI_RUBY_SIGNATURE = "#{RUBY_NAME}-#{RUBY_VERSION}"

def compile_file(file)
  base      = file.gsub(/\.[^\.]+\Z/, "")
  obj       = "#{base}.o"
  signature = "#{base}.sig"

  return false if File.exists?(signature) and
                  IO.read(signature).chomp == FFI_RUBY_SIGNATURE and
                  File.exists?(obj) and File.mtime(obj) > File.mtime(file)

  cc        = RbConfig::CONFIG["CC"]
  cflags    = RbConfig::CONFIG["CFLAGS"]
  output    = `#{cc} #{cflags} #{ENV["CFLAGS"]} -c #{file} -o #{obj}`

  if $?.exitstatus != 0 or !File.exists?(obj)
    puts "ERROR:\n#{file}"
    raise "Unable to compile \"#{file}\""
  end

  File.open(signature, "w") { |f| f.puts FFI_RUBY_SIGNATURE }
  true
end

def compile_library(path, lib)

  dir = File.expand_path("../#{path}", __FILE__)
  files = Dir["#{dir}/*.{c,cpp}"]
  objs  = files.map do |f|
    base      = f.gsub(/\.[^\.]+\Z/, "")
    "#{base}.o"
  end
  needs_compile = false
  files.each do |file|
    file_compiled = compile_file(file)
    needs_compile ||= file_compiled
  end

  lib = "#{dir}/#{lib}"
  if !File.exists?(lib) || needs_compile
    ldshared  = RbConfig::CONFIG["LDSHARED"]
    libs      = RbConfig::CONFIG["LIBS"]
    dldflags  = RbConfig::CONFIG["DLDFLAGS"]

    output = `#{ldshared} #{objs.join(" ")} #{dldflags} #{libs} -o #{lib}`

    if $?.exitstatus != 0
      puts "ERROR:\n#{output}"
      raise "Unable to link \"#{source}\""
    end
  end

  lib
end

require "ffi"

module TestLibrary
  PATH = compile_library("fixtures", "libtest.#{FFI::Platform::LIBSUFFIX}")
end
module LibTest
  extend FFI::Library
  ffi_lib TestLibrary::PATH
end
