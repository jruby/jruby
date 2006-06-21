require 'java'
require 'optparse'
require 'ostruct'

include_class "java.lang.System"
include_class("java.lang.Exception") {|p,n| "J" + n }
include_class "java.io.FileOutputStream"
include_class "javax.xml.transform.TransformerFactory"
include_class ["StreamSource", "StreamResult"].map {|e| "javax.xml.transform.stream." + e}

class XSLTOptions
  def self.parse(args)
    options = OpenStruct.new
    options.parameters = {}
       
    opts = OptionParser.new do |opts|
      opts.banner = "Usage: [options] xslt {xml} {xslt} [{result}]"
      opts.separator ""
      opts.separator "Specific options:"
      
      opts.on("-p", "--parameters name=value,name1=value1", Array) do |n|
	n.collect do |v| 
	  name, value = v.split(/\s*=\s*/)
	  options.parameters[name] = value
	end
      end

    end  
    opts.parse!(args)
    options
  end
end

options = XSLTOptions.parse(ARGV)

if (ARGV.length < 2 || ARGV.length > 3) 
  puts "Usage: xslt {xml} {xslt} [{result}]"
  exit
end

document =   StreamSource.new(ARGV[0])
stylesheet = StreamSource.new(ARGV[1])
output =     ARGV.length == 2 ? System::out : FileOutputStream.new(ARGV[2])
result =     StreamResult.new(output)

begin
  transformer = TransformerFactory.newInstance.newTransformer(stylesheet)
  options.parameters.each {|name, value| transformer.setParameter name, value }
  transformer.transform(document, result)
rescue Exception => e
  puts e
end
