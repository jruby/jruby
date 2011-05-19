# actor.rb - implementation of the actor model
#
# Copyright 2007-2008  MenTaLguY <mental@rydia.net>
#
# All rights reserved.
# 
# Redistribution and use in source and binary forms, with or without 
# modification, are permitted provided that the following conditions are met:
# 
# * Redistributions of source code must retain the above copyright notice,
#   thi slist of conditions and the following disclaimer.
# * Redistributions in binary form must reproduce the above copyright notice
#   this list of conditions and the following disclaimer in the documentatio
#   and/or other materials provided with the distribution.
# * Neither the name of the Evan Phoenix nor the names of its contributors 
#   may be used to endorse or promote products derived from this software 
#   without specific prior written permission.
# 
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
# INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
# CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.

class Actor
  class DeadActorError < RuntimeError
    attr_reader :actor
    attr_reader :reason
    def initialize(actor, reason)
      super(reason)
      @actor = actor
      @reason = reason
    end
  end

  ANY = Object.new
  def ANY.===(other)
    true
  end

  class << self
    alias_method :private_new, :new
    private :private_new

    @@registered_lock = Rubinius::Channel.new
    @@registered = {}
    @@registered_lock << nil
  
    def current
      Thread.current[:__current_actor__] ||= private_new
    end

    # Spawn a new Actor that will run in its own thread
    def spawn(*args, &block)
      raise ArgumentError, "no block given" unless block
      spawned = Rubinius::Channel.new
      Thread.new do
        private_new do |actor|
          Thread.current[:__current_actor__] = actor
          spawned << actor
          block.call *args
        end
      end
      spawned.receive
    end
    alias_method :new, :spawn

    # Atomically spawn an actor and link it to the current actor
    def spawn_link(*args, &block)
      current = self.current
      link_complete = Rubinius::Channel.new
      spawn do
        begin
          Actor.link(current)
        ensure
          link_complete << Actor.current
        end
        block.call *args
      end
      link_complete.receive
    end

    # Polls for exit notifications
    def check_for_interrupt
      current._check_for_interrupt
      self
    end

    # Waits until a matching message is received in the current actor's
    # mailbox, and executes the appropriate action.  May be interrupted by
    # exit notifications.
    def receive #:yields: filter
      filter = Filter.new
      if block_given?
        yield filter
      else
        filter.when(ANY) { |m| m }
      end
      current._receive(filter)
    end

    # Send a "fake" exit notification to another actor, as if the current
    # actor had exited with +reason+
    def send_exit(recipient, reason)
      recipient.notify_exited(current, reason)
      self
    end
    
    # Link the current Actor to another one.
    def link(actor)
      current = self.current
      current.notify_link actor
      actor.notify_link current
      self
    end
    
    # Unlink the current Actor from another one
    def unlink(actor)
      current = self.current
      current.notify_unlink actor
      actor.notify_unlink current
      self
    end

    # Actors trapping exit do not die when an error occurs in an Actor they
    # are linked to.  Instead the exit message is sent to their regular
    # mailbox in the form [:exit, actor, reason].  This allows certain
    # Actors to supervise sets of others and restart them in the event
    # of an error.  Setting the trap flag may be interrupted by pending
    # exit notifications.
    #
    def trap_exit=(value)
      current._trap_exit = value
      self
    end

    # Is the Actor trapping exit?
    def trap_exit
      current._trap_exit
    end
    alias_method :trap_exit?, :trap_exit

    # Lookup a locally named service
    def lookup(name)
      raise ArgumentError, "name must be a symbol" unless Symbol === name
      @@registered_lock.receive
      begin
        @@registered[name]
      ensure
        @@registered_lock << nil
      end
    end
    alias_method :[], :lookup

    # Register an Actor locally as a named service
    def register(name, actor)
      raise ArgumentError, "name must be a symbol" unless Symbol === name
      unless actor.nil? or actor.is_a?(Actor)
        raise ArgumentError, "only actors may be registered"
      end

      @@registered_lock.receive
      begin
        if actor.nil?
          @@registered.delete(name)
        else
          @@registered[name] = actor
        end
      ensure
        @@registered_lock << nil
      end
    end
    alias_method :[]=, :register

    def _unregister(actor) #:nodoc:
      @@registered_lock.receive
      begin
        @@registered.delete_if { |n, a| actor.equal? a }
      ensure
        @@registered_lock << nil
      end
    end
  end

  def initialize
    @lock = Rubinius::Channel.new

    @filter = nil
    @ready = Rubinius::Channel.new
    @action = nil
    @message = nil

    @mailbox = []
    @interrupts = []
    @links = []
    @alive = true
    @exit_reason = nil
    @trap_exit = false
    @thread = Thread.current

    @lock << nil

    if block_given?
      watchdog { yield self }
    else
      Thread.new { watchdog { @thread.join } }
    end
  end

  def send(message)
    @lock.receive
    begin
      return self unless @alive
      if @filter
        @action = @filter.action_for(message)
        if @action
          @filter = nil
          @message = message
          @ready << nil
        else
          @mailbox << message
        end
      else
        @mailbox << message
      end
    ensure
      @lock << nil
    end
    self
  end
  alias_method :<<, :send

  def _check_for_interrupt #:nodoc:
    check_thread
    @lock.receive
    begin
      raise @interrupts.shift unless @interrupts.empty?
    ensure
      @lock << nil
    end
  end

  def _receive(filter) #:nodoc:
    check_thread

    action = nil
    message = nil
    timed_out = false

    @lock.receive
    begin
      raise @interrupts.shift unless @interrupts.empty?

      for i in 0...(@mailbox.size)
        message = @mailbox[i]
        action = filter.action_for(message)
        if action
          @mailbox.delete_at(i)
          break
        end
      end

      unless action
        @filter = filter
        @lock << nil
        begin
          if filter.timeout?
            timed_out = @ready.receive_timeout(filter.timeout) == false
          else
            @ready.receive
          end
        ensure
          @lock.receive
        end

        if !timed_out and @interrupts.empty?
          action = @action
          message = @message
        else
          @mailbox << @message if @action
        end

        @action = nil
        @message = nil

        raise @interrupts.shift unless @interrupts.empty?
      end
    ensure
      @lock << nil
    end

    if timed_out
      filter.timeout_action.call
    else
      action.call message
    end
  end
 
  # Notify this actor that it's now linked to the given one; this is not
  # intended to be used directly except by actor implementations.  Most
  # users will want to use Actor.link instead.
  #
  def notify_link(actor)
    @lock.receive
    alive = nil
    exit_reason = nil
    begin
      alive = @alive
      exit_reason = @exit_reason
      @links << actor if alive and not @links.include? actor
    ensure
      @lock << nil
    end
    actor.notify_exited(self, exit_reason) unless alive
    self
  end
  
  # Notify this actor that it's now unlinked from the given one; this is
  # not intended to be used directly except by actor implementations.  Most
  # users will want to use Actor.unlink instead.
  #
  def notify_unlink(actor)
    @lock.receive
    begin
      return self unless @alive
      @links.delete(actor)
    ensure
      @lock << nil
    end
    self
  end
  
  # Notify this actor that one of the Actors it's linked to has exited;
  # this is not intended to be used directly except by actor implementations.
  # Most users will want to use Actor.send_exit instead.
  #
  def notify_exited(actor, reason)
    to_send = nil
    @lock.receive
    begin
      return self unless @alive
      @links.delete(actor)
      ex = DeadActorError.new(actor, reason)
      if @trap_exit
        to_send = ex
      elsif reason
        @interrupts << ex
        if @filter
          @filter = nil
          @ready << nil
        end
      end
    ensure
      @lock << nil
    end
    send to_send if to_send
    self
  end

  def watchdog
    reason = nil
    begin
      yield
    rescue Exception => reason
    ensure
      links = nil
      Actor._unregister(self)
      @lock.receive
      begin
        @alive = false
        @mailbox = nil
        @interrupts = nil
        @exit_reason = reason
        links = @links
        @links = nil
      ensure
        @lock << nil
      end
      links.each do |actor|
        begin
          actor.notify_exited(self, reason)
        rescue Exception
        end
      end
    end
  end
  private :watchdog

  def check_thread
    unless Thread.current == @thread
      raise ThreadError, "illegal cross-actor call"
    end
  end
  private :check_thread
  
  def _trap_exit=(value) #:nodoc:
    check_thread
    @lock.receive
    begin
      raise @interrupts.shift unless @interrupts.empty?
      @trap_exit = !!value
    ensure
      @lock << nil
    end
  end
  
  def _trap_exit #:nodoc:
    check_thread
    @lock.receive
    begin
      @trap_exit
    ensure
      @lock << nil
    end
  end
end

require 'actor/filter'
