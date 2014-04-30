# Derived from http://pws.prserv.net/dlissett/ben/bench1.htm
# Licensed CC BY-NC-SA 1.0

# Adapted by Stefan Marr, and then by Chris Seaton

IDLE = 0  
WORKER = 1  
HANDLERA = 2  
HANDLERB = 3  
DEVICEA = 4  
DEVICEB = 5 

MAXTASKS = 6
$layout = 0


def run(iterations)
   s = Scheduler.new 
   for i in 0..iterations
     s.reset
     s.addIdleTask(IDLE, 0, nil, 10000)

     wkq = Packet.new(nil, WORKER, Packet::WORK)
     wkq = Packet.new(wkq, WORKER, Packet::WORK)   
     s.addWorkerTask(WORKER, 1000, wkq)

     wkq = Packet.new(nil, DEVICEA, Packet::DEVICE)
     wkq = Packet.new(wkq, DEVICEA, Packet::DEVICE)  
     wkq = Packet.new(wkq, DEVICEA, Packet::DEVICE)       
     s.addHandlerTask(HANDLERA, 2000, wkq)   

     wkq = Packet.new(nil, DEVICEB, Packet::DEVICE)
     wkq = Packet.new(wkq, DEVICEB, Packet::DEVICE)  
     wkq = Packet.new(wkq, DEVICEB, Packet::DEVICE) 
     s.addHandlerTask(HANDLERB, 3000, wkq)  

     s.addDeviceTask(DEVICEA, 4000, nil) 
     s.addDeviceTask(DEVICEB, 5000, nil)           
     s.schedule
     
     if s.holdCount != 9297 or s.queueCount != 23246
       return false
     end
   end
   return true
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
   attr_reader :queueCount, :holdCount

   def initialize()
      @table = Array.new(MAXTASKS,nil)
      @list = nil 
      @queueCount = 0
      @holdCount = 0
   end   
   
   def reset()
      @queueCount = 0
      @holdCount = 0
   end
   
   def holdCurrent
      @holdCount += 1
      @currentTcb.held
      @currentTcb.link
   end

   def queue(packet)
      task = @table.at(packet.id)
      if task # not nil
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
   
   def schedule()
      @currentTcb = @list
      while @currentTcb # not nil        
         if @currentTcb.isHeldOrSuspended?     
            @currentTcb = @currentTcb.link
         else           
            @currentId = @currentTcb.id                     
            # trace(@currentId + 1) #TRACE         
            @currentTcb = @currentTcb.run
         end           
      end                  
   end
   
   def suspendCurrent()
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
      if packet # not nil
         @v1 = packet       
         # trace(packet.a1.chr) #TRACE       
         @scheduler.holdCurrent                                    
      else
         if @v1 # not nil
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
      if packet # not nil    
         if packet.kind == Packet::WORK
            @v1 = packet.addTo(@v1)             
         else
            @v2 = packet.addTo(@v2)                    
         end   
      end   
      if @v1 # not nil               
         if (count = @v1.a1) < 4
            if @v2 # not nil
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
         if (@v1 & 1).zero? 
            @v1 >>= 1            
            @scheduler.release(DEVICEA) 
         else
            @v1 >>= 1 
            @v1 ^= 0xD008
            @scheduler.release(DEVICEB)          
         end                  
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
      if packet # not nil
         @v1 = 
            if @v1 == HANDLERA 
               HANDLERB
            else
               HANDLERA
            end      
         packet.id = @v1
         packet.a1 = 0
                  
         packet.a2.collect! {|x|
            @v2 += 1
            @v2 = 1 if @v2 > 26 
            # FIXME(CS)
            if @v2 == 27
              puts "error"
              exit
            end
            ALPHA[@v2]
            }                  
         @scheduler.queue(packet) 
      else
         @scheduler.suspendCurrent         
      end      
   end
end


class Tcb
   RUNNING = 0b0   
   RUNNABLE = 0b1 
   SUSPENDED = 0b10   
   HELD = 0b100   
   SUSPENDED_RUNNABLE = SUSPENDED | RUNNABLE
   NOT_HELD = ~HELD 

   attr_reader :link, :id, :pri

   def initialize(link, id, pri, wkq, task)
      @link = link
      @id = id
      @pri = pri
      @wkq = wkq     
      @task = task  
      # SUSPENDED_RUNNABLE else SUSPENDED       
      @state = if wkq then 0b11 else 0b10 end                        
   end                 
          
   def checkPriorityAdd(task,packet)
      if @wkq # not nil
         packet.addTo(@wkq)      
      else      
         @wkq = packet
         @state |= 0b1 # RUNNABLE      
         return self if @pri > task.pri         
      end      
      task
   end
           
   def run 
      if @state == 0b11 # SUSPENDED_RUNNABLE   
         packet = @wkq
         @wkq = packet.link         
         @state = @wkq ? 0b1 : 0b0 # RUNNABLE : RUNNING
      else
         packet = nil
      end   
      @task.run(packet)     
   end         
   
   def setRunning
      @state = 0b0 # RUNNING
   end
   
   def suspended
      @state |= 0b10 # SUSPENDED
      self      
   end      
   
   def held
      @state |= 0b100 # HELD
   end   
   
   def notHeld
      @state &= ~0b100 # NOT_HELD
   end     
   
   def isHeldOrSuspended?
      #(@state & HELD) != 0 || @state == SUSPENDED
      (@state & 0b100) != 0 || @state == 0b10
   end        
end 


class Packet
   DEVICE = 0
   WORK = 1

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

def warmup
  1_000.times do
    run(1)
  end
end

def sample
  run(100)
end

def name
  return "richards"
end
