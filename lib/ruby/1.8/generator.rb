# Because generator is needed by Enumerator in 1.8.7 mode, we moved generator
# logic to src/bultin/generator_internal.rb and require that here and from
# within JRuby, so that Enumerator works even without stdlib present.
require 'generator_internal'