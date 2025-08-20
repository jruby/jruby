# @see Module.java_import
def java_import(*import_classes, &block)
  Object.send :java_import, *import_classes, &block
end
