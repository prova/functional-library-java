package ws.prova.func;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import fj.Effect;
import fj.F;
import fj.F2;
import fj.F3;
import fj.P;
import fj.P2;
import fj.data.List;
import fj.data.List.Buffer;

public class ConversationStrategy<A> {
	
//	AtomicLong xid = new AtomicLong();
	
	private F2<Long, Callable<A>, Future<A>> f;

	public ConversationStrategy(F2<Long, Callable<A>, Future<A>> f) {
		this.f = f;
	}

	public F2<Long, Callable<A>, Future<A>> f() {
		return f;
	}

	public static <A> ConversationStrategy<A> strategy(
			final ConversationPool<A> xp) {
		return new ConversationStrategy<A>(new F2<Long, Callable<A>, Future<A>>() {
			public Future<A> f(final Long xid, final Callable<A> p) {
				return xp.submit(xid, p);
			}
		});
	}

	public final static <B> List<B> arrayToList(
			final B[] arr) {
		final Buffer<B> bs = Buffer.<B> empty();

		for (int i=0; i<arr.length; i++) {
			bs.snoc(arr[i]);
		}

		return bs.toList();
	}

	/**
	 * Run parallel tasks on conversation threads, assemble results and ship via the supplied continuation.
	 * At the moment call(s) in Callables are not really used.
	 * @param strategy
	 * @param fx
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <A, B> F3<Long, List<A>, Effect<List<B>>, Callable<List<B>>> parMap(
			final ConversationStrategy<B> strategy,
			final F<A, B> fx) {
		return new F3<Long, List<A>, Effect<List<B>>, Callable<List<B>>>() {
			@Override
			public Callable<List<B>> f(final Long xid0, final List<A> as, final Effect<List<B>> cont) {
				final AtomicInteger count = new AtomicInteger(as.length());
				final B[] bs = (B[]) new Object[as.length()];
				final F2<P2<Long,B>, Long, Callable<B>> assembler = new F2<P2<Long,B>, Long, Callable<B>>() {
//					int count = as.length();
					@Override
					public Callable<B> f(final P2<Long,B> e, final Long xid) {
						return new Callable<B>() {
							public B call() throws Exception {
								final int now = e._1().intValue();
//								System.out.println(now);
								bs[now] = e._2();
								if( count.decrementAndGet()==0 ) {
									cont.e(arrayToList(bs));
								}
								return null;
							}							
						};
					}
				};
				final F<Callable<B>, Future<B>> f0 = strategy.f().f(xid0);
				final F2<A, Long, Callable<B>> fcb = new F2<A, Long, Callable<B>>() {
					@Override
					public Callable<B> f(final A a, final Long xid) {
						return new Callable<B>() {
							public B call() throws Exception {
								B b = fx.f(a);
//								f0.f(assembler.f(P.p(xid,b),xid0));
								assembler.f(P.p(xid,b),xid0).call();
//								System.out.println("yeah");
								return null;
							}
						};
					}
				};
				as.foreach(new Effect<A>() {
					long xid = 0;
					@Override
					public void e(A a) {
						strategy.f().f(xid).f(fcb.f(a,xid));
						xid++;
					}
				});
				return null;
			}			
		};
	}

}
