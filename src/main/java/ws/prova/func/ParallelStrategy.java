package ws.prova.func;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import fj.F;
import fj.F2;
import fj.Function;
import fj.data.List;
import fj.data.List.Buffer;

public final class ParallelStrategy<A> {

	private F<Callable<A>, Future<A>> f;

	private ParallelStrategy(final F<Callable<A>, Future<A>> f) {
		this.f = f;
	}

	public F<Callable<A>, Future<A>> f() {
		return f;
	}

	public static <A> ParallelStrategy<A> strategy(
			final F<Callable<A>, Future<A>> f) {
		return new ParallelStrategy<A>(f);
	}

	public static <A> ParallelStrategy<A> simpleThreadStrategy() {
		return strategy(new F<Callable<A>, Future<A>>() {
			public Future<A> f(final Callable<A> p) {
				final FutureTask<A> t = new FutureTask<A>(p);
				new Thread(t).start();
				return t;
			}
		});
	}

	public static <A> ParallelStrategy<A> executorStrategy(
			final ExecutorService s) {
		return strategy(new F<Callable<A>, Future<A>>() {
			public Future<A> f(final Callable<A> p) {
				return s.submit(p);
			}
		});
	}

	public <B> F<B, Future<A>> lift(final F<B, A> f) {
		final ParallelStrategy<A> self = this;
		return new F<B, Future<A>>() {
			public Future<A> f(final B b) {
				return self.f().f(new Callable<A>() {
					public A call() {
						return f.f(b);
					}
				});
			}
		};
	}

	public static <A> F<Future<A>, Callable<A>> obtain() {
		return new F<Future<A>, Callable<A>>() {
			public Callable<A> f(final Future<A> t) {
				return obtain(t);
			}
		};
	}

	public static <A> Callable<A> obtain(final Future<A> x) {
		return new Callable<A>() {
			public A call() throws Exception {
				return x.get();
			}
		};
	}

	/**
	 * Maps the given function across the supplied list with possible exceptions.
	 * 
	 * @param list
	 *            The list to map over.
	 * @param f
	 *            The Partial function to map across the supplied list.
	 * @return A new list after the given function has been applied to each
	 *         element.
	 * @throws E Exception thrown if any application fails
	 */
	public final static <A, B, E extends Exception> List<B> map(
			final List<A> list, final Partial<A, B, E> f) throws E {
		final Buffer<B> bs = Buffer.<B> empty();

		for (List<A> xs = list; xs.isNotEmpty(); xs = xs.tail()) {
			bs.snoc(f.run(xs.head()));
		}

		return bs.toList();
	}

	/**
	 * Function from a list of As to a deferred computation
	 *    that runs direct computations {@code F<A,B>} over this list of As
	 * @param s Parallel execution strategy
	 * @param f Direct computation to be run for each list element
	 * 
	 * @return Function as expected
	 */
	public static <A, B> F<List<A>, Callable<List<B>>> parMap(
			final ParallelStrategy<B> s, final F<A, B> f) {
		return new F<List<A>, Callable<List<B>>>() {
			@Override
			public Callable<List<B>> f(final List<A> as) {
				// The next line already starts the computations in the pool via the chosen strategy
				final List<Callable<B>> runningList = as.map(Function.compose(
						s.par(), Callables.unit(f)));
				// This represents a deferred synchronization of all included computations.
				// It replaces a horrible Callables.sequence method.
				return Callables.sequenceCallable(runningList);
			}
		};
	}

	/**
	 * Function from a list of As to a deferred computation
	 *    that runs deferred computations {@code F<A,Callable<B>>} over this list of As
	 * @param s Parallel execution strategy
	 * @param f Direct computation to be run for each list element
	 * 
	 * @return Function as expected
	 */
	public static <A, B> F<List<A>, Callable<List<B>>> parMapCallable(
			final ParallelStrategy<B> s, final F<A, Callable<B>> f) {
		return new F<List<A>, Callable<List<B>>>() {
			@Override
			public Callable<List<B>> f(final List<A> as) {
				// The next line already starts the computations in the pool via the chosen strategy
				final List<Callable<B>> runningList = as.map(Function.compose(
						s.par(), f));
				// This represents a deferred synchronization of all included computations.
				// It replaces a horrible Callables.sequence method.
				return Callables.sequenceCallable(runningList);
			}
		};
	}

	public static <A, B> F<List<A>, Callable<List<B>>> parFlatMap(
			final ParallelStrategy<List<B>> s, final F<A, List<B>> f) {
		return new F<List<A>, Callable<List<B>>>() {
			@Override
			public Callable<List<B>> f(List<A> as) {
				return Callables.fmap(List.<B> join()).f(parMap(s, f).f(as));
			}
		};
	}

	public static <A, B> F<List<A>, Callable<List<B>>> parFlatMapCallable(
			final ParallelStrategy<List<B>> s, final F<A, Callable<List<B>>> f) {
		return new F<List<A>, Callable<List<B>>>() {
			@Override
			public Callable<List<B>> f(List<A> as) {
				return Callables.fmap(List.<B> join()).f(
						parMapCallable(s, f).f(as));
			}
		};
	}

	public static <A, B, C> F2<List<A>, List<B>, Callable<List<C>>> parZipWith(
			final ParallelStrategy<C> s, final F2<A, B, C> f) {
		return new F2<List<A>, List<B>, Callable<List<C>>>() {
			public Callable<List<C>> f(final List<A> as, final List<B> bs) {
				// The next line already starts the computations in the pool via the chosen strategy
				final List<Callable<C>> runningList = as.zipWith(bs,
						Function.compose(Callables.<B, C> arrow(), f.curry()))
						.map(s.par());
				// This represents a deferred synchronization of all included computations.
				// It replaces a horrible Callables.sequence method.
				return Callables.sequenceCallable(runningList);
			}
		};
	}

	public F<Callable<A>, Callable<A>> par() {
		return Function.compose(ParallelStrategy.<A> obtain(), f());
	}

	/**
	 * Apply the strategy to the given Callable computation from B
	 * 
	 * @param a
	 *            A Callable computation from B to evaluate according to this
	 *            strategy.
	 * @return A parallel version of this computation
	 */
	public <B> F<B, Callable<A>> par(final F<B, Callable<A>> f) {
		return Function.compose(par(), f);
	}

}
