require "java"

include_class "java.io.FileOutputStream"
include_class "javax.xml.transform.TransformerFactory"
include_class ["StreamSource", "StreamResult"].map {|e| "javax.xml.transform.stream." + e}

if (ARGV.length < 3) 
  puts "Usage: xslt {xml} {xslt} {result}"
  exit
end

document =   StreamSource.new(ARGV[0])
stylesheet = StreamSource.new(ARGV[1])
result =     StreamResult.new(FileOutputStream.new(ARGV[2]))

transformer = TransformerFactory.newInstance.newTransformer(stylesheet)
transformer.transform(document, result)

