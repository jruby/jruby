class MethodVisibility

  private

  def priv_method; false end

  def publ_method; true end
  public :publ_method

  def prot_method; end
  protected :prot_method

  public

  def hello(arg = there)
    "Hello #{arg}"
  end

  def there
    @there ||= 'World!'
  end
  private :there

  protected

  java_signature 'public int protMethodWithJSign(String)'
  def prot_method_with_java_signature(str_arg)
    return str_arg.size
  end

end
