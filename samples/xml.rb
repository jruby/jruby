#
# This example transform a xml file with a xslt stylesheet
# to an output xml.
#

# Load the Java classes.

JavaObject.load_class "java.io.File"
JavaObject.load_class "javax.xml.transform.stream.StreamResult"
JavaObject.load_class "javax.xml.transform.stream.StreamSource"
JavaObject.load_class "javax.xml.transform.Transformer"
JavaObject.load_class "javax.xml.transform.TransformerFactory"

$xml = "./samples/birds.xml"
$xslt = "./samples/birds.xsl"
$ouput = "./samples/birds.html"

xml_file = File.new $xml
xslt_file = File.new $xslt
output_file = File.new $output

xml_source = StreamSource.new xml_file
xslt_source = StreamSource.new xslt_file
output_result = StreamResult.new output_file

factory = TransformerFactory.newInstance

transformer = factory.newTransformer xslt_source
transformer.transform xml_source, output_result

puts "XML file transformed."