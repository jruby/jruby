require 'java'

describe "java_import" do

  it "should explode when trying to import a non-existant Java class" do
    expect do
      Module.new do
        java_import('does.not.Exist')
      end
    end.to raise_error(NameError, /Java class .?does.not.Exist.? not found/)
  end

  it "should receive an array with the imported classes" do
    mod = Module.new do
      @import_1 = java_import()
      @import_2 = java_import 'java.util.HashMap'

      @import_3 = java_import java.util.Hashtable, Java::java::util::Hashtable, Java::JavaUtil::Hashtable
    end

    expect(mod.instance_variable_get(:@import_1)).to eq([])
    expect(mod.instance_variable_get(:@import_2)).to eq([java.util.HashMap])
    expect(mod.instance_variable_get(:@import_3)).to eq([java.util.Hashtable] * 3)
  end

  it "should import named inner class" do
    mod = Module.new do
      java_import 'java.nio.file.WatchEvent$Kind'
    end
    expect(mod::Kind).to be Java::JavaNioFile::WatchEvent::Kind
  end

  java_import "java_integration.fixtures.InnerClasses"

  it "raises error importing lower-case names" do
    expect do
      Module.new { java_import InnerClasses::lowerInnerClass }
    end.to raise_error(NameError, /cannot import Java class .*?InnerClasses\$lowerInnerClass.*?: wrong constant name lowerInnerClass/)
  end

  it "imports upper-case names successfully" do
    mod = nil
    expect do
      mod = Module.new { java_import InnerClasses::CapsInnerClass }
    end.not_to raise_error
    expect(mod::CapsInnerClass).to eq(InnerClasses::CapsInnerClass)
  end

end

