package ws.prova.func;

/*
 *  Calculation of Pi using quadrature realized with an approached based on using parallel map from
 *  Functional Java.
 *
 *  Copyright � 2010�2011 Russel Winder
 */

import java.util.concurrent.Executors;

import fj.F ;
import fj.F2 ;
import fj.Unit ;
import fj.data.Array ;
import fj.control.parallel.ParModule ;
import fj.control.parallel.Promise;
import fj.control.parallel.Strategy ;

//import java.util.concurrent.Executors ;

public class Pi {
  private static void execute ( final int numberOfTasks ) {
    final int n = 1000000000 ;
    final double delta = 1.0 / n ;
    final long startTimeNanos = System.nanoTime ( ) ;
    final int sliceSize = n / numberOfTasks ;
    final Array<Integer> inputData = Array.range ( 0 , numberOfTasks ) ;
    final F<Integer,Double> sliceCalculator = new F<Integer,Double> ( ) {
      @Override public Double f ( final Integer taskId ) {
        final int start = 1 + taskId * sliceSize ;
        final int end = ( taskId + 1 ) * sliceSize ;
        double sum = 0.0 ;
        for ( int i = start ; i <= end ; ++i ) {
          final double x = ( i - 0.5 ) * delta ;
          sum += 1.0 / ( 1.0 + x * x ) ;
        }
        return sum ;
      }
    } ;
    final F2<Double,Double,Double> add = new F2<Double,Double,Double> ( ) {
      @Override public Double f ( final Double a , final Double b ) { return a + b ; }
    } ;
    final Strategy<Unit> strategy = Strategy.executorStrategy ( Executors.newCachedThreadPool ( ) ) ;
    //final Strategy<Unit> strategy = Strategy.simpleThreadStrategy ( ) ;
    final Promise<Array<Double>> parMap = ParModule.parModule ( strategy ).parMap ( inputData , sliceCalculator );
	final double pi = 4.0 * delta * parMap.claim ( ).foldLeft ( add , 0.0 ) ;
    final double elapseTime = ( System.nanoTime ( ) - startTimeNanos ) / 1e9 ;
    System.out.println ( "==== Java FunctionalJava ParMap pi = " + pi ) ;
    System.out.println ( "==== Java FunctionalJava ParMap iteration count = " + n ) ;
    System.out.println ( "==== Java FunctionalJava ParMap elapse = " + elapseTime ) ;
    System.out.println ( "==== Java FunctionalJava ParMap processor count = " + Runtime.getRuntime ( ).availableProcessors ( ) ) ;
    System.out.println ( "==== Java FunctionalJava ParMap thread count = " + numberOfTasks ) ;
  }
  public static void main ( final String[] args ) {
    Pi.execute ( 1 ) ;
    System.out.println ( ) ;
    Pi.execute ( 2 ) ;
    System.out.println ( ) ;
    Pi.execute ( 8 ) ;
    System.out.println ( ) ;
    Pi.execute ( 32 ) ;
  }
}
