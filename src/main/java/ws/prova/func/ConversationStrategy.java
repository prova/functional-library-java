package ws.prova.func;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import fj.F;

public class ConversationStrategy<A> {
	
	private F<Callable<A>, Future<A>> f;

	public ConversationStrategy(F<Callable<A>, Future<A>> f) {
		this.f = f;
	}

	public static <A> ConversationStrategy<A> strategy(
			final ConversationPool<A> xp) {
		return new ConversationStrategy(new F<Callable<A>, Future<A>>() {
			public Future<A> f(final Callable<A> p) {
				return null; //xp.submit(p);
			}
		});
	}

	public void assemble(final A a) {
		
	}

}
