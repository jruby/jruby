require 'test/minirunit'
require 'rexml/document'
  
SOME_TAG = <<SOME
<some_tag some_attr="foo">
</some_tag>
SOME
XML_FILE = <<END_OF_FILE
<another_tag>
#{SOME_TAG}
</another_tag>
END_OF_FILE

doc = REXML::Document.new(XML_FILE) 
test_ok(doc)