require 'java'

describe "An object wanting to import Java classes" do
  it "should explode when trying to import a non-existant Java class" do
    expect{ java_import 'does.not.Exist' }.to raise_error(NameError)
  end

  it "should receive an array with the imported classes" do
    expect(java_import()).to eq([])
    expect(java_import('java.util.Hashtable')).to eq([java.util.Hashtable])

    imported_classes = java_import('java.util.Hashtable', java.util.Hashtable, Java::java.util.Hashtable, Java::JavaUtil::Hashtable)
    expect(imported_classes).to eq([java.util.Hashtable] * 4)
  end
end

