require 'test/minirunit'
test_check "Test string#split stress test:"

# this test tried and failed to cause out of memory errors on the 0.9.0 release
mess="A @B @C @D @E @F @G @H @I @J @K @L @M @N @O @P @Q @R @S @T @U @V @W @X @Y @Z @a @b @c @d @e @f @g @h @i @h @k @l @m @n @o @p @q @r @s @t @u @v @w @x @y @z @0 @1 @2 @3 @4 @5 @6 @7 @8 @9 @! @+ @" #64
mess << mess 
mess << mess 
mess << mess 
mess << mess 
mess << mess 
mess << mess 
mess << mess 
mess << mess 
mess << mess 

# puts mess.size
result = mess.split(/( )(@)/)[-3]
test_equal("+", result)



