require 'mspec/utils/version'
require 'mspec/guards/guard'

class Object
  # Accepts either a single argument or an Array of arguments. If RUBY_VERSION
  # is less than 1.9, converts the argument(s) to Strings; otherwise, converts
  # the argument(s) to Symbols.
  #
  # If one argument is passed, the converted argument is returned. If an Array
  # is passed, an Array is returned.
  #
  # For example, if RUBY_VERSION == 1.8.7
  #
  #   stasy(:some) => "some"
  #   stasy("nom") => "nom"
  #
  # while if RUBY_VERSION == 1.9.0
  #
  #   stasy(:some) => :some
  #   stasy("nom") => :nom

  def stasy(one, *rest)
    MSpec.deprecate "stasy", "a Symbol literal"
    one = one.send :to_sym
    if rest.empty?
      one
    else
      [one].concat rest.map { |x| x.send :to_sym }
    end
  end
end
