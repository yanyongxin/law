package sftrack;

public class LexEntityNotFoundException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String entity;

	public LexEntityNotFoundException(String entityName) {
		entity = entityName;
	}

	@Override
	public String getMessage() {
		return "Entity not found in knowledge base: " + entity;
	}
}
