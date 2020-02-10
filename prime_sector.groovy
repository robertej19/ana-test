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
def hhel = new H1F("Hist_ihel","helicity",7,-2,2)
def hphi = new H1F("Hist_phi","Phi Distribution",2500,-10,370)
def hq2 = new H1F("Hist_q2","Q^2 Distribution",1000,0,12)

def processEvent(event,hhel,hphi,hq2) {
	def beam = LorentzVector.withPID(11,0,0,10.6)
	def target = LorentzVector.withPID(2212,0,0,0)

	def banknames = ['REC::Event','REC::Particle','REC::Cherenkov','REC::Calorimeter','REC::Traj','REC::Track','REC::Scintillator']

	    if(banknames.every{event.hasBank(it)}) {
		    def (evb,partb,cc,ec,traj,trck,scib) = banknames.collect{event.getBank(it)}
		    def banks = [cc:cc,ec:ec,part:partb,traj:traj,trck:trck]
		    def ihel = evb.getByte('helicity',0)
		   //println "ihel is "+ihel



		   def index_of_electrons_and_protons = (0..<partb.rows()).findAll{partb.getInt('pid',it)==11 && partb.getShort('status',it)<0}
			   .collectMany{iele->(0..<partb.rows()).findAll{partb.getInt('pid',it)==2212}.collect{ipro->[iele,ipro]}
		   }
		   //println "index_of_electrons_and_protons "+index_of_electrons_and_protons

		   def index_of_pions = (0..<partb.rows()-1).findAll{partb.getInt('pid',it)==22 && partb.getShort('status',it)>=2000}
			   .findAll{ig1->'xyz'.collect{partb.getFloat("p$it",ig1)**2}.sum()>0.16}
			   .collectMany{ig1->
			   (ig1+1..<partb.rows()).findAll{partb.getInt('pid',it)==22 && partb.getShort('status',it)>=2000}
			   .findAll{ig2->'xyz'.collect{partb.getFloat("p$it",ig2)**2}.sum()>0.16}
			   .collect{ig2->[ig1,ig2]}
		   }
		   //println "index of pions is " + index_of_pions

		   def isep0s = index_of_electrons_and_protons.findAll{iele,ipro->
			   def ele = LorentzVector.withPID(11,*['px','py','pz'].collect{partb.getFloat(it,iele)})
			   def pro = LorentzVector.withPID(2212,*['px','py','pz'].collect{partb.getFloat(it,ipro)})
			   //println "first electron is"+ele
			   if(event.hasBank("MC::Particle")) {
				   //println "Event has MC Particle bank!"
				   def mcb = event.getBank("MC::Particle")
				   def mfac = (partb.getShort('status',ipro)/1000).toInteger()==2 ? 3.2 : 2.5

				   def profac = 0.9

				   //mfac=1
				   profac = 1.0

				   ele = LorentzVector.withPID(11,*['px','py','pz'].collect{mcb.getFloat(it,0) + (partb.getFloat(it,iele)-mcb.getFloat(it,0))*mfac})
				   pro = LorentzVector.withPID(2212,*['px','py','pz'].collect{profac*(mcb.getFloat(it,1) + (partb.getFloat(it,ipro)-mcb.getFloat(it,1))*mfac)})
				   //println "second electron is"+ele
				   def evec = new Vector3()
				   evec.setMagThetaPhi(ele.p(), ele.theta(), ele.phi())
				   def pvec = new Vector3()
				   pvec.setMagThetaPhi(pro.p(), pro.theta(), pro.phi())
			   }

			   def wvec = beam+target-ele
			   def qvec = beam-ele
			   def epx = beam+target-ele-pro

			   //println "qvec is " + qvec
			   //println "qvec comp is " + -qvec.mass2()

			   def pdet = (partb.getShort('status',ipro)/1000).toInteger()==2 ? 'FD':'CD'

			   def profi = Math.toDegrees(pro.phi())
			   if(profi<0) profi+=360

			   hhel.fill(ihel)
			   hphi.fill(profi)
			   hq2.fill(-qvec.mass2())

		   }


	}

}

def smalltest = 0

if (smalltest == 0){
	println "Yes, smalltest is zero"
	def evcount = new AtomicInteger()
	evcount.set(0)
	while(reader.hasEvent()) {
		evcount.getAndIncrement()
		if(evcount.get() % 10000 == 0){
			println "event count: "+evcount.get()/10000 + "0 K"
		}
		def event = reader.getNextEvent()
		processEvent(event,hhel,hphi,hq2)
	}
}
else {
	for (int i=0; i < smalltest; i++) {
		def event = reader.getNextEvent()
		processEvent(event,hhel,hphi,hq2)
	}
}

reader.close()

def run = "testrun5036"
TDirectory out = new TDirectory()
out.mkdir('/'+run)
out.cd('/'+run)

out.addDataSet(hhel)
out.addDataSet(hphi)
out.addDataSet(hq2)

out.writeFile(run+'.hipo')
