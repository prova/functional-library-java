package test.ws.prova.func.tests;

import static fj.data.List.list;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import ws.prova.func.Callables;
import ws.prova.func.ParallelStrategy;
import ws.prova.func.Partial;
import fj.F;
import fj.F2;
import fj.P;
import fj.P2;
import fj.Unit;
import fj.control.parallel.ParModule;
import fj.control.parallel.Promise;
import fj.control.parallel.Strategy;
import fj.data.List;
import fj.data.Option;

/**
 * Test parallel execution of simple algorithms using the basics from Functional
 * Java with extensions based on Callables and ParallelStrategy inspired by the
 * Apocalisp blog.
 * 
 * Requires -Xmx512m to run.
 * 
 */
public class TestCallables {

	final ExecutorService s = Executors.newFixedThreadPool(32);
	final ParallelStrategy<Double> strategy = ParallelStrategy
			.executorStrategy(s);
	final ParallelStrategy<List<Double>> strategy2 = ParallelStrategy
			.executorStrategy(s);
	final F2<Double, Double, Double> sum = new F2<Double, Double, Double>() {
		@Override
		public Double f(Double a, Double b) {
			return a + b;
		}
	};
	final F2<Double, Double, Double> dist = new F2<Double, Double, Double>() {
		@Override
		public Double f(Double a, Double b) {
			return Math.abs(a - b);
		}
	};
	final List<Double> list = list(1.0, 2.0, 3.0, 4.0);

	/**
	 * Test bind (composition) of Callables
	 * @throws Exception
	 */
	@Test
	public void testBindCallables() throws Exception {
		F<Double, Callable<Double>> bob = new F<Double, Callable<Double>>() {
			public Callable<Double> f(final Double w) {
				return new Callable<Double>() {
					public Double call() {
						return w * 2.0;
					}
				};
			}
		};
		F<Double, Callable<Double>> alice = new F<Double, Callable<Double>>() {
			public Callable<Double> f(final Double w) {
				return new Callable<Double>() {
					public Double call() {
						return w * 4.0;
					}
				};
			}
		};

		Callable<Double> bobThenAlice = Callables.bind(bob.f(1.0), alice);
		// So far neither of bob() or alice() have been called, but will be when we invoke call() below
		Double result = bobThenAlice.call();
		assertEquals("Incorrect bind result", "8.0", result.toString());
	}

	/**
	 * Straight parallel map on a fj List
	 * 
	 * @throws Exception
	 */
	@Test
	public void testParallelMap() throws Exception {
		// The line below already starts computations using the supplied strategy
		final Callable<List<Double>> c = ParallelStrategy.parMap(strategy,
				new F<Double, Double>() {
					@Override
					public Double f(final Double a) {
						return a * 2.0;
					}
				}).f(list);
		// The line below synchronizes the results and copies them all into the list of results
		List<Double> o = c.call();
		System.out.println(o.toCollection());

		assertEquals("Incorrect result of parallel map",
				"[2.0, 4.0, 6.0, 8.0]", o.toCollection().toString());
	}

	/**
	 * Straight parallel map on a fj List with some calls throwing an exception
	 * 
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testParallelMapWithException() throws Exception {
		// The line below already starts computations using the supplied strategy
		final Callable<List<Double>> c = ParallelStrategy.parMap(strategy,
				new Partial<Double, Double, Exception>() {
					@Override
					public Double run(final Double a) throws Exception {
						if( a > 2.0 )
							throw new IOException("Fake IO exception");
						return a * 2.0;
					}
				}).f(list);
		// The line below synchronizes the results but blows up with IOException
		c.call();
	}

	/**
	 * A nested serial computation flattened inside a parallel computation
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSerialInsideParallelFlatMap() throws Exception {
		// The line below already starts computations using the supplied strategy
		final Callable<List<Double>> c = ParallelStrategy.parFlatMap(strategy2,
				new F<Double, List<Double>>() {
					@Override
					public List<Double> f(final Double a) {
						return list.map(new F<Double, Double>() {
							@Override
							public Double f(final Double b) {
								return a + b;
							}
						});
					}
				}).f(list);
		// The line below synchronizes the results and copies them all into the list of results
		List<Double> o = c.call();
		System.out.println(o.toCollection());
		assertEquals(
				"Incorrect result of serial computation inside a parallel flat map",
				"[2.0, 3.0, 4.0, 5.0, 3.0, 4.0, 5.0, 6.0, 4.0, 5.0, 6.0, 7.0, 5.0, 6.0, 7.0, 8.0]",
				o.toCollection().toString());
	}

	/**
	 * A nested parallel computation flattened inside a parallel computation
	 * 
	 * @throws Exception
	 */
	@Test
	public void testParallelInsideParallelFlatMap() throws Exception {
		final Callable<List<Double>> c = ParallelStrategy.parFlatMapCallable(
				strategy2, new F<Double, Callable<List<Double>>>() {
					@Override
					public Callable<List<Double>> f(final Double a) {
						return ParallelStrategy.parMap(strategy,
								new F<Double, Double>() {
									@Override
									public Double f(final Double b) {
										return a + b;
									}
								}).f(list);
					}
				}).f(list);
		List<Double> o = c.call();
		System.out.println(o.toCollection());
		assertEquals(
				"Incorrect result of parallel computation inside a parallel flat map",
				"[2.0, 3.0, 4.0, 5.0, 3.0, 4.0, 5.0, 6.0, 4.0, 5.0, 6.0, 7.0, 5.0, 6.0, 7.0, 8.0]",
				o.toCollection().toString());
	}

	/**
	 * Parallel zip of a list with its tail followed in parallel by foldLeft on
	 * the resulting list. It is a total length of edges between points.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testParallelZipFollowedByFold() throws Exception {
		final Callable<List<Double>> c = ParallelStrategy.parZipWith(strategy,
				dist).f(list, list.tail());
		// A Callables monad version of foldLeft of sum(Double,Double) with seed
		// 0.0
		final F<Callable<List<Double>>, Callable<Double>> parFoldLeft = Callables
				.fmap(List.<Double, Double> foldLeft().f(sum.curry()).f(0.0));
		final Callable<Double> c_length = strategy.par(parFoldLeft).f(c);
		Double o = c_length.call();
		System.out.println(o);
		assertEquals(
				"Incorrect result of the fold that follows a parallel zip",
				"3.0", o.toString());
	}

	/**
	 * A nested parallel computation and a fold inside a parallel computation.
	 * Sums distances between each point and all other points (as used in
	 * clustering).
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFoldsInsideParallelMap() throws Exception {
		// A Callables monad version of foldLeft of sum(Double,Double) with seed
		// 0.0
		final F<Callable<List<Double>>, Callable<Double>> parFoldLeft = Callables
				.fmap(List.<Double, Double> foldLeft().f(sum.curry()).f(0.0));
		final Callable<List<Double>> c = ParallelStrategy.parMapCallable(
				strategy, new F<Double, Callable<Double>>() {
					@Override
					public Callable<Double> f(final Double a) {
						Callable<List<Double>> c1 = ParallelStrategy.parMap(
								strategy, new F<Double, Double>() {
									@Override
									public Double f(final Double b) {
										return Math.abs(a - b);
									}
								}).f(list);
						return strategy.par(parFoldLeft).f(c1);
					}
				}).f(list);
		List<Double> o = c.call();
		System.out.println(o.toCollection());
		assertEquals("Incorrect result of folds inside a parallel map",
				"[6.0, 4.0, 4.0, 6.0]", o.toCollection().toString());
	}

	/**
	 * A nested parallel computation and a fold inside a parallel computation.
	 * Sums distances between each point and all other points (as used in
	 * clustering).
	 * 
	 * @throws Exception
	 */
	@Test(expected=IOException.class)
	public void testFoldsInsideParallelMapWithException() throws Exception {
		// A Callables monad version of foldLeft of sum(Double,Double) with seed
		// 0.0
		final F<Callable<List<Double>>, Callable<Double>> parFoldLeft = Callables
				.fmap(List.<Double, Double> foldLeft().f(sum.curry()).f(0.0));
		final Callable<List<Double>> c = ParallelStrategy.parMapCallable(
				strategy, new F<Double, Callable<Double>>() {
					@Override
					public Callable<Double> f(final Double a) {
						Callable<List<Double>> c1 = ParallelStrategy.parMap(
								strategy, new Partial<Double, Double, Exception>() {
									@Override
									public Double run(final Double b) throws Exception {
										if( a > 2.0 )
											throw new IOException("Fake IO exception");
										return Math.abs(a - b);
									}
								}).f(list);
						return strategy.par(parFoldLeft).f(c1);
					}
				}).f(list);
		// The line below synchronizes the results but blows up with IOException
		c.call();
	}

	@Test
	public void testPerformanceFoldsInsideParallelMap() throws Exception {
		final List<Double> longList = List.unfold(
				new F<Double, Option<P2<Double, Double>>>() {
					@Override
					public Option<P2<Double, Double>> f(Double a) {
						return a < 1001.0 ? Option.some(P.p(a, a + 1.0))
								: Option.<P2<Double, Double>> none();
					}
				}, 1.0);
		assertEquals("Incorrect generated list length", 1000, longList.length());
		System.out.println("Long list generated");
		final AtomicLong count = new AtomicLong(0);
		// A Callables monad version of foldLeft of sum(Double,Double) with seed
		// 0.0
		final F<Callable<List<Double>>, Callable<Double>> parFoldLeft = Callables
				.fmap(List.<Double, Double> foldLeft().f(sum.curry()).f(0.0));
		final Callable<List<Double>> c = ParallelStrategy.parMapCallable(
				strategy, new F<Double, Callable<Double>>() {
					@Override
					public Callable<Double> f(final Double a) {
						Callable<List<Double>> c1 = ParallelStrategy.parMap(
								strategy, new F<Double, Double>() {
									@Override
									public Double f(final Double b) {
										long n = count.incrementAndGet();
										if (n % 10000 == 0)
											System.out.println(n);
										return Math.abs(a - b);
									}
								}).f(longList);
						return strategy.par(parFoldLeft).f(c1);
					}
				}).f(longList);
		List<Double> o = c.call();
		System.out.println(o.length());
		assertEquals("Incorrect result of folds inside a parallel map", 1000,
				o.length());
	}

	/**
	 * Straight Promise-based parallel map on a fj List
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPromiseParallelMap() throws Exception {
		final Strategy<Unit> strategy = Strategy.executorStrategy(Executors
				.newFixedThreadPool(32));
		final Promise<List<Double>> c = ParModule.parModule(strategy).parMap(
				list, new F<Double, Double>() {
					@Override
					public Double f(final Double a) {
						return a * 2.0;
					}
				});
		List<Double> o = c.claim();
		System.out.println(o.toCollection());

		assertEquals("Incorrect result of parallel map",
				"[2.0, 4.0, 6.0, 8.0]", o.toCollection().toString());
	}

	/**
	 * Straight Promise-based parallel map on a fj List.
	 * This actually DOES NOT work beyond fairly modest list lengths.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPerformancePromiseParallelMap() throws Exception {
		final List<Double> longList = List.unfold(
				new F<Double, Option<P2<Double, Double>>>() {
					@Override
					public Option<P2<Double, Double>> f(Double a) {
						return a < 20001.0 ? Option.some(P.p(a, a + 1.0))
								: Option.<P2<Double, Double>> none();
					}
				}, 1.0);
		assertEquals("Incorrect generated list length", 20000,
				longList.length());
		// final long startTimeNanos = System.nanoTime ( ) ;
		final AtomicLong count = new AtomicLong(0);
		final Strategy<Unit> strategy = Strategy.executorStrategy(Executors
				.newFixedThreadPool(32));
		final Promise<List<Double>> c = ParModule.parModule(strategy).parMap(
				longList, new F<Double, Double>() {
					@Override
					public Double f(final Double a) {
						long n = count.incrementAndGet();
						if (n % 1000 == 0)
							System.out.println(n);
						double result = 0.0;
						for( int i=0; i<1000; i++ )
							result += Math.expm1(Math.cosh(a * 2.0));
						return result;
					}
				});
		List<Double> o = c.claim(); // 5L,TimeUnit.SECONDS);
		System.out.println(o.length());

		assertEquals("Incorrect result of parallel map", 20000, o.length());
	}

	/**
	 * Straight parallel map on a fj List
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPerformanceParallelMap() throws Exception {
		final List<Double> longList = List.unfold(
				new F<Double, Option<P2<Double, Double>>>() {
					@Override
					public Option<P2<Double, Double>> f(Double a) {
						return a < 1000001.0 ? Option.some(P.p(a, a + 1.0))
								: Option.<P2<Double, Double>> none();
					}
				}, 1.0);
		assertEquals("Incorrect generated list length", 1000000,
				longList.length());
		final AtomicLong count = new AtomicLong(0);
		// Here Double is the target type of function f
		final F<List<Double>, Callable<List<Double>>> parMap = ParallelStrategy
				.parMap(strategy, new F<Double, Double>() {
					@Override
					public Double f(final Double a) {
						long n = count.incrementAndGet();
						if (n % 1000 == 0)
							System.out.println(n);
						double result = 0.0;
						for( int i=0; i<1000; i++ )
							result += Math.expm1(Math.cosh(a * 2.0));
						return result;
					}
				});
		final Callable<List<Double>> c = parMap.f(longList);
		List<Double> o = c.call();
		System.out.println(o.length());

		assertEquals("Incorrect result of parallel map", 1000000, o.length());
	}

	/**
	 * Baseline serial map on a fj List
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPerformanceMap() throws Exception {
		final List<Double> longList = List.unfold(
				new F<Double, Option<P2<Double, Double>>>() {
					@Override
					public Option<P2<Double, Double>> f(Double a) {
						return a < 1000001.0 ? Option.some(P.p(a, a + 1.0))
								: Option.<P2<Double, Double>> none();
					}
				}, 1.0);
		assertEquals("Incorrect generated list length", 1000000,
				longList.length());
		final AtomicLong count = new AtomicLong(0);
		// Here Double is the target type of function f
		final List<Double> o = longList.map(new F<Double, Double>() {
					@Override
					public Double f(final Double a) {
						long n = count.incrementAndGet();
						if (n % 1000 == 0)
							System.out.println(n);
						double result = 0.0;
						for( int i=0; i<1000; i++ )
							result += Math.expm1(Math.cosh(a * 2.0));
						return result;
					}
				});
		System.out.println(o.length());

		assertEquals("Incorrect result of parallel map", 1000000, o.length());
	}

	/**
	 * Straight parallel map on a fj List
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPerformanceParallelMap2() throws Exception {
		final List<Double> longList = List.unfold(
				new F<Double, Option<P2<Double, Double>>>() {
					@Override
					public Option<P2<Double, Double>> f(Double a) {
						return a < 1001.0 ? Option.some(P.p(a, a + 1.0))
								: Option.<P2<Double, Double>> none();
					}
				}, 1.0);
		assertEquals("Incorrect generated list length", 1000,
				longList.length());
		final AtomicLong count = new AtomicLong(0);
		// Here Double is the target type of function f
		final F<List<Double>, Callable<List<Double>>> parMap = ParallelStrategy
				.parMap(strategy, new F<Double, Double>() {
					@Override
					public Double f(final Double a) {
						long n = count.incrementAndGet();
						if (n % 1000 == 0)
							System.out.println(n);
						double result = 0.0;
						for( int i=0; i<1000000; i++ )
							result += Math.expm1(Math.cosh(a * 2.0));
						return result;
					}
				});
		final Callable<List<Double>> c = parMap.f(longList);
		List<Double> o = c.call();
		System.out.println(o.length());

		assertEquals("Incorrect result of parallel map", 1000, o.length());
	}

	/**
	 * Baseline serial map on a fj List.
	 * This 4 times slower than the parallel version above.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPerformanceMap2() throws Exception {
		final List<Double> longList = List.unfold(
				new F<Double, Option<P2<Double, Double>>>() {
					@Override
					public Option<P2<Double, Double>> f(Double a) {
						return a < 1001.0 ? Option.some(P.p(a, a + 1.0))
								: Option.<P2<Double, Double>> none();
					}
				}, 1.0);
		assertEquals("Incorrect generated list length", 1000,
				longList.length());
		final AtomicLong count = new AtomicLong(0);
		// Here Double is the target type of function f
		final List<Double> o = longList.map(new F<Double, Double>() {
					@Override
					public Double f(final Double a) {
						long n = count.incrementAndGet();
						if (n % 1000 == 0)
							System.out.println(n);
						double result = 0.0;
						for( int i=0; i<1000000; i++ )
							result += Math.expm1(Math.cosh(a * 2.0));
						return result;
					}
				});
		System.out.println(o.length());

		assertEquals("Incorrect result of parallel map", 1000, o.length());
	}

	/**
	 * Straight parallel map on a fj List
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPerformanceParallelMap3() throws Exception {
		final List<Double> longList = List.unfold(
				new F<Double, Option<P2<Double, Double>>>() {
					@Override
					public Option<P2<Double, Double>> f(Double a) {
						return a < 101.0 ? Option.some(P.p(a, a + 1.0))
								: Option.<P2<Double, Double>> none();
					}
				}, 1.0);
		assertEquals("Incorrect generated list length", 100,
				longList.length());
		final AtomicLong count = new AtomicLong(0);
		// Here Double is the target type of function f
		final F<List<Double>, Callable<List<Double>>> parMap = ParallelStrategy
				.parMap(strategy, new F<Double, Double>() {
					@Override
					public Double f(final Double a) {
						long n = count.incrementAndGet();
						if (n % 1000 == 0)
							System.out.println(n);
						double result = 0.0;
						for( int i=0; i<10000000; i++ )
							result += Math.expm1(Math.cosh(a * 2.0));
						return result;
					}
				});
		final Callable<List<Double>> c = parMap.f(longList);
		List<Double> o = c.call();
		System.out.println(o.length());

		assertEquals("Incorrect result of parallel map", 100, o.length());
	}

	/**
	 * Baseline serial map on a fj List.
	 * This 4 times slower than the parallel version above.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPerformanceMap3() throws Exception {
		final List<Double> longList = List.unfold(
				new F<Double, Option<P2<Double, Double>>>() {
					@Override
					public Option<P2<Double, Double>> f(Double a) {
						return a < 101.0 ? Option.some(P.p(a, a + 1.0))
								: Option.<P2<Double, Double>> none();
					}
				}, 1.0);
		assertEquals("Incorrect generated list length", 100,
				longList.length());
		final AtomicLong count = new AtomicLong(0);
		// Here Double is the target type of function f
		final List<Double> o = longList.map(new F<Double, Double>() {
					@Override
					public Double f(final Double a) {
						long n = count.incrementAndGet();
						if (n % 1000 == 0)
							System.out.println(n);
						double result = 0.0;
						for( int i=0; i<10000000; i++ )
							result += Math.expm1(Math.cosh(a * 2.0));
						return result;
					}
				});
		System.out.println(o.length());

		assertEquals("Incorrect result of parallel map", 100, o.length());
	}

}
