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
	//def hists = new ConcurrentHashMap()

	def beam = LorentzVector.withPID(11,0,0,10.6)
	def target = LorentzVector.withPID(2212,0,0,0)

	def hw = {new H1F("$it","$it",200,0,5)}
	def hq2 = {new H1F("$it","$it",200,0,10)}
	def hhel = new H1F("Hist_ihel","helicity",7,-2,2)

	def banknames = ['REC::Event','REC::Particle','REC::Cherenkov','REC::Calorimeter','REC::Traj','REC::Track','REC::Scintillator']
	def processEvent(event) {
		if(banknames.every{event.hasBank(it)}) {
			def (evb,partb,cc,ec,traj,trck,scib) = banknames.collect{event.getBank(it)}
			def banks = [cc:cc,ec:ec,part:partb,traj:traj,trck:trck]
			def ihel = evb.getByte('helicity',0)
			println "ihel is "+ihel

			def index_of_electrons_and_protons = (0..<partb.rows()).findAll{partb.getInt('pid',it)==11 && partb.getShort('status',it)<0}
				.collectMany{iele->(0..<partb.rows()).findAll{partb.getInt('pid',it)==2212}.collect{ipro->[iele,ipro]}
			}
			println "index_of_electrons_and_protons "+index_of_electrons_and_protons

			def index_of_pions = (0..<partb.rows()-1).findAll{partb.getInt('pid',it)==22 && partb.getShort('status',it)>=2000}
				.findAll{ig1->'xyz'.collect{partb.getFloat("p$it",ig1)**2}.sum()>0.16}
				.collectMany{ig1->
				(ig1+1..<partb.rows()).findAll{partb.getInt('pid',it)==22 && partb.getShort('status',it)>=2000}
				.findAll{ig2->'xyz'.collect{partb.getFloat("p$it",ig2)**2}.sum()>0.16}
				.collect{ig2->[ig1,ig2]}
			}
			println "index of pions is " + index_of_pions

			def isep0s = index_of_electrons_and_protons.findAll{iele,ipro->
				def ele = LorentzVector.withPID(11,*['px','py','pz'].collect{partb.getFloat(it,iele)})
				def pro = LorentzVector.withPID(2212,*['px','py','pz'].collect{partb.getFloat(it,ipro)})
				//println "first electron is"+ele
				if(event.hasBank("MC::Particle")) {
					println "Event has MC Particle bank!"
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

				def pdet = (partb.getShort('status',ipro)/1000).toInteger()==2 ? 'FD':'CD'

				def profi = Math.toDegrees(pro.phi())
				if(profi<0) profi+=360

				println "wvec is" + wvec
				println "profi is " + profi

				def esec = (0..<scib.rows()).find{scib.getShort('pindex',it)==iele}?.with{scib.getByte('sector',it)}
				def psec = (0..<scib.rows()).find{scib.getShort('pindex',it)==ipro}?.with{scib.getByte('sector',it)}
				if(psec==0) {
				  psec = Math.floor(profi/60).toInteger() +2
				  if(psec==7) psec=1
				}

				println "electron sector is " + esec
				def isep0 = epx.mass2()<1 && wvec.mass()>2
				println "i sep 0 is " + isep0


			}
			hhel.fill(ihel)
			return ihel
		}
	}
}
