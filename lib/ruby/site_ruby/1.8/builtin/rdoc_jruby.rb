
require 'java'
require 'rdoc/rdoc'

module JRuby
  class RDoc
    class << self
      def install_doc(package = [])
        r = ::RDoc::RDoc.new
        (class << r; self; end).send(:define_method, :parse_files) do |options|
          location = org.jruby.Ruby.java_class.protection_domain.code_source.location

          class_names = JRuby::RDoc::find_classes_from_location(location, package)

          classes = class_names.map {|c| JavaUtilities.get_proxy_class(c) }

          JRuby::Rdoc::extract_rdoc_information_from_classes(classes, options)
        end
        
        r.document(%w(--all --ri))
      end
    end
  end
end
