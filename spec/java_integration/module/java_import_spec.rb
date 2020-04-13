require 'java'

describe "java_import" do

  it "should explode when trying to import a non-existant Java class" do
    expect do
      Module.new do
        java_import('does.not.Exist')
      end
    end.to raise_error(NameError, /cannot load Java class.*?does.not.Exist.*?/)
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
end

