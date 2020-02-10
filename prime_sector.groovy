#!/usr/bin/groovy

import org.jlab.io.hipo.HipoDataSource
import org.jlab.io.hipo.HipoDataSync
import org.jlab.detector.base.DetectorType
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

def reader = new HipoDataSource()
reader.open(args[0])

def beam = LorentzVector.withPID(11,0,0,10.6)
def target = LorentzVector.withPID(2212,0,0,0)
def hhel = new H1F("Hist_ihel","helicity",7,-2,2)

for (int i=0; i < 50; i++) {
  def event = reader.getNextEvent()

  if (event.hasBank("REC::Particle")){

    def event_scint = event.getBank("REC::Scintillator")

    def pind_sarray = event_scint.getShort('pindex')*.toInteger()
    def sect_sarray_l = event_scint.getByte('layer')

    def sect_sarray = event_scint.getByte('sector')
    def stati = event.getBank("REC::Particle").getInt('status')

    def secs = [event_scint.getShort('pindex')*.toInteger(), event_scint.getByte('sector')].transpose().collectEntries()


    def sec_scint = event_scint.getFloat('time')

    def dete = event_scint.getInt('detector')

    event_start_time = event.getBank("REC::Event").getFloat("startTime")
    println(event_start_time)

    }
}

reader.close()
