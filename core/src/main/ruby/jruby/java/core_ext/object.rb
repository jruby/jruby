class Object
  # include the class specified by +include_class+ into the current namespace,
  # using either its base name or by using a name returned from an optional block,
  # passing all specified classes in turn and providing the block package name
  # and base class name.
  # @deprecated use {Object#java_import}
  def include_class(include_class, &block)
    warn "#{__method__} is deprecated. Use java_import."
    java_import(include_class, &block)
  end
  private :include_class

  # @deprecated
  def java_kind_of?(other) # TODO: this can go away now, but people may be using it
    return true if self.kind_of?(other)
    return false unless self.respond_to?(:java_class) && other.respond_to?(:java_class) &&
      other.kind_of?(Module) && !self.kind_of?(Module) 
    return other.java_class.assignable_from?(self.java_class)
  end

  # Import one or many Java classes as follows:
  #
  #   java_import java.lang.System
  #   java_import java.lang.System, java.lang.Thread
  #   java_import [java.lang.System, java.lang.Thread]
  #
  # @!visibility public
  def java_import(*import_classes, &block)
    warn "calling java_import on a non-Module object is deprecated"
    Object.send :java_import, *import_classes, &block
  end
  private :java_import

  alias :import :java_import unless respond_to?(:import)

end

# toplevel java_import is ok
def java_import(*import_classes, &block)
  Object.send :java_import, *import_classes, &block
end
