package ws.prova.func;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ConversationPool<A> {

	private final ExecutorService[] partitionedPool = new ExecutorService[32];

	private final Map<Long, Integer> threadId2Index = new HashMap<Long, Integer>(
			32);

	public ConversationPool() {
		for (int i = 0; i < partitionedPool.length; i++) {
			final int index = i;
			this.partitionedPool[i] =
				new ThreadPoolExecutor(1, 1,
					    0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueueWithPut<Runnable>(81920),
					new ThreadFactory() {

						public Thread newThread(Runnable r) {
							Thread th = null;
							th = new Thread(r);
							threadId2Index.put(th.getId(), index);
							th.setName("Async-"+(index+1));
							return th;
						}

					});
		}
	}

	public Future<A> submit( final Long xid, final Callable<A> task ) {
		return partitionedPool[(int) (xid % partitionedPool.length)].submit(task);
	}
	
	private class ArrayBlockingQueueWithPut<E> extends ArrayBlockingQueue<E> {

		private static final long serialVersionUID = -3392821517081645923L;

		public ArrayBlockingQueueWithPut(int capacity) {
			super(capacity);
		}

		public boolean offer(E e) {
	        try {
				put(e);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
	        return true;
	    }
	}
	
}
