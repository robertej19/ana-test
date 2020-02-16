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
import groovy.io.FileType

MyMods.enable() //I don't know what this does, its from Andrey, don't touch it, it works

println("\n \n \n \n \n \n \n \n \n \n \n \n \n \n")

Mil = 1000000

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




def FileGetter(FileLocation){
	def FileList = []
	def dir = new File(FileLocation)
	dir.eachFileRecurse (FileType.FILES) { file ->
		FileList << file
	}
	return FileList
}

def hhel = new H1F("Hist_ihel","helicity",7,-2,2)
def hphi = new H1F("Hist_phi","Phi Distribution",2500,-10,370)
def hq2 = new H1F("Hist_q2","Q^2 Distribution",1000,0,12)
def hW = new H1F("Hist_W","W Distribution",1000,0,12)
def hxB = new H1F("Hist_xB","Bjorken x Distribution",1000,0,1.5)
def H_xB_Q2 = new H2F("Hist_xB_Q2" , "Bjorken X vs. Q^2",100,0,1.5,100,0,12)

def processEvent(event,hhel,hphi,hq2,hW,hxB,H_xB_Q2) {
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
			printer("epx mass squared is:${epx.mass2()}",0)
			def xBjorken = -qvec.mass2()/(2*pro.vect().dot(qvec.vect()))
			printer("xB is " + xBjorken,0)

			def pdet = (partb.getShort('status',ipro)/1000).toInteger()==2 ? 'FD':'CD'

			def profi = Math.toDegrees(pro.phi())
			if(profi<0) profi+=360

			def esec = (0..<scib.rows()).find{scib.getShort('pindex',it)==iele}?.with{scib.getByte('sector',it)}
			def psec = (0..<scib.rows()).find{scib.getShort('pindex',it)==ipro}?.with{scib.getByte('sector',it)}
			if(psec==0) {
				psec = Math.floor(profi/60).toInteger() +2
				if(psec==7) psec=1
			}

			def isep0 = epx.mass2()<1 && wvec.mass()>2

			def pi0s = index_of_pions.collect{ig1,ig2->
				def g1 = LorentzVector.withPID(22,*['px','py','pz'].collect{partb.getFloat(it,ig1)})
				def g2 = LorentzVector.withPID(22,*['px','py','pz'].collect{partb.getFloat(it,ig2)})
				if(ele.vect().theta(g1.vect())>8 && ele.vect().theta(g2.vect())>8) {
					def gg = g1+g2
					def ggmass = gg.mass()
					def ispi0 = ggmass<0.2 && ggmass>0.07// && gg.p()>1.5

					if(ispi0) {
						def epggx = epx-gg
						def thetaXPi = epx.vect().theta(gg.vect())
						def dpt0 = epggx.px().abs()<0.3 && epggx.py().abs()<0.3
						def dmisse0 = epggx.e()<1
						def tt0 = -(pro-target).mass2()
						def procalc = beam+target-ele-gg
						def tt1 = -(procalc-target).mass2()

						if(isep0){
							hhel.fill(ihel)
							hphi.fill(profi)
							hq2.fill(-qvec.mass2())
							hW.fill(wvec.mass())
							hxB.fill(xBjorken)
							H_xB_Q2.fill(xBjorken,-qvec.mass2())
						}
					}
					return ispi0
				}
			}
			return false
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
		printer("${(CurrentCounter/Mil).round(2)}M events have been processed, ${(CountsLeft/Mil).round(2)}M events remain",2)
		printer("Processing Rate is ${Rate.round(1)} kHz",2)
		printer("Anticipated finish time is $eta, ${TimeLeft.round(2)} minutes left",1)
	}
}





def reader = new HipoDataSource()
if (args.size()<2) {
	printer("You need to include the number of events you want to process in the start command!",1)
	printer("For example, <run-groovy filename.groovy hipo_file_to_process.hipo 1000",1)
}


def FilesToProcess = FileGetter(args[0])
def NumEventsToProcess = args[1].toInteger()
printer("The following files have been found: ",1)

for (FileName in FilesToProcess)
printer(FileName,1)


for (int i=0; i < FilesToProcess.size(); i++) {
	def fname = FilesToProcess[i]
	reader.open(fname)
	def NumEventsInFile= reader.getSize().toInteger()
	if (NumEventsToProcess == 0){NumEventsToProcess = NumEventsInFile}

	def evcount = new AtomicInteger()
	evcount.set(0)

	def date = new Date()
	def FileStartTime = date.getTime()
	printer("Starting to process file $fname at ${date.format('HH:mm:ss')}",1)



	def CountRate = NumEventsToProcess/10
	printer("Processing $NumEventsToProcess events",1)
	for (int j=0; j < NumEventsToProcess; j++) {
		evcount.getAndIncrement()
		screen_updater(FileStartTime,evcount.get(),CountRate.toInteger(),NumEventsToProcess)
		def event = reader.getNextEvent()
		processEvent(event,hhel,hphi,hq2,hW,hxB,H_xB_Q2)
	}

	endtime = new Date()
	def TotalRunTime = (endtime.getTime() - FileStartTime)/1000/60
	printer("Finished processing ${(NumEventsToProcess/Mil).round(2)} M events at ${date.format('HH:mm:ss')},total run time ${TotalRunTime.round(2)} minutes",1)
	reader.close()
}

//xxxxx


def OutFileName = "output_file_histos"
TDirectory out = new TDirectory()
out.mkdir('/'+OutFileName)
out.cd('/'+OutFileName)
out.addDataSet(hhel)
out.addDataSet(hphi)
out.addDataSet(hq2)
out.addDataSet(hW)
out.addDataSet(hxB)
out.addDataSet(H_xB_Q2)
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
