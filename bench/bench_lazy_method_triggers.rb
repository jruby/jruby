class Foo
  def uno
    tres if respond_to?(:tres)
  end

  def dos
    tres
  rescue NameError
  end

  def none
    tres
  end

  def tres
    3
  end

  def method_missing(meth, *args)
  end
end

require "rubygems"
require "rbench"

FOO = Foo.new

(ARGV[0] || 1).to_i.times do
RBench.run(1_000_000) do
  report("respond_to?") do
    FOO.uno
  end
  report("blind run") do
    FOO.none
  end
  report("blind run and rescue") do
    FOO.dos
  end
end
end

