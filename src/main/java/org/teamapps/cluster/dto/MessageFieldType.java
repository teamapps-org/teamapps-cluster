package org.teamapps.cluster.dto;

public enum MessageFieldType {

	OBJECT(1),
	OBJECT_SINGLE_REFERENCE(2),
	OBJECT_MULTI_REFERENCE(3),
	BOOLEAN(4),
	BYTE(5),
	INT(6),
	LONG(7),
	FLOAT(8),
	DOUBLE(9),
	STRING(10),
	BITSET(11),
	BYTE_ARRAY(12),
	INT_ARRAY(13),
	LONG_ARRAY(14),
	FLOAT_ARRAY(15),
	DOUBLE_ARRAY(16),
	STRING_ARRAY(17),
	FILE(18),
	ENUM(19),
	;

	private final int id;

	MessageFieldType(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public static MessageFieldType getById(int id) {
		return switch (id) {
			case 1 -> OBJECT;
			case 2 -> OBJECT_SINGLE_REFERENCE;
			case 3 -> OBJECT_MULTI_REFERENCE;
			case 4 -> BOOLEAN;
			case 5 -> BYTE;
			case 6 -> INT;
			case 7 -> LONG;
			case 8 -> FLOAT;
			case 9 -> DOUBLE;
			case 10 -> STRING;
			case 11 -> BITSET;
			case 12 -> BYTE_ARRAY;
			case 13 -> INT_ARRAY;
			case 14 -> LONG_ARRAY;
			case 15 -> FLOAT_ARRAY;
			case 16 -> DOUBLE_ARRAY;
			case 17 -> STRING_ARRAY;
			case 18 -> FILE;
			case 19 -> ENUM;
			default -> null;
		};
	}
}
