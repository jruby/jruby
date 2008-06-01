# Fetched from http://flwrpwr.free.fr/Ruby/bench/bench27.rb on 2008-06-01

IDLE = 0
WORKER = 1
HANDLERA = 2
HANDLERB = 3
DEVICEA = 4
DEVICEB = 5

MAXTASKS = 6
$layout = 0


def main
  count = Integer(ARGV.shift || 10000)

  startTicks = Process.times.utime

  s = Scheduler.new
  s.addIdleTask(IDLE, 0, nil, count)

  wkq = Packet.new(nil, WORKER, :work)
  wkq = Packet.new(wkq, WORKER, :work)
  s.addWorkerTask(WORKER, 1000, wkq)

  wkq = Packet.new(nil, DEVICEA, :device)
  wkq = Packet.new(wkq, DEVICEA, :device)
  wkq = Packet.new(wkq, DEVICEA, :device)
  s.addHandlerTask(HANDLERA, 2000, wkq)

  wkq = Packet.new(nil, DEVICEB, :device)
  wkq = Packet.new(wkq, DEVICEB, :device)
  wkq = Packet.new(wkq, DEVICEB, :device)
  s.addHandlerTask(HANDLERB, 3000, wkq)

  s.addDeviceTask(DEVICEA, 4000, nil)
  s.addDeviceTask(DEVICEB, 5000, nil)
  s.schedule

  stopTicks = Process.times.utime
  frequency = 1000

  open('testrun-times.xml', "a") do |f|
    f.print "<ExternalStat>"
    f.print "<size>"
    f.print count
    f.print "</size>"
    f.print "<ticks>"
    f.print( ((stopTicks - startTicks)*frequency).to_i )
    f.print "</ticks>"
    f.print "<ticksPerSecond>"
    f.print frequency
    f.print "</ticksPerSecond>"
    f.print "</ExternalStat>\n"
  end

  puts "QueueCount = #{s.queueCount}\nHoldCount = #{s.holdCount}"

end


def trace(c)
  if $layout <= 0
    print "\n"
    $layout = 50
  end
  $layout -= 1
  print c
end


class Scheduler
  attr_reader :holdCount, :queueCount

  def initialize
    @table = Array.new(MAXTASKS,nil)
    @list = nil
    @queueCount = 0
    @holdCount = 0
  end

  def holdCurrent
    @holdCount += 1
    @currentTcb.held
    @currentTcb.link
  end

  def queue(packet)
    if (task = @table.at(packet.id))
      @queueCount += 1
      packet.link = nil
      packet.id = @currentId
      task.checkPriorityAdd(@currentTcb,packet)
    else
      task
    end
  end

  def release(id)
    task = @table.at(id)
    task.notHeld
    if task.pri > @currentTcb.pri
      task
    else
      @currentTcb
    end
  end

  def schedule
    @currentTcb = @list
    while @currentTcb
      if @currentTcb.isHeldOrSuspended?
        @currentTcb = @currentTcb.link
      else
        @currentId = @currentTcb.id
        # trace(@currentId + 1) #TRACE
        @currentTcb = @currentTcb.run
      end
    end
  end


  def suspendCurrent
    @currentTcb.suspended
  end


  def addDeviceTask(id,pri,wkq)
    createTcb(id,pri,wkq, DeviceTask.new(self))
  end

  def addHandlerTask(id,pri,wkq)
    createTcb(id,pri,wkq, HandlerTask.new(self))
  end

  def addIdleTask(id,pri,wkq,count)
    createRunningTcb(id,pri,wkq, IdleTask.new(self,1,count))
  end

  def addWorkerTask(id,pri,wkq)
    createTcb(id,pri,wkq, WorkerTask.new(self,HANDLERA,0))
  end

  def createRunningTcb(id,pri,wkq,task)
    createTcb(id,pri,wkq,task)
    @currentTcb.setRunning
  end

  def createTcb(id,pri,wkq,task)
    @currentTcb = Tcb.new(@list,id,pri,wkq,task)
    @list = @currentTcb
    @table[id] = @currentTcb
  end
end


class DeviceTask

  def initialize(scheduler)
    @scheduler = scheduler
  end

  def run(packet)
    if packet
      @v1 = packet
      # trace(packet.a1.chr) #TRACE
      @scheduler.holdCurrent
    else
      if @v1
        pkt = @v1
        @v1 = nil
        @scheduler.queue(pkt)
      else
        @scheduler.suspendCurrent
      end
    end
  end
end


class HandlerTask

  def initialize(scheduler)
    @scheduler = scheduler
  end

  def run(packet)
    if packet
      if packet.kind == :work
        @v1 = packet.addTo(@v1)
      else
        @v2 = packet.addTo(@v2)
      end
    end
    if @v1
      if ((count = @v1.a1)  < 4 )
        if @v2
          v = @v2
          @v2 = @v2.link
          v.a1 = @v1.a2.at(count)
          @v1.a1 = count+1
          return @scheduler.queue(v)
        end
      else
        v = @v1
        @v1 = @v1.link
        return @scheduler.queue(v)
      end
    end
    @scheduler.suspendCurrent
  end
end


class IdleTask

  def initialize(scheduler,v1,v2)
    @scheduler = scheduler
    @v1 = v1
    @v2 = v2
  end

  def run(packet)
    if ( @v2 -= 1 ).zero?
      @scheduler.holdCurrent
    else
      @scheduler.release(if (@v1 & 1).zero?
      @v1 >>=  1
      DEVICEA
    else
      @v1 >>= 1
      @v1 ^= 0xD008
      DEVICEB
    end )
  end
end
end


class WorkerTask
  ALPHA = "0ABCDEFGHIJKLMNOPQRSTUVWXYZ"

  def initialize(scheduler,v1,v2)
    @scheduler = scheduler
    @v1 = v1
    @v2 = v2
  end

  def run(packet)
    if packet
      @v1 = if ( @v1 == HANDLERA )
        HANDLERB
      else
        HANDLERA
      end
      packet.id = @v1
      packet.a1 = 0

      packet.a2.collect! {|x|
        @v2 += 1
        @v2 = 1 if @v2 > 26
        ALPHA[@v2]
      }
      @scheduler.queue(packet)
    else
      @scheduler.suspendCurrent
    end
  end
end

class Tcb
  RUNNING = 0b0   # 0
  RUNNABLE = 0b1  # 1
  SUSPENDED = 0b10  # 2
  HELD = 0b100   # 4
  SUSPENDED_RUNNABLE = SUSPENDED | RUNNABLE # 3
  NOT_HELD = ~HELD # -5

  attr_reader :link, :id, :pri

  def initialize(link, id, pri, wkq, task)
    @link = link
    @id = id
    @pri = pri
    @wkq = wkq
    @task = task
    @state = if wkq  then 0b11 else 0b10 end
      #      @state = if wkq  then SUSPENDED_RUNNABLE else SUSPENDED end
      @old = nil
    end

    def checkPriorityAdd(task,packet)
      if @wkq
        packet.addTo(@wkq)
      else
        @wkq = packet
        @state |= 0b1 # RUNNABLE
        return self if @pri > task.pri
      end
      task
    end

    def run
      @task.run(if @state == 0b11 # 3 # SUSPENDED_RUNNABLE
      @old = @wkq
      @wkq = @old.link
      @state = @wkq ? 0b1 : 0b0 # RUNNABLE : RUNNING
      @old
    end )
  end

  def setRunning
    @state = 0b0 # RUNNING
  end

  def suspended
    @state |= 0b10 # 2 # SUSPENDED
    self
  end

  def held
    @state |= 0b100 # 4 #HELD
  end

  def notHeld
    @state &= -5 # NOT_HELD
  end

  PRECOMP = (0..5).collect{|state|
    (state & 0b100 ) != 0 || state == 0b10
    #    (state & HELD) != 0 || state == SUSPENDED
  }

  def isHeldOrSuspended?
    PRECOMP.at(@state)
  end
end


class Packet

  attr_accessor :link, :id, :kind, :a1
  attr_reader :a2
  def initialize(link, id, kind)
    @link = link
    @id = id
    @kind = kind
    @a1 = 0
    @a2 = Array.new(4,0)
  end

  def addTo(queue)
    @link = nil
    unless queue
      self
    else
      nextPacket = queue
      while (peek = nextPacket.link)
        nextPacket = peek
      end
      nextPacket.link = self
      queue
    end
  end

end

main
