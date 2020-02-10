#!/usr/bin/groovy

import org.jlab.detector.base.DetectorType
import org.jlab.clas.physics.LorentzVector
import org.jlab.clas.physics.Vector3
import org.jlab.groot.data.H1F
import org.jlab.groot.data.H2F
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.nio.ByteBuffer


class ep_test {
  def hists = new ConcurrentHashMap()

  def beam = LorentzVector.withPID(11,0,0,10.6)
  def target = LorentzVector.withPID(2212,0,0,0)

  def hw = {new H1F("$it","$it",200,0,5)}
  def hq2 = {new H1F("$it","$it",200,0,10)}

   def banknames = ['REC::Event','REC::Particle','REC::Cherenkov','REC::Calorimeter','REC::Traj','REC::Track','REC::Scintillator']
  def processEvent(event) {
    if(banknames.every{event.hasBank(it)}) {
      def (evb,partb,cc,ec,traj,trck,scib) = banknames.collect{event.getBank(it)}
      def banks = [cc:cc,ec:ec,part:partb,traj:traj,trck:trck]
      def ihel = evb.getByte('helicity',0)

      println "ihel is "+ihel
      return ihel
}
}
}
