require 'chunky_png'
require 'oily_png'

abort unless OilyPNG::Color.r(0x11223344) == ChunkyPNG::Color.r(0x11223344)
