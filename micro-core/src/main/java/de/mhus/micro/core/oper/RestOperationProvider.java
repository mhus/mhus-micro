package de.mhus.micro.core.oper;

import de.mhus.lib.core.MProperties;
import de.mhus.lib.core.io.http.MHttp;
import de.mhus.lib.core.operation.OperationDescription;
import de.mhus.micro.core.api.Micro;

public class RestOperationProvider extends OperationProvider {

	private String baseUrl = "http://${host}/rest/public/operation/";
	private String host = "localhost:8080";
	
	@Override
	protected OperationDescription cloneDescription(OperationDescription description) {
		OperationDescription out = new OperationDescription(description);
		((MProperties)out.getLabels()).setString(Micro.LABEL_PROTO, Micro.PROTO_REST);
		((MProperties)out.getLabels()).setString(Micro.REST_METHOD, MHttp.METHOD_POST);
		((MProperties)out.getLabels()).setString(Micro.REST_URL, baseUrl + description.getPathVersion() ); // use UUID?
		((MProperties)out.getLabels()).setString(Micro.REST_HOST, host);
		
		return out;
	}

}
