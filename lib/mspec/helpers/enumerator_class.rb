class Object
  def enumerator_class
    MSpec.deprecate "enumerator_class", "Enumerator"
    Enumerator
  end
end
