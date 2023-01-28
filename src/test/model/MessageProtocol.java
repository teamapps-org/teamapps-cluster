import org.teamapps.message.protocol.message.MessageDefinition;
import org.teamapps.message.protocol.message.MessageModelCollection;
import org.teamapps.message.protocol.model.EnumDefinition;
import org.teamapps.message.protocol.model.ModelCollection;
import org.teamapps.message.protocol.model.ModelCollectionProvider;
import org.teamapps.message.protocol.service.ServiceProtocol;

public class MessageProtocol implements ModelCollectionProvider {
	@Override
	public ModelCollection getModelCollection() {
		MessageModelCollection modelCollection = new MessageModelCollection("clusterTestMessageProtocol", "org.teamapps.cluster.message.test.protocol", 1);

		MessageDefinition testMethodRequest = modelCollection.createModel("testMethodRequest", "test.testMethodRequest");
		MessageDefinition testMethodResult = modelCollection.createModel("testMethodResult", "test.testMethodResult");

		testMethodRequest.addInteger("executionDelaySeconds", 1);
		testMethodRequest.addBoolean("throwException", 2);
		testMethodRequest.addString("answerPayload", 3);
		testMethodRequest.addString("failIfNodeId", 4);

		testMethodResult.addString("answerPayload", 1);
		testMethodResult.addString("executingNode", 2);

		ServiceProtocol clusterTest = modelCollection.createService("clusterTest");
		clusterTest.addMethod("testServiceMethod", testMethodRequest, testMethodResult);

		return modelCollection;
	}
}
