unless defined? FFI
  begin
    require "ffi"
  rescue LoadError
    require "rubygems"
    require "ffi"
  end
end

module FFISpecs
  include FFI

  LongSize = FFI::Platform::LONG_SIZE / 8

  FIXTURE_DIR = File.expand_path("../fixtures", __FILE__)
  LIBRARY = File.join(FIXTURE_DIR, "build/libtest/libtest.#{FFI::Platform::LIBSUFFIX}")

  def self.need_to_compile_fixtures?
    !File.exist?(LIBRARY) or Dir.glob(File.join(FIXTURE_DIR, "*.c")).any? { |f| File.mtime(f) > File.mtime(LIBRARY) }
  end

  if need_to_compile_fixtures?
    puts "[!] Compiling Ruby-FFI fixtures"
    Dir.chdir(File.dirname(FIXTURE_DIR)) do
      unless system("make -f fixtures/GNUmakefile")
        raise "Failed to compile Ruby-FFI fixtures"
      end
    end
  end

  module LibTest
    extend FFI::Library
    ffi_lib LIBRARY
  end
end

require File.join(FFISpecs::FIXTURE_DIR, 'classes')
