require File.dirname(__FILE__) + "/../spec_helper"
require "kotlin_fixtures.jar"

java_import "java_integration.fixtures.CompanionObject"
java_import "java_integration.fixtures.NamedCompanionObject"

describe "Kotlin Companion objects" do
  describe "unnamed companion objects" do
    it "exposes constants on the parent class" do
      expect(CompanionObject::COMPANION_CONST).to eq("Constant")
    end

    it "exposes values on the companion object" do
      expect(CompanionObject::Companion.value).to eq("Value")
    end

    it "exposes @JvmStatic values on the parent class" do
      expect(CompanionObject.jvm_static_value).to eq("JvmStaticValue")
    end

    it "exposes @JvmStatic values on the companion object" do
      expect(CompanionObject::Companion.jvm_static_value).to eq("JvmStaticValue")
    end

    it "exposes non-@JvmStatic methods on the companion object" do
      expect(CompanionObject::Companion.companion_method).to eq("Method")
    end

    it "does not expose non-@JvmStatic methods on the companion object" do
      expect do
        CompanionObject.companion_method
      end.to raise_error(NameError)
    end

    it "exposes @JvmStatic methods on the parent class" do
      expect(CompanionObject.jvmStaticMethod).to eq("JvmStaticMethod")
    end

    it "exposes @JvmStatic methods on the companion object" do
      expect(CompanionObject::Companion.jvmStaticMethod).to eq("JvmStaticMethod")
    end
  end

  describe "named companion objects" do
    it "exposes constants on the parent class" do
      expect(NamedCompanionObject::COMPANION_CONST).to eq("Constant")
    end

    it "exposes values on the companion object" do
      expect(NamedCompanionObject::Factory.value).to eq("Value")
    end

    it "exposes @JvmStatic values on the parent class" do
      expect(NamedCompanionObject.jvm_static_value).to eq("JvmStaticValue")
    end

    it "exposes @JvmStatic values on the companion object" do
      expect(NamedCompanionObject::Factory.jvm_static_value).to eq("JvmStaticValue")
    end

    it "exposes non-@JvmStatic methods on the companion object" do
      expect(NamedCompanionObject::Factory.companion_method).to eq("Method")
    end

    it "does not expose non-@JvmStatic methods on the companion object" do
      expect do
        NamedCompanionObject.companion_method
      end.to raise_error(NameError)
    end

    it "exposes @JvmStatic methods on the parent class" do
      expect(NamedCompanionObject.jvmStaticMethod).to eq("JvmStaticMethod")
    end

    it "exposes @JvmStatic methods on the companion object" do
      expect(NamedCompanionObject::Factory.jvmStaticMethod).to eq("JvmStaticMethod")
    end
  end
end
