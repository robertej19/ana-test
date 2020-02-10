#!/usr/bin/groovy

import org.jlab.io.hipo.HipoDataSource
import org.jlab.io.hipo.HipoDataSync
import org.jlab.detector.base.DetectorType

def reader = new HipoDataSource()
reader.open(args[0])


for (int i=0; i < 50; i++) {
  def event = reader.getNextEvent()

  if (event.hasBank("REC::Particle")){// && event.hasBank("REC::Calorimeter")) {
    //def event_cal = event.getBank("REC::Calorimeter")
    def event_scint = event.getBank("REC::Scintillator")
    //def pind_array = event_cal.getShort('pindex')*.toInteger()
    def pind_sarray = event_scint.getShort('pindex')*.toInteger()
    def sect_sarray_l = event_scint.getByte('layer')
   // def sect_array = event_cal.getByte('sector')
    def sect_sarray = event_scint.getByte('sector')
    def stati = event.getBank("REC::Particle").getInt('status')
    //println(stati)
    //println "pindex array for event $i is: $pind_array and has sector array: $sect_array"
    //println("pindex sarray for event $i is: $pind_sarray and has sector sarray $sect_sarray and layer array $sect_sarray_l")
 

    def secs = [event_scint.getShort('pindex')*.toInteger(), event_scint.getByte('sector')].transpose().collectEntries()
    

    def sec_scint = event_scint.getFloat('time')
    //def det = event_scint.getInt('component')
    
    def dete = event_scint.getInt('detector')
    //print(sec_scint)
    event_start_time = event.getBank("REC::Event").getFloat("startTime")
    println(event_start_time)
    //println("detector is:$dete status is $stati \n")
    //println("dectecotr 1 is:" + event_scint.getInt('detector',1))
    //println(event.getBank("REC::Particle").rows())
    //def evp = event.getBank("REC::Particle")
    //evp.getInt("pid").eachWithIndex{pid, ind ->
    //  println("pID is $pid with sector _  $ind :"+ secs[ind])
//	}
    }
}

reader.close()
