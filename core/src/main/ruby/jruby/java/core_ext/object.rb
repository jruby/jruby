class Object
  # :nodoc:
  # @deprecated
  def java_import(*import_classes, &block)
    warn "calling java_import on a non-Module object is deprecated"
    Object.send :java_import, *import_classes, &block
  end
  private :java_import

  alias :import :java_import unless respond_to?(:import)

end

# @see Module.java_import
def java_import(*import_classes, &block)
  Object.send :java_import, *import_classes, &block
end
