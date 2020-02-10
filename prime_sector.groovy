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
import java.io.*
import java.util.*
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.groot.group.DataGroup
import org.jlab.groot.data.H1F
import org.jlab.groot.data.H2F
import org.jlab.groot.math.F1D
import org.jlab.groot.fitter.DataFitter
import org.jlab.io.base.DataBank
import org.jlab.io.base.DataEvent
import org.jlab.io.hipo.HipoDataSource
import org.jlab.io.hipo.HipoDataSync
import org.jlab.detector.base.DetectorType
import org.jlab.clas.physics.Vector3
import org.jlab.clas.physics.LorentzVector
import org.jlab.groot.base.GStyle
import org.jlab.groot.graphics.EmbeddedCanvas
import java.text.SimpleDateFormat
import java.time.Instant

MyMods.enable()
/////////////////

def reader = new HipoDataSource()
reader.open(args[0])

def beam = LorentzVector.withPID(11,0,0,10.6)
def target = LorentzVector.withPID(2212,0,0,0)
def hhel = new H1F("Hist_ihel","helicity",7,-2,2)



def processEvent(event,hhel) {
	def banknames = ['REC::Event','REC::Particle','REC::Cherenkov','REC::Calorimeter','REC::Traj','REC::Track','REC::Scintillator']

	    if(banknames.every{event.hasBank(it)}) {
		    def (evb,partb,cc,ec,traj,trck,scib) = banknames.collect{event.getBank(it)}
		    def banks = [cc:cc,ec:ec,part:partb,traj:traj,trck:trck]
		    def ihel = evb.getByte('helicity',0)
		   println "ihel is "+ihel

		   hhel.fill(ihel)

	}

}


for (int i=0; i < 50; i++) {
  def event = reader.getNextEvent()
  processEvent(event,hhel)
}

reader.close()

def run = "testrun5036"
TDirectory out = new TDirectory()
out.mkdir('/'+run)
out.cd('/'+run)

out.addDataSet(hhel)

out.writeFile(run+'.hipo')
