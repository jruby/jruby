class Object
  # :nodoc:
  # @deprecated
  unless respond_to?(:import)
    def import(*import_classes, &block)
      warn "import is deprecated; use java_import", uplevel: 1
      Object.send :java_import, *import_classes, &block
    end
    private :import
  end

end

# @see Module.java_import
def java_import(*import_classes, &block)
  Object.send :java_import, *import_classes, &block
end
