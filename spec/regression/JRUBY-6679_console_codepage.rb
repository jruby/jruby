require 'rspec'
require 'java'

describe "JRUBY-6679: Encoding.default_external" do
  it "should count in Windows console code page" do
    if java.lang.System.getProperty('os.name').include? 'Windows'
      enc_ext = Encoding.default_external.name
      codepage = `cmd /c chcp`.split.last
      enc_ext.include?(codepage).should == true
    end
  end
end
