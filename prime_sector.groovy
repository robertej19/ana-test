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

MyMods.enable()
/////////////////

def reader = new HipoDataSource()
reader.open(args[0])

def beam = LorentzVector.withPID(11,0,0,10.6)
def target = LorentzVector.withPID(2212,0,0,0)
def hhel = new H1F("Hist_ihel","helicity",7,-2,2)

for (int i=0; i < 50; i++) {
  def event = reader.getNextEvent()

  if (event.hasBank("REC::Particle")){
    event_start_time = event.getBank("REC::Event").getFloat("startTime")
    println(event_start_time)

    }
}

reader.close()
