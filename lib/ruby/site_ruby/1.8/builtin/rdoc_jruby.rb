
require 'java'
require 'rdoc/rdoc'

module JRuby
  class RDoc
    class AnnotationParser
      attr_accessor :progress
      # prepare to parse a Java class with annotations
      def initialize(top_level, clazz, options)
        @options = options
        @top_level = top_level
        @classes = Hash.new
        @progress = $stderr unless options.quiet
        @clazz = clazz
      end

      def scan
        extract_class_information
        @top_level
      end

      #######
      private
      #######

      def progress(char)
        unless @options.quiet
          @progress.print(char)
          @progress.flush
        end
      end

      def warn(msg)
        $stderr.puts
        $stderr.puts msg
        $stderr.flush
      end      

      RDocAnnotation = org.jruby.anno.RDoc.java_class
      JRubyMethodAnnotation = org.jruby.anno.JRubyMethod.java_class
      
      def handle_class_module(class_mod, annotation, enclosure)
        progress(class_mod[0, 1])

        name = annotation.name
        parent = annotation.parent
      end
      
      def extract_class_information
        a = @clazz.java_class.annotation(RDocAnnotation)
        if a
          class_mod = if a.type == "Class"
                        "class"
                      else
                        "module"
                      end
          handle_class_module(class_mod, a, @top_level)
        end
      end
    end
    
    INTERNAL_PACKAGES = %w(org.jruby.yaml org.jruby.util org.jruby.runtime org.jruby.ast org.jruby.internal org.jruby.lexer org.jruby.evaluator org.jruby.compiler)
    
    class << self
      def find_classes_from_jar(jar, package)
        file = java.util.jar.JarFile.new(jar.toString[5..-1], false)
        beginning = %r[^(#{package.empty? ? '' : (package.join("/") + "/")}.*)\.class$]

        result = []
        file.entries.each do |e|
          result << $1.gsub('/', '.') if /Invoker\$/ !~ e.to_s && beginning =~ e.to_s
        end

        result
      end

      def find_classes_from_directory(dir, package)
        raise "not implemented yet"
      end

      def find_classes_from_location(location, package)
        if /\.jar$/ =~ location.to_s
          find_classes_from_jar(location, package)
        else 
          find_classes_from_directory(location, package)
        end
      end

      
      # Executes a block inside of a context where the named method on the object in question will just return nil
      # without actually doing anything. Useful to stub out things temporarily
      def returning_nil(object, method_name) 
        singleton_object = (class << object; self; end)
        singleton_object.send :alias_method, :"#{method_name}_with_real_functionality", method_name
        singleton_object.send :define_method, method_name do |*args|
          nil
        end

        begin 
          result = yield
        ensure
          singleton_object.send(:alias_method, method_name, :"#{method_name}_with_real_functionality")
        end
        result
      end
    
      # Returns an array of TopLevel
      def extract_rdoc_information_from_classes(classes, options)
        result = []
        classes.each do |clzz|
          $stderr.printf("\n%70s: ", clzz.java_class.to_s) unless options.quiet
          tp = returning_nil(File, :stat) { ::RDoc::TopLevel.new(clzz.java_class.to_s) }
          result << AnnotationParser.new(tp, clzz, options).scan
        end
        raise "not implemented yet"
      end
      
      def install_doc(package = [])
        r = ::RDoc::RDoc.new
        
        # So parse_files should actually return an array of TopLevel objects
        (class << r; self; end).send(:define_method, :parse_files) do |options|
          location = org.jruby.Ruby.java_class.protection_domain.code_source.location

          class_names = JRuby::RDoc::find_classes_from_location(location, package)

          classes = class_names.map {|c| JavaUtilities.get_proxy_class(c) }

          JRuby::RDoc::extract_rdoc_information_from_classes(classes, options)
        end
        
        r.document(%w(--all --ri))
      end
    end
  end
end
