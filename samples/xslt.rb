require "java"

module JavaIO
  include_package "java.io"
end

module XSLT
  include_package "javax.xml.transform"
  include_package "javax.xml.transform.stream"

  java_alias :Source, :StreamSource
  java_alias :Result, :StreamResult
end

if (ARGV.length < 3) 
  puts "Usage: xslt {xml} {xslt} {result}"
  exit
end

document =   XSLT::Source.new(ARGV[0])
stylesheet = XSLT::Source.new(ARGV[1])
result =     XSLT::Result.new(JavaIO::FileOutputStream.new(ARGV[2]))

transformer = XSLT::TransformerFactory.newInstance.newTransformer(stylesheet)
transformer.transform(document, result);

