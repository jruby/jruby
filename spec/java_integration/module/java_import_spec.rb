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
    end.to raise_error(NameError, /cannot import Java class .*?InnerClasses\$lowerInnerClass.*?: bad constant name lowerInnerClass/)
  end

  it "imports upper-case names successfully" do
    mod = nil
    expect do
      mod = Module.new { java_import InnerClasses::CapsInnerClass }
    end.not_to raise_error
    expect(mod::CapsInnerClass).to eq(InnerClasses::CapsInnerClass)
  end

  class Object
    java_import 'javax.lang.model.element.Element'
  end

  module JavaImportSpec
    java_import 'javax.lang.model.element.Element'
    module Nested
      java_import org.w3c.dom.Element
      module Nested2
        java_import 'org.w3c.dom.Element'
      end
    end if Element
    module NestedAgain
      java_import 'javax.lang.model.element.Element'
    end
  end

  it "sets constant into proper scope (when parent has same name)" do
    Object.send(:remove_const, :Element)
    expect(JavaImportSpec::Element).to be(javax.lang.model.element.Element) # used to fail in JRuby <= 9.4.5

    expect(JavaImportSpec::Nested::Element).to be(org.w3c.dom.Element)

    expect(JavaImportSpec::Nested::Nested2.constants).to eql [:Element]
    expect(JavaImportSpec::NestedAgain.constants).to eql [:Element]

    expect(JavaImportSpec::Nested::Nested2::Element).to be(org.w3c.dom.Element)
    expect(JavaImportSpec::NestedAgain::Element).to be(javax.lang.model.element.Element)
  end

end

