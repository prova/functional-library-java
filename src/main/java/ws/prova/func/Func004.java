package ws.prova.func;


/**
 * Functional Java is used only minimally
 */
public class Func004 {

	public static void main(String[] args) {

		// PartialTimed<String, InputStream, Closeable, Exception> f1 =
		// new PartialTimed<String, InputStream, Closeable, Exception>(1000) {
		// @Override
		// protected Pair<Closeable,InputStream> run(String a) throws Exception
		// {
		// HttpURLConnection c = (HttpURLConnection) new
		// URL(a).openConnection();
		// c.setRequestMethod("GET");
		// c.setReadTimeout(timeout);
		// c.connect();
		// final InputStream is = c.getInputStream();
		// return Pairs.pair((Closeable)is, is);
		// }
		// };
		// PartialCloseable<InputStream,String, Closeable, Exception> f2 =
		// new PartialCloseable<InputStream,String, Closeable, Exception>() {
		// @Override
		// protected Pair<Closeable,String> run(InputStream a) throws Exception
		// {
		// final BufferedReader reader = new BufferedReader(new
		// InputStreamReader(a));
		// final String s = reader.readLine();
		// System.out.println(s);
		// return Pairs.pair((Closeable)reader, s);
		// }
		// };

		// ErrorHandler.extractFrom(
		// ThrowersCloseables.bind(f1.apply("http://www.cnn.com"), f2));
	}

}
