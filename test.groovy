#!/home/kenjo/.groovy/coatjava/bin/run-groovy
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

def processors = [new eppi0_mon()]

def evcount = new AtomicInteger()
def save = {
  processors.each{
    def out = new TDirectory()
    out.mkdir("/root")
    out.cd("/root")
    it.hists.each{out.writeDataSet(it.value)}
    def clasname = it.getClass().getSimpleName()
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
      def event = reader.getNextEvent()
      processors.each{it.processEvent(event)}
    }

    reader.close()
  }
}

exe.shutdown()
save()
