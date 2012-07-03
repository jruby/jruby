require 'rspec'
require 'java'

describe "JRUBY-6679: Encoding.default_external" do
  it "should count in Windows console code page" do
    if java.lang.System.getProperty('os.name').include? 'Windows'
      enc_ext = Encoding.default_external

      # unfortunately `cmd /c chcp` doesn't work
      console = java.lang.System.console
      fcs = java.io.Console.java_class.declared_field 'cs'
      fcs.accessible = true
      cs = fcs.value console

      bl = org.jruby.util.ByteList.create cs.name
      con_enc = org.jruby.Ruby.globalRuntime.encodingService.loadEncoding bl

      if enc_ext.to_s != 'UTF-8'
        enc_ext.to_s.should == con_enc.to_s
        #enc_ext.should == con_enc
      end
    end
  end
end
