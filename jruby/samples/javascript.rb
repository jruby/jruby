require 'samples/scripting'
include Scriptable

x = 'Hello'
y = 'world'

javascript <<JS
z = 'multilanguage'
println(x + ', ' + z + ' ' + y)
JS
