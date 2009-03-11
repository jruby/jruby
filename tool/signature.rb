class Class
  def signature(name, signature)
    name = name.to_s
    signatures[name] = signature
  end

  def signatures
    @signatures ||= {}
  end
end