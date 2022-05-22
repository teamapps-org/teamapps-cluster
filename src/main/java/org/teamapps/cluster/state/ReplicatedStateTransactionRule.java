package org.teamapps.cluster.state;

public class ReplicatedStateTransactionRule {

	private final String subStateId;
	private final String identifier;
	private final TransactionCompareRule compareRule;
	private final int value;


	private ReplicatedStateTransactionRule(String subStateId, String identifier, TransactionCompareRule compareRule, int value) {
		this.subStateId = subStateId;
		this.identifier = identifier;
		this.compareRule = compareRule;
		this.value = value;
	}


	public String getSubStateId() {
		return subStateId;
	}

	public String getIdentifier() {
		return identifier;
	}

	public TransactionCompareRule getCompareRule() {
		return compareRule;
	}

	public int getValue() {
		return value;
	}
}
