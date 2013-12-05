require File.expand_path('../../../spec_helper', __FILE__)

describe "FFI::Platform::LIBSUFFIX" do
  platform_is :linux do
    it "returns 'so'" do
      FFI::Platform::LIBSUFFIX.should == 'so'
    end
  end

  platform_is :windows do
    it "returns 'dll'" do
      FFI::Platform::LIBSUFFIX.should == 'dll'
    end
  end

  platform_is :darwin do
    it "returns 'dylib'" do
      FFI::Platform::LIBSUFFIX.should == 'dylib'
    end
  end
end

describe "FFI::Platform::IS_WINDOWS" do
  platform_is :linux do
    it "returns false" do
      FFI::Platform::IS_WINDOWS.should == false
    end
  end

  platform_is :windows do
    it "returns true" do
      FFI::Platform::IS_WINDOWS.should == true
    end
  end

  platform_is :darwin do
    it "returns false" do
      FFI::Platform::IS_WINDOWS.should == false
    end
  end
end

describe "FFI::Platform::ARCH" do
  it "returns the architecture type" do
    FFI::Platform::ARCH.should == Rubinius::CPU
  end
end

describe "FFI::Platform::OS" do
  platform_is :linux do
    it "returns 'linux' as a string" do
      FFI::Platform::OS.should == 'linux'
    end
  end

  platform_is :windows do
    it "returns 'windows' as a string" do
      FFI::Platform::OS.should == 'windows'
    end
  end

  platform_is :darwin do
    it "returns 'darwin' as a string" do
      FFI::Platform::OS.should == 'darwin'
    end
  end

  describe "FFI::Platform.windows?" do
    platform_is :linux do
      it "returns false" do
        FFI::Platform.windows?.should == false
      end
    end

    platform_is :windows do
      it "returns true" do
        FFI::Platform.windows?.should == true
      end
    end

    platform_is :darwin do
      it "returns false" do
        FFI::Platform.windows?.should == false
      end
    end
  end

  describe "FFI::Platform.mac?" do
    platform_is :linux do
      it "returns false" do
        FFI::Platform.mac?.should == false
      end
    end

    platform_is :windows do
      it "returns false" do
        FFI::Platform.mac?.should == false
      end
    end

    platform_is :darwin do
      it "returns true" do
        FFI::Platform.mac?.should == true
      end
    end
  end

  describe "FFI::Platform.unix?" do
    platform_is :linux do
      it "returns true" do
        FFI::Platform.unix?.should == true
      end
    end

    platform_is :windows do
      it "returns false" do
        FFI::Platform.unix?.should == false
      end
    end

    platform_is :darwin do
      it "returns true" do
        FFI::Platform.unix?.should == true
      end
    end
  end
end
