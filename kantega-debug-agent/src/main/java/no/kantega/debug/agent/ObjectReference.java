package no.kantega.debug.agent;

public class ObjectReference {
	private final long id;
	private final String klass;
	public ObjectReference(final com.sun.jdi.ObjectReference source) {
		super();
		this.id = source.uniqueID();
		this.klass = source.type().name();
	}
	public long getId() {
		return id;
	}
	public String getKlass() {
		return klass;
	}
}
