class Enumerable::Enumerator
  def __generator
    @generator ||= __choose_generator
  end

  def __choose_generator
    iter_for_method = :"iter_for_#{@__method__}"
    if ENV_JAVA['jruby.enumerator.lightweight'] != 'false' &&
        @__object__.respond_to?(iter_for_method)
      @__object__.send iter_for_method
    else
      Generator::Threaded.new(self)
    end
  end
  private :__generator, :__choose_generator

  # call-seq:
  #   e.next   => object
  #
  # Returns the next object in the enumerator, and move the internal
  # position forward.  When the position reached at the end,
  # StopIteration is raised until the enumerator is rewound.
  #
  # Note that enumeration sequence by next method does not affect other
  # non-external enumeration methods, unless underlying iteration
  # methods itself has side-effect, e.g. IO#each_line.
  def next
    g = __generator
    begin
      g.next
    rescue EOFError
      raise StopIteration, 'iteration reached at end'
    end
  end
  
  def rewind
    __generator.rewind
    self
  end
end