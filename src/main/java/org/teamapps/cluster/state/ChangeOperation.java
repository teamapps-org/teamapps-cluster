package org.teamapps.cluster.state;

public enum ChangeOperation {

	ADD(1),
	UPDATE(2),
	REMOVE(3),
	SET(4),
	REMOVE_ALL(5),
	SEND_AND_FORGET(6),

	;
	private final int id;

	ChangeOperation(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public static ChangeOperation getById(int id) {
		return switch (id) {
			case 1 -> ADD;
			case 2 -> UPDATE;
			case 3 -> REMOVE;
			case 4 -> SET;
			case 5 -> REMOVE_ALL;
			case 6 -> SEND_AND_FORGET;
			default -> null;
		};
	}
}
