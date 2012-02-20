package ws.prova.func;

import java.util.concurrent.Callable;

import fj.F;
import fj.F2;
import fj.Function;
import fj.data.List;

public class Callables {

	public static <A, B> F<Callable<A>, Callable<B>> fmap(final F<A, B> f) {
		return new F<Callable<A>, Callable<B>>() {
			public Callable<B> f(final Callable<A> a) {
				return new Callable<B>() {
					public B call() throws Exception {
						return f.f(a.call());
					}
				};
			}
		};
	}

	/**
	 * Unit function of the monad, i.e., a->Ma
	 * 
	 * @param a
	 * @return a Callable returning a
	 */
	public static <A> Callable<A> unit(final A a) {
		return new Callable<A>() {
			public A call() throws Exception {
				return a;
			}
		};
	}

	/**
	 * Wraps a given function's return value in a Callable. The Kleisli arrow
	 * for Callables.
	 * 
	 * @param f
	 *            The function whose return value to wrap in a Callable.
	 * @return The equivalent function whose return value is wrapped in a
	 *         Callable.
	 */
	public static <A, B> F<A, Callable<B>> unit(final F<A, B> f) {
		return new F<A, Callable<B>>() {
			public Callable<B> f(final A a) {
				return new Callable<B>() {
					public B call() {
						return f.f(a);
					}
				};
			}
		};
	}

	/**
	 * Bind function of the Callable monad with signature Ma->(a->Mb)->Mb. This
	 * allows us to compose the monadic computations to create infinite
	 * pipelines Mb->...->Mb->...Mb.
	 * 
	 * @param a
	 *            a monadic value Ma
	 * @param fm
	 *            a monadic function a->Mb, i.e., a function that either returns
	 *            B or throws E
	 * @return the monadic result of the computation Mb
	 */
	public static <A, B> Callable<B> bind(final Callable<A> a,
			final F<A, Callable<B>> fm) {
		return new Callable<B>() {
			public B call() throws Exception {
				return fm.f(a.call()).call();
			}
		};
	}

	/**
	 * Binds the given function to the values in the given Callables with a
	 * final join.
	 * 
	 * @param ca
	 *            A given Callable to bind the given function with.
	 * @param cb
	 *            A given Callable to bind the given function with.
	 * @param f
	 *            The function to apply to the values in the given Callables.
	 * @return A new Callable after performing the map, then final join.
	 */
	public static <A, B, C> Callable<C> bind(final Callable<A> ca,
			final Callable<B> cb, final F<A, F<B, C>> f) {
		return apply(cb, fmap(f).f(ca));
	}

	/**
	 * Promotes a function of arity-2 to a function on Callables.
	 * 
	 * @param f
	 *            The function to promote.
	 * @return a function of arity-2 promoted to map over Callables.
	 */
	public static <A, B, C> F<Callable<A>, F<Callable<B>, Callable<C>>> liftM2(
			final F<A, F<B, C>> f) {
		return Function.curry(new F2<Callable<A>, Callable<B>, Callable<C>>() {
			public Callable<C> f(final Callable<A> pa, final Callable<B> pb) {
				return bind(pa, pb, f);
			}
		});
	}

	/**
	 * Turns a List of Callables into a single Callable of a List.
	 * 
	 * @param as
	 *            The list of Callables to transform.
	 * @return a single Callable for the given List.
	 */
	public static <A> Callable<List<A>> sequence(final List<Callable<A>> as) {
		final F<Callable<A>, F<Callable<List<A>>, Callable<List<A>>>> lifter = liftM2(List.<A> cons());
		// The code below does not scale as the stack will be O(as.length) deep due to recursion in foldRight
		return as.foldRight(lifter, unit(List.<A> nil()));
	}

	/**
	 * Turns a List of Callables into a single Callable (a deferred synchronization) of a List.
	 * This actually calls the nested Callables and deals with exceptions via Partial result.
	 * Note that the actual computations behind these calls of call() are normally done elsewhere
	 * and the Future.get() inside the call just return the pre-computed results.
	 * 
	 * @param as A List of Callables
	 * @return A Callable of a List of real data (with computations already ensured via call())
	 */
	public static <A> Callable<List<A>> sequenceCallable(final List<Callable<A>> as) {
		return new Callable<List<A>>() {
			public List<A> call() throws Exception {
//				System.out.println("*Doing it for "+as.length()+" on "+Thread.currentThread().getName());
				return ParallelStrategy.map(as,
						new Partial<Callable<A>, A, Exception>() {
							@Override
							protected A run(Callable<A> c) throws Exception {
								return c.call();
							}
						});
			}
		};
	}
	
	/**
	 * Performs function application within a Callable (applicative functor pattern).
	 * 
	 * @param ca
	 *            The Callable to which to apply a function.
	 * @param cf
	 *            The Callable function to apply.
	 * @return A new Callable after applying the given Callable function to the first
	 *         argument.
	 */
	public static <A, B> Callable<B> apply(final Callable<A> ca,
			final Callable<F<A, B>> cf) {
		return bind(cf, new F<F<A, B>, Callable<B>>() {
			public Callable<B> f(final F<A, B> f) {
				return fmap(f).f(ca);
			}
		});
	}

	public static <A> Callable<A> join(final Callable<Callable<A>> a) {
		return bind(a, Function.<Callable<A>> identity());
	}

	/**
	 * Return a Kleisli arrow
	 * 
	 * @return a function that makes a normal function into a Kleisli arrow
	 */
	public static <A, B> F<F<A,B>, F<A,Callable<B>>> arrow() {
		return new F<F<A,B>, F<A,Callable<B>>>() {
			@Override
			public F<A, Callable<B>> f(final F<A, B> f) {
				return Callables.unit(f);
			}
		};
	}

}
