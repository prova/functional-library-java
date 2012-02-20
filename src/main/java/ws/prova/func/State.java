package ws.prova.func;

public interface State<S, A> {

	public Pair<S, A> run(S s);

}
