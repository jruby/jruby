#
# This file is part of ruby-ffi.
# For licensing, see LICENSE.SPECS
#

require 'ffi'
require_relative 'spec_helper'

describe "FFI::Platform::LIBSUFFIX" do
  it "returns 'so'", if: RbConfig::CONFIG['host_os'].match('linux') do
    FFI::Platform::LIBSUFFIX.should == 'so'
  end

  it "returns 'dll'", if: RbConfig::CONFIG['host_os'].match('windows') do
    FFI::Platform::LIBSUFFIX.should == 'dll'
  end

  it "returns 'dylib'", if: RbConfig::CONFIG['host_os'].match('darwin') do
    FFI::Platform::LIBSUFFIX.should == 'dylib'
  end
end

describe "FFI::Platform::IS_WINDOWS" do
  it "returns false", if: RbConfig::CONFIG['host_os'].match('linux') do
    FFI::Platform::IS_WINDOWS.should == false
  end

  it "returns true", if: RbConfig::CONFIG['host_os'].match('windows') do
    FFI::Platform::IS_WINDOWS.should == true
  end

  it "returns false", if: RbConfig::CONFIG['host_os'].match('darwin') do
    FFI::Platform::IS_WINDOWS.should == false
  end
end

describe "FFI::Platform::ARCH" do
  it "returns the architecture type" do
    FFI::Platform::ARCH.should == RbConfig::CONFIG["target_cpu"]
  end
end

describe "FFI::Platform::OS" do
  it "returns 'linux' as a string", if: RbConfig::CONFIG['host_os'].match('linux') do
    FFI::Platform::OS.should == 'linux'
  end

  it "returns 'windows' as a string", if: RbConfig::CONFIG['host_os'].match('windows') do
    FFI::Platform::OS.should == 'windows'
  end

  it "returns 'darwin' as a string", if: RbConfig::CONFIG['host_os'].match('darwin') do
    FFI::Platform::OS.should == 'darwin'
  end

  describe "FFI::Platform.windows?" do
    it "returns false", if: RbConfig::CONFIG['host_os'].match('linux') do
      FFI::Platform.windows?.should == false
    end

    it "returns true", if: RbConfig::CONFIG['host_os'].match('windows') do
      FFI::Platform.windows?.should == true
    end

    it "returns false", if: RbConfig::CONFIG['host_os'].match('darwin') do
      FFI::Platform.windows?.should == false
    end
  end

  describe "FFI::Platform.mac?" do
    it "returns false", if: RbConfig::CONFIG['host_os'].match('linux') do
      FFI::Platform.mac?.should == false
    end

    it "returns false", if: RbConfig::CONFIG['host_os'].match('windows') do
      FFI::Platform.mac?.should == false
    end

    it "returns true", if: RbConfig::CONFIG['host_os'].match('darwin') do
      FFI::Platform.mac?.should == true
    end
  end

  describe "FFI::Platform.unix?" do
    it "returns true", if: RbConfig::CONFIG['host_os'].match('linux') do
      FFI::Platform.unix?.should == true
    end

    it "returns false", if: RbConfig::CONFIG['host_os'].match('windows') do
      FFI::Platform.unix?.should == false
    end

    it "returns true", if: RbConfig::CONFIG['host_os'].match('darwin') do
      FFI::Platform.unix?.should == true
    end
  end
end
