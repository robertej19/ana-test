#!/usr/bin/groovy

import java.io.*
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.Date
import org.jlab.clas.physics.LorentzVector
import org.jlab.clas.physics.Vector3
import org.jlab.detector.base.DetectorType
import org.jlab.groot.base.GStyle
import org.jlab.groot.data.GraphErrors
import org.jlab.groot.data.H1F
import org.jlab.groot.data.H2F
import org.jlab.groot.data.TDirectory
import org.jlab.groot.fitter.DataFitter
import org.jlab.groot.graphics.EmbeddedCanvas
import org.jlab.groot.group.DataGroup
import org.jlab.groot.math.F1D
import org.jlab.io.base.DataBank
import org.jlab.io.base.DataEvent
import org.jlab.io.hipo.HipoDataSource
import org.jlab.io.hipo.HipoDataSync

MyMods.enable() //I don't know what this does, its from Andrey, don't touch it, it works

println("\n \n \n \n \n \n \n \n \n \n \n \n \n \n")

/////////////////

def printer(string,override){
	k = 0
	if(k==1){
		println("\n"+string+"\n")
		if(override==2){
			println(string)
		}
	}
	if(k==0){
		if(override==1){
			println(string+"\n")
		}
		if(override==2){
			println(string)
		}
	}
}

def hhel = new H1F("Hist_ihel","helicity",7,-2,2)
def hphi = new H1F("Hist_phi","Phi Distribution",2500,-10,370)
def hq2 = new H1F("Hist_q2","Q^2 Distribution",1000,0,12)
def hW = new H1F("Hist_W","W Distribution",1000,0,12)
def hxB = new H1F("Hist_xB","Bjorken x Distribution",1000,-1,2)

def processEvent(event,hhel,hphi,hq2,hW,hxB) {
	def beam = LorentzVector.withPID(11,0,0,10.6)
	def target = LorentzVector.withPID(2212,0,0,0)

	def banknames = ['REC::Event','REC::Particle','REC::Cherenkov','REC::Calorimeter','REC::Traj','REC::Track','REC::Scintillator']

	if(banknames.every{event.hasBank(it)}) {
		def (evb,partb,cc,ec,traj,trck,scib) = banknames.collect{event.getBank(it)}
		def banks = [cc:cc,ec:ec,part:partb,traj:traj,trck:trck]
		def ihel = evb.getByte('helicity',0)
		printer("ihel is "+ihel,0)



		def index_of_electrons_and_protons = (0..<partb.rows()).findAll{partb.getInt('pid',it)==11 && partb.getShort('status',it)<0}
			.collectMany{iele->(0..<partb.rows()).findAll{partb.getInt('pid',it)==2212}.collect{ipro->[iele,ipro]}
		}
		printer("index_of_electrons_and_protons "+index_of_electrons_and_protons,0)

		def index_of_pions = (0..<partb.rows()-1).findAll{partb.getInt('pid',it)==22 && partb.getShort('status',it)>=2000}
			.findAll{ig1->'xyz'.collect{partb.getFloat("p$it",ig1)**2}.sum()>0.16}
			.collectMany{ig1->
			(ig1+1..<partb.rows()).findAll{partb.getInt('pid',it)==22 && partb.getShort('status',it)>=2000}
			.findAll{ig2->'xyz'.collect{partb.getFloat("p$it",ig2)**2}.sum()>0.16}
			.collect{ig2->[ig1,ig2]}
		}
		printer("index of pions is " + index_of_pions,0)

		def isep0s = index_of_electrons_and_protons.findAll{iele,ipro->
			def ele = LorentzVector.withPID(11,*['px','py','pz'].collect{partb.getFloat(it,iele)})
			def pro = LorentzVector.withPID(2212,*['px','py','pz'].collect{partb.getFloat(it,ipro)})
			printer("first electron is"+ele,0)

			if(event.hasBank("MC::Particle")) {
				printer("Event has MC Particle bank!",0)
				def mcb = event.getBank("MC::Particle")
				def mfac = (partb.getShort('status',ipro)/1000).toInteger()==2 ? 3.2 : 2.5

				def profac = 0.9

				//mfac=1
				profac = 1.0

				ele = LorentzVector.withPID(11,*['px','py','pz'].collect{mcb.getFloat(it,0) + (partb.getFloat(it,iele)-mcb.getFloat(it,0))*mfac})
				pro = LorentzVector.withPID(2212,*['px','py','pz'].collect{profac*(mcb.getFloat(it,1) + (partb.getFloat(it,ipro)-mcb.getFloat(it,1))*mfac)})
				printer("second electron is"+ele)
				def evec = new Vector3()
				evec.setMagThetaPhi(ele.p(), ele.theta(), ele.phi())
				def pvec = new Vector3()
				pvec.setMagThetaPhi(pro.p(), pro.theta(), pro.phi())
			}

			def wvec = beam+target-ele
			def qvec = beam-ele
			def epx = beam+target-ele-pro
			def xBjorken = -qvec.mass2()/(2*pro.vect().dot(qvec.vect()))
			printer("xB is " + xBjorken,0)

			//println "qvec is " + qvec
			//println "qvec comp is " + -qvec.mass2()

			def pdet = (partb.getShort('status',ipro)/1000).toInteger()==2 ? 'FD':'CD'

			def profi = Math.toDegrees(pro.phi())
			if(profi<0) profi+=360

			def esec = (0..<scib.rows()).find{scib.getShort('pindex',it)==iele}?.with{scib.getByte('sector',it)}
			def psec = (0..<scib.rows()).find{scib.getShort('pindex',it)==ipro}?.with{scib.getByte('sector',it)}
			if(psec==0) {
				psec = Math.floor(profi/60).toInteger() +2
				if(psec==7) psec=1
			}





			hhel.fill(ihel)
			hphi.fill(profi)
			hq2.fill(-qvec.mass2())
			hW.fill(wvec.mass())
			hxB.fill(xBjorken)

		 }
	}
}


def screen_updater(FileStartTime,CurrentCounter,CountRate,NumTotalCounts){
	if(CurrentCounter % CountRate == 0){
		runtime = new Date()
		TimeElapsed = (runtime.getTime() - FileStartTime)/1000/60
		CountsLeft = NumTotalCounts-CurrentCounter
		TimeLeft = TimeElapsed/CurrentCounter*CountsLeft
		Rate = CurrentCounter/TimeElapsed/1000/60
		uTS = Math.round(TimeLeft*60+runtime.getTime()/1000)
		eta = Date.from(Instant.ofEpochSecond(uTS)).format('HH:mm:ss')

		//printer("Total running time in minutes is: ${TimeElapsed.round(2)}",2)
		printer(CurrentCounter+" Events have been processed, $CountsLeft files remain",2)
		printer("Processing Rate is ${Rate.round(1)} kHz",2)
		printer("Anticipated finish time is $eta",1)
	}
}





def reader = new HipoDataSource()
def fname = args[0]
def NumEventsToProcess = args[1].toInteger()
reader.open(fname)
def NumEventsInFile= reader.getSize().toInteger()
if (NumEventsToProcess == 0){NumEventsToProcess = NumEventsInFile}

def evcount = new AtomicInteger()
evcount.set(0)

def date = new Date()
def FileStartTime = date.getTime()
printer("Processing file at time ${date.format('HH:mm:ss')}",1)


def CountRate = NumEventsToProcess/10
printer("Processing $NumEventsToProcess events",1)
for (int i=0; i < NumEventsToProcess; i++) {
	evcount.getAndIncrement()
	screen_updater(FileStartTime,evcount.get(),CountRate.toInteger(),NumEventsToProcess)
	def event = reader.getNextEvent()
	processEvent(event,hhel,hphi,hq2,hW,hxB)
}

endtime = new Date()
def TotalRunTime = (endtime.getTime() - FileStartTime)/1000/60
printer(date.getTime(),2)
printer(FileStartTime,2)
printer("Finished processing ${(NumEventsToProcess/1000000).round(2)} M events at ${date.format('HH:mm:ss')},total run time ${TotalRunTime} minutes",1)
reader.close()

def OutFileName = "output_file_histos"
TDirectory out = new TDirectory()
out.mkdir('/'+OutFileName)
out.cd('/'+OutFileName)
out.addDataSet(hhel)
out.addDataSet(hphi)
out.addDataSet(hq2)
out.addDataSet(hW)
out.addDataSet(hxB)
out.writeFile(OutFileName+'.hipo')

/*Shit that does not work for trying to format axes in plots.
https://github.com/gavalian/groot/wiki/Histograms might have some more helpful info.
canvas.draw(hW);
canvas.setFont("HanziPen TC");
canvas.setTitleSize(72);
canvas.setAxisTitleSize(72);
canvas.setAxisLabelSize(76);
canvas.draw(hW);
GStyle.getAxisAttributesX().setTitleFontSize(98);
GStyle.getAxisAttributesX().setLabelFontSize(90);
EmbeddedCanvas canvas = new EmbeddedCanvas();
*/
