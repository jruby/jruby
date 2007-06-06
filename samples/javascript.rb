# This example demonstrates language interoperability and also JRubys' java integration features.
require 'samples/scripting'
include Scriptable

x = 'Hello'
y = 'world'

javascript <<JS
z = 'multilanguage'
println(x + ', ' + z + ' ' + y)
JS
