#!/usr/bin/groovy

import org.jlab.io.hipo.HipoDataSource
import org.jlab.detector.base.DetectorType
import org.jlab.clas.physics.Vector3
import org.jlab.groot.data.H1F
import org.jlab.groot.data.H2F
import org.jlab.groot.data.TDirectory
import groovyx.gpars.GParsPool
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

MyMods.enable()
/////////////////////////////////////////////////////////////////////

def outname = args[0].split('/')[-1]

def processor = [new ep_test()]

def evcount = new AtomicInteger()
def save = {
  processor.each{
    def out = new TDirectory()
    out.mkdir("/base")
    out.cd("/base")
    it.hists.each{out.writeDataSet(it.value)}
    def clasname = it.getClass().getSimpleName()
    println "clasname is "+clasname
    out.writeFile("test_${clasname}_${outname}")
  }
  println "event count: "+evcount.get()
  evcount.set(0)
}

def exe = Executors.newScheduledThreadPool(1)
exe.scheduleWithFixedDelay(save, 5, 30, TimeUnit.SECONDS)

GParsPool.withPool 12, {
  args.eachParallel{fname->
    def reader = new HipoDataSource()
    reader.open(fname)

    while(reader.hasEvent()) {
      evcount.getAndIncrement()
      if(evcount.get() % 10000 == 0){
      	println "event count: "+evcount.get()/10000 + "0 K"
	}
      def event = reader.getNextEvent()
      processor.each{it.processEvent(event)}
    }

    reader.close()
  }
}

exe.shutdown()
save()
