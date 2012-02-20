package ws.prova.func;

import java.io.Closeable;
import java.io.IOException;

public class ErrorHandler {

	public static <A, E extends Throwable> A extractFrom(Thrower<A, E> f) {
		try {
			return f.extract();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
    
    public static <A, C extends Closeable, E extends Throwable> A extractFrom(ThrowerCloseable<A, C, E> f) {

        Pair<C, A> result = null;
        try {
            result = f.extract();
            return result.second();
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        } finally {
            if (result!=null) {
                final Closeable c = result.first();
                if (c!=null) {
                    System.out.println("Closing "+c.getClass().getSimpleName());
                    try {
                        c.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }
	}

}
