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
    era = SpecVersion.new(SpecGuard.ruby_version) < "1.9"
    convert = era ? :to_s : :to_sym

    one = one.send convert
    if rest.empty?
      one
    else
      [one].concat rest.map { |x| x.send convert }
    end
  end
end
