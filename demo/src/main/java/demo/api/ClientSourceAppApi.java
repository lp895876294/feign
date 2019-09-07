package demo.api;

import feign.*;

import java.net.URI;
import java.util.Map;

public interface ClientSourceAppApi {

//    @RequestLine("GET /api/client/list")
//    @Headers("Content-Type: application/x-www-form-urlencoded")
//    Object queryApp(@Param("name") String name);
//
//    @RequestLine("GET {uri}")
//    String executeGetRequest( @Param("uri") String uri , @QueryMap Map<String,Object> queryParam , @HeaderMap Map<String,String> headerMap) ;

    @RequestLine("GET {uri}")
    String executeGetRequest( @Param("uri") String uri , @QueryMap Map<String,Object> queryParam ,
                                   @HeaderMap Map<String,String> headerMap) ;

    @RequestLine("POST {uri}")
    Response executePostJsonRequest( @Param("uri") String uri , Map<String,Object> queryParam ,
                               @HeaderMap Map<String,String> headerMap) ;

    @RequestLine("POST {uri}")
    Response executePostFormRequest( @Param("uri") String uri , @QueryMap Map<String,Object> queryParam ,
                                   @HeaderMap Map<String,String> headerMap) ;





}
