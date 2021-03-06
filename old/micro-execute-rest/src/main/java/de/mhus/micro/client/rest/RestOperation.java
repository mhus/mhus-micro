package de.mhus.micro.client.rest;

import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.shiro.subject.Subject;

import de.mhus.lib.core.IProperties;
import de.mhus.lib.core.MFile;
import de.mhus.lib.core.MLog;
import de.mhus.lib.core.MString;
import de.mhus.lib.core.config.IConfig;
import de.mhus.lib.core.config.JsonConfigBuilder;
import de.mhus.lib.core.config.MConfig;
import de.mhus.lib.core.io.http.MHttp;
import de.mhus.lib.core.io.http.MHttpClientBuilder;
import de.mhus.lib.core.operation.OperationDescription;
import de.mhus.lib.core.parser.StringCompiler;
import de.mhus.lib.core.aaa.Aaa;
import de.mhus.micro.api.MicroConst;
import de.mhus.micro.api.client.MicroOperation;
import de.mhus.micro.api.client.MicroResult;

public class RestOperation extends MLog  implements MicroOperation {

    private OperationDescription description;
    private HttpClient client;
    private String uri;
    private Object method;

    public RestOperation(OperationDescription description, HttpClient client) {
        this.description = description;
        this.client = client;
        this.uri = description.getLabels().get(MicroConst.REST_URL);
        this.method = description.getLabels().getOrDefault(MicroConst.REST_METHOD, MHttp.METHOD_POST);
    }

    @Override
    public MicroResult execute(IConfig arguments, IProperties properties) {
        HttpResponse res = null;
        try {
            Subject subject = Aaa.getSubject();
            
            if (uri.contains("{"))
                uri = StringCompiler.compile(uri).execute(arguments);
            // TODO different methods - GET POST PUT DELETE
            if (MHttp.METHOD_POST.equals(method)) {
                HttpPost post = new HttpPost(uri);
                if (subject.isAuthenticated()) {
                    String jwt = Aaa.createBearerToken(subject, null);
                    if (jwt != null)
                        post.addHeader("Authorization", "Bearer " + jwt);
                }
                // TODO different transport types - xml, form properties - pluggable?
                String argJson = IConfig.toPrettyJsonString(arguments);
                post.setEntity(new StringEntity(argJson, MString.CHARSET_UTF_8));
                res = client.execute(post);
            } else
            if (MHttp.METHOD_PUT.equals(method)) {
                HttpPut post = new HttpPut(uri);
                if (subject.isAuthenticated()) {
                    String jwt = Aaa.createBearerToken(subject, null);
                    if (jwt != null)
                        post.addHeader("Authorization", "Bearer " + jwt);
                }
                // TODO different transport types - xml, form properties - pluggable?
                String argJson = IConfig.toPrettyJsonString(arguments);
                post.setEntity(new StringEntity(argJson, MString.CHARSET_UTF_8));
                res = client.execute(post);
            } 
//            else
//            if (MHttp.METHOD_DELETE.equals(method)) {
//                HttpDelete post = new HttpDelete(uri);
//            
//                // TODO different transport types - xml, form properties - pluggable?
//                String argJson = IConfig.toPrettyJsonString(arguments);
//                post.setEntity(new StringEntity(argJson, MHttp.CONTENT_TYPE_JSON));
//                res = client.execute(post);
//            } else
//            if (MHttp.METHOD_GET.equals(method)) {
//                HttpGet post = new HttpGet(uri);
//
//                // TODO different transport types - xml, form properties - pluggable?
//                String argJson = IConfig.toPrettyJsonString(arguments);
//                tEntity(new StringEntity(argJson, MHttp.CONTENT_TYPE_JSON));
//                res = client.execute(post);
//            }

            if (res == null)
                return null;

            HttpEntity entry = res.getEntity();
            Header type = entry.getContentType();
            // TODO different return formats - xml, plain, stream - pluggable?
            if (type.getValue().startsWith(MHttp.CONTENT_TYPE_JSON)) {
                InputStream is = entry.getContent();
                IConfig resJson = new JsonConfigBuilder().read(is);

                if (properties != null)
                    properties.put("result", resJson);

                if (MHttp.getHeader(res, "Encapsulated", "").equals("result"))
                    return new MicroResult(true, resJson.getInt("rc", 0), resJson.getString("msg", ""), getDescription(), resJson.getObjectOrNull("result"));
                return new MicroResult(true, 0, "",getDescription(), resJson);
            }
            if (MHttp.CONTENT_TYPE_TEXT.equals(type.getValue())) {
                InputStream is = entry.getContent();
                IConfig resCfg = new MConfig();
                String resText = MFile.readFile(is);
                resCfg.setString(IConfig.NAMELESS_VALUE, resText);
                return new MicroResult(true, 0, "",getDescription(), resCfg);
            }
            
        } catch (Exception e) {
            log().e(e);
            return new MicroResult(false, -1, e.getMessage(), getDescription(), null);
        } finally {
            MHttpClientBuilder.close(res); // no NPE
        }
        return null;
    }

    @Override
    public OperationDescription getDescription() {
        return description;
    }

}
