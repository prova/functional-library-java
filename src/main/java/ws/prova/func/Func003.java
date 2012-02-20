package ws.prova.func;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *
 */
public class Func003 {

    public static void main(String[] args) {
        
        PartialCloseable<String, InputStream, Closeable, Exception> f1 = new PartialCloseable<String, InputStream, Closeable, Exception>() {
            @Override
            protected Pair<Closeable,InputStream> run(String a) throws Exception {
                HttpURLConnection c = (HttpURLConnection) new URL(a).openConnection();
                c.setRequestMethod("GET");
                c.setReadTimeout(10000);
                c.connect();
                final InputStream is = c.getInputStream();
                return Pairs.pair((Closeable)is, is);
            }
        };
        PartialCloseable<InputStream,String, Closeable, Exception> f2 = new PartialCloseable<InputStream,String, Closeable, Exception>() {
            @Override
            protected Pair<Closeable,String> run(InputStream a) throws Exception {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(a));
                final String s = reader.readLine();
                System.out.println(s);
                if( s.length()!=0 )
                	throw new IOException();
                return Pairs.pair((Closeable)reader, s);
            }
        };

        ErrorHandler.extractFrom(
                ThrowersCloseables.bind(f1.apply("http://www.cnn.com"), f2));
    }


}
