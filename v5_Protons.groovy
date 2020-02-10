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
	for(int ii=0;ii<=6666;ii++){
	println("On event number $ii")
		def event = reader.getNextEvent()
		if(!event.hasBank("REC::Particle")){
			printer("event bank empty, skipping",0)
			continue
		}
		def event_start_time = event.getBank("REC::Event").getFloat("startTime")[0]

		if(event_start_time<0){
			printer("Event time was $event_start_time, skipping",0)
			continue
		}


		def recon_Particles = event.getBank("REC::Particle")
		def recon_Cal = event.getBank("REC::Calorimeter")
		def recon_Scint = event.getBank("REC::Scintillator")

		if(!(recon_Particles.getInt("pid",0)==11)){
			printer("Event had trigger particle with pID not equal to 11, skipping",0)
			continue
		}

		def particle_momenta = ['x','y','z'].collect{recon_Particles.getFloat('p'+it)}.transpose().collect{Math.sqrt(it.collect{x->x*x}.sum())}
		def particle_stati = recon_Particles.getInt('status')
		def scint_hit_array = recon_Scint.getShort('pindex')*.toInteger()
		def cal_hit_array = recon_Cal.getShort('pindex')*.toInteger()
		printer("scintillator interaction index array is $scint_hit_array",0)
		printer("calorimeter interaction index array is $cal_hit_array",0)
		printer("particle status is $particle_stati",0)

		def scint_times = [recon_Scint.getShort('pindex')*.toInteger(), recon_Scint.getFloat('time')].transpose().collectEntries()
		def scint_paths = [recon_Scint.getShort('pindex')*.toInteger(), recon_Scint.getFloat('path')].transpose().collectEntries()
		def scint_layers = [recon_Scint.getShort('pindex')*.toInteger(), recon_Scint.getInt('layer')].transpose().collectEntries()
		def scint_sectors = [recon_Scint.getShort('pindex')*.toInteger(), recon_Scint.getInt('sector')].transpose().collectEntries()
		def scint_detectors = [recon_Scint.getShort('pindex')*.toInteger(), recon_Scint.getInt('detector')].transpose().collectEntries()
		def cal_detectors = [recon_Cal.getShort('pindex')*.toInteger(), recon_Cal.getInt('detector')].transpose().collectEntries()

		printer("layer in scints are: $scint_layers",0)
		printer("sectors in scints are: $scint_sectors",0)
		printer("scint detectors are: $scint_detectors",0)
		printer("cal detectors are: $cal_detectors",0)

		for(int particle_index=0;particle_index<event.getBank("REC::Particle").rows();particle_index++){
			if(!(recon_Particles.getInt("charge",particle_index)>0)){
				continue
			}
			//if(!recon_Particles.getInt("pid",p)==2212) continue;

			float particle_momentum = particle_momenta[particle_index]
			float beta_recon = recon_Particles.getFloat("beta",particle_index)
			float proton_mass = 0.938 //Proton mass in GeV
			particle_momentum_squared = particle_momentum*particle_momentum
			proton_mass_squared = proton_mass*proton_mass
			float beta_calculated = Math.sqrt(particle_momentum_squared/(particle_momentum_squared+proton_mass_squared))

			printer("particle number index is $particle_index",0)
			printer("scint response for $particle_index is: "+scint_sectors[particle_index]+" layer: "+
				scint_layers[particle_index]+" sector: "+scint_sectors[particle_index],0)
			printer("cal response is: "+cal_detectors[particle_index],0)
			printer("status of particle is: "+particle_stati[particle_index],0)
			printer("End of particle information",0)

			//	if(secs[particle_index]==12){
			//		println("using scint data")
			//		p_layer = recon_Scint.getInt("layer",particle_index)
			//               p_sect = recon_Scint.getInt("sector",particle_index)
			//		println(p_layer + "  is layer, sector is: "+p_sect)
			//	}
			//	if(cecs[particle_index]==7){
			//		println("using calorimeter data")
			//		p_layer = recon_Cal.getInt("layer",particle_index)
			//              p_sect = recon_Cal.getInt("sector",particle_index)
			//		println(p_layer + "  is layer, sector is: "+p_sect)
			//	}

			if(scint_detectors[particle_index]==4){
				printer("particle detected in CTOF",0)
				printer("particle status is: ${particle_stati[particle_index]}",0)
				//printer("event start time is: ${event_start_time}")
				//if(beta_recon<0){
			//		println("Beta was less than zero")
			//		println(event_start_time)
			//		printer("particle beta is $beta_recon",1)
			//		println(scint_paths[particle_index])
			//		println("above is scint path, below is scint time")
			//		println(scint_times[particle_index])
			//	}


				p_time = scint_times[particle_index]
				p_path = scint_paths[particle_index]

				printer("layer and sector are: "+ scint_sectors[particle_index]+" layer: "+
					scint_layers[particle_index]+" sector: "+scint_sectors[particle_index],0)
				Hist_beta_p_ctof.fill(particle_momentum,beta_recon)
				Hist_beta_p2_ctof.fill(particle_momentum,p_path/(p_time-event_start_time)/29.98)

			}

			if(scint_detectors[particle_index]==12){
				p_layer = scint_layers[particle_index]
				p_sect = scint_sectors[particle_index]
				p_time = scint_times[particle_index]
				p_path = scint_paths[particle_index]
				//p_comp =recon_Scint.getInt('component',particle_index)
				printer("particle status is: "+recon_Particles.getInt('status')[particle_index],0)

				if ([1, 2, 3, 4, 5, 6].contains(p_sect) && [1, 2, 3].contains(p_layer)){
					title = "sec${p_sect}_layer${p_layer}"
					Hist_beta_p[title].fill(particle_momentum,beta_recon)
					Hist_deltaB_p[title].fill(particle_momentum,beta_recon-beta_calculated)
					Hist_beta_p2[title].fill(particle_momentum,p_path/(p_time-event_start_time)/29.98)
				}
				else{
					println("Value not contained!")
					println(p_sect)
					println(p_layer)
				}
			}
		}
	}}
	reader.close()
}
TDirectory out = new TDirectory()
out.mkdir('/'+run)
out.cd('/'+run)

for(int isec=1;isec<=6;isec++){
	for(int ilay=1;ilay<=3;ilay++){
		title = "sec${isec}_layer${ilay}"
		out.addDataSet(Hist_beta_p[title])
		out.addDataSet(Hist_deltaB_p[title])
		out.addDataSet(Hist_beta_p2[title])
	}
}

out.addDataSet(Hist_beta_p_ctof)
out.addDataSet(Hist_beta_p2_ctof)
date = new Date()
file_end_time = date.format("yyyyMMdd_HH-mm-ss")

out.writeFile("hipo-root_files/pID_new_protons_${file_end_time}_"+run+'.hipo')


println("Started at $file_start_time")
println("Ended   at $file_end_time")
