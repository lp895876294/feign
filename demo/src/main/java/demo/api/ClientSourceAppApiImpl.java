package demo.api;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import feign.Request;
import feign.Response;
import rx.Observable;

import java.util.Map;

public class ClientSourceAppApiImpl implements ClientSourceAppApi {

    @Override
    public String executeGetRequest(String uri, Object queryParam, Map<String, String> headerMap) {
        return null;
    }

    @Override
    public Observable<Response> executeHystrixPostJsonRequest(String uri, Object queryParam, Map<String, String> headerMap) {
        Response response = executePostJsonRequest( uri , queryParam , headerMap ) ;
        return Observable.just( response ) ;
    }

    @Override
    public Response executePostJsonRequest(String uri, Object queryParam, Map<String, String> headerMap) {
        System.out.println("执行失败");
        return Response.builder()
                .request( Request.create( Request.HttpMethod.GET , uri , Maps.newHashMap(), null ) )
                .body( uri + " execute failed" , Charsets.UTF_8)
                .build() ;
    }

    @Override
    public Response executePostFormRequest(String uri, Map<String, Object> queryParam, Map<String, String> headerMap) {
        return null;
    }
}
