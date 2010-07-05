package ahc;
import com.ning.http.client.RequestBuilder;
public class RequestBuilderWrapper {
    private RequestBuilder rb;
    public RequestBuilderWrapper(RequestBuilder rb) { this.rb = rb; }
    public RequestBuilderWrapper addHeader(String k, String v) {
        rb.addHeader(k, v);
        return this;}
    public RequestBuilderWrapper addParameter(String k, String v) {
        rb.addParameter(k, v);
        return this;}
    public RequestBuilderWrapper addQueryParameter(String k, String v) {
        rb.addQueryParameter(k,v);
        return this;}
    public RequestBuilder getRequestBuilder() { return rb; }}
