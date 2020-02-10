#!/usr/bin/groovy

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
import java.util.Date

float EB = 10.6f
int run = args[0].split("/")[-1].split('\\.')[0][-4..-1].toInteger()
if(run>6607) EB=10.2f

def Hist_beta_p 	= [:].withDefault{new H2F("Hist_beta_p${it}"		, "Beta vs. Momentum ${it}"		          ,800,0,EB,300,  -0.1   ,1.2)}
def Hist_deltaB_p = [:].withDefault{new H2F("Hist_deltaB_p${it}"	, "Delta B vs. Momentum ${it}"          ,800,0,EB,1600,  -1     ,1  )}
def Hist_beta_p2 	= [:].withDefault{new H2F("Hist_beta_p2${it}"	  , "Beta (path/time) vs. Momentum ${it}"	,800,0,EB,300,  -0.1   ,1.2)}
def Hist_beta_p_ctof = new H2F("Hist_beta_p_ctof"	  , "Beta (CTOF) vs. Momentum"	,800,0,EB,300,  -1.2   ,1.2)
def Hist_beta_p2_ctof 	= new H2F("Hist_beta_p2_ctof"	  , "Beta (path/time) (CTOF) vs. Momentum"	,800,0,EB,300,  -1.2   ,1.2)

def printer(string,override){
	k = 0
	if(k==1){
		println(string)
	}
	if(k==0){
		if(override==1){
			println(string)
		}
	}
}

date = new Date()
fst = date.getTime()
file_start_time = date.format("yyyyMMdd_HH-mm-ss")

array_index = -1
for(fname in args) {
	array_index++
	runtime = new Date()
	println("Processing $fname at time ${runtime.format('HH:mm:ss')}")
	time_diff = (runtime.getTime() - fst)/1000/60
	if(array_index>0){
		println("Total running time in minutes is: ${Math.round(time_diff*10)/10}")
		array_left = args.length-array_index
		println("$array_index Files have been processed, $array_left files remain")
		time_left = time_diff*array_left/array_index
		uTS = Math.round(time_left*60+runtime.getTime()/1000)
		eta = Date.from(Instant.ofEpochSecond(uTS)).format('HH:mm:ss')
		println("Anticipated finish time is $eta")
	}

	def reader = new HipoDataSource()

	reader.open(fname)
	while(reader.hasEvent()) {
	//for(int ii=0;ii<=66;ii++){
	//println("On event number $ii")
		def event = reader.getNextEvent()
		if(!event.hasBank("REC::Particle")){
			printer("event bank empty, skipping",0)
			continue
		}
		def event_start_time = event.getBank("REC::Event").getFloat("startTime")[0]

		if(event_start_time>0){
			printer("Event time was $event_start_time, skipping",0)
			continue
		}

	}

}

date = new Date()
file_end_time = date.format("yyyyMMdd_HH-mm-ss")

out.writeFile("hipo-root_files/pID_new_protons_${file_end_time}_"+run+'.hipo')


println("Started at $file_start_time")
println("Ended   at $file_end_time")
