# Load the built-in set library
JRuby::Util.load_ext("org.jruby.ext.set.SetLibrary")

autoload :SortedSet, "#{__dir__}/set/sorted_set"
