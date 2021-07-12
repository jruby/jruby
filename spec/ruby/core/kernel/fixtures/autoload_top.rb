autoload(:KSAutoloadB, "#{__dir__}/autoload_b.rb")
puts Thread.new { KSAutoloadB.loaded }.join.value
