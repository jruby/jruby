module ObjectSpaceFixtures
  def self.garbage
    blah
  end

  def self.blah
    o = "hello"
    @garbage_objid = o.object_id
    return o
  end

  @last_objid = nil

  def self.last_objid
    @last_objid
  end

  def self.garbage_objid
    @garbage_objid
  end

  def self.make_finalizer
    proc { |obj_id| @last_objid = obj_id }
  end

  def self.define_finalizer
    handler = lambda { |obj| ScratchPad.record :finalized }
    ObjectSpace.define_finalizer "#{rand 5}", handler
  end

  def self.scoped(wr)
    return Proc.new { wr.write "finalized"; wr.close }
  end

end
