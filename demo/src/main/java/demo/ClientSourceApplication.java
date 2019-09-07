package demo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import feign.*;
import demo.api.ClientSourceAppApi;
import feign.codec.StringDecoder;
import feign.form.ContentType;
import feign.form.FormEncoder;
import feign.hystrix.HystrixFeign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import demo.log.ConsoleLogger;
import feign.ribbon.LBClient;
import feign.ribbon.LBClientFactory;
import feign.ribbon.RibbonClient;
import feign.slf4j.Slf4jLogger;
import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

public class ClientSourceApplication {

//    @Test
//    public void queryApp() {
//        ClientSourceAppApi clientSourceAppApi = Feign.builder()
//                .encoder(new FormEncoder())
//                .decoder(new JacksonDecoder())
//                .target(ClientSourceAppApi.class, "http://localhost:8081");
//
//        Object result = clientSourceAppApi.queryApp("测试");
//
//        System.out.println(JSON.toJSONString(result));
//    }
//
//    @Test
//    public void executeGetRequest() {
//        ClientSourceAppApi clientSourceAppApi = Feign.builder()
//                .encoder(new FormEncoder())
//                .decoder(new StringDecoder())
//                .target( Target.EmptyTarget.create( ClientSourceAppApi.class ) ) ;
//
//        Map<String,Object> queryParam = Maps.newHashMap() ;
//        queryParam.put("name", "测试-GET") ;
//
//        Map<String,String> headerMap = Maps.newHashMap() ;
//        headerMap.put("Content-Type", "application/x-www-form-urlencoded") ;
//
//        String result = clientSourceAppApi.executeGetRequest("/api/client/list" , queryParam , headerMap );
//
//        result = StringEscapeUtils.unescapeJson( result ) ;
//
//        System.out.println( result );
//
//        JSONArray jsonObject = JSON.parseObject( result , JSONArray.class ) ;
//
//        System.out.println(JSON.toJSONString(jsonObject));
//    }

    @Test
    public void executePostJsonRequest() throws IOException {

        ClientSourceAppApi clientSourceAppApi = Feign.builder()
                .logger( new ConsoleLogger() )
                .logLevel( Logger.Level.FULL )
                .encoder( new JacksonEncoder() )
                .decoder( new StringDecoder() )
                .target( ClientSourceAppApi.class , "http://dyjy.dtdjzx.gov.cn" ) ;

        Map<String,Object> queryParam = Maps.newHashMap() ;
        queryParam.put("courseId", "2982960448340992") ;
        queryParam.put("specialId", "2982776468030464") ;

        Map<String,String> headerMap = Maps.newHashMap() ;
        headerMap.put("Content-Type", "application/json") ;
        headerMap.put("Cache-Control" , "no-cache") ;
        headerMap.put("Connection" , "keep-alive") ;

        Response response = clientSourceAppApi.executePostJsonRequest("/bintang/findCourseDetails" , queryParam , headerMap ) ;

        String resultText = Util.toString( response.body().asReader() ) ;

        System.out.println( StringEscapeUtils.unescapeJson( resultText ) ) ;
    }

    @Test
    public void executePostFormRequest() {

        ClientSourceAppApi clientSourceAppApi = Feign.builder()
                .logger( new ConsoleLogger() )
                .logLevel( Logger.Level.FULL )
                .encoder(new FormEncoder())
                .decoder( new StringDecoder() )
                .target( ClientSourceAppApi.class , "http://localhost:8081" ) ;

        Map<String,Object> queryParam = Maps.newHashMap() ;
        queryParam.put("name" , "post");

        Map<String,String> headerMap = Maps.newHashMap() ;
        headerMap.put("Content-Type", "application/x-www-form-urlencoded") ;
        headerMap.put("Cache-Control" , "no-cache") ;
        headerMap.put("Connection" , "keep-alive") ;

        Response result = clientSourceAppApi.executePostFormRequest("/api/client/list" , queryParam , headerMap );

//        result = StringEscapeUtils.unescapeJson( result ) ;
//
//        System.out.println( result );
    }

//    @Test
//    public void executeRibbon() {
//
////        RibbonClient.builder().lbClientFactory(new LBClientFactory() {
////            @Override
////            public LBClient create(String clientName) {
////                return null;
////            }
////        })
//
//        ClientSourceAppApi clientSourceAppApi = Feign.builder()
////                .client(RibbonClient.create())
//                .encoder(new FormEncoder())
//                .decoder(new StringDecoder())
//                .target( ClientSourceAppApi.class , "http://localhost:8081/" );
//
//        Map<String,Object> queryParam = Maps.newHashMap() ;
//        queryParam.put("name", "测试-POST") ;
//
//        Map<String,String> headerMap = Maps.newHashMap() ;
//        headerMap.put("Content-Type", "application/x-www-form-urlencoded") ;
//
//        String result = clientSourceAppApi.executePostRequest( "/api/client/list" , queryParam , headerMap );
//
//        result = StringEscapeUtils.unescapeJson( result ) ;
//
//        System.out.println( result );
//    }
//
//    @Test
//    public void executeHystrix() {
//
//        ClientSourceAppApi clientSourceAppApi = HystrixFeign.builder()
//                .encoder(new FormEncoder())
//                .decoder(new StringDecoder())
//                .target( ClientSourceAppApi.class , "http://localhost:8081/" );
//
//        Map<String,Object> queryParam = Maps.newHashMap() ;
//        queryParam.put("name", "测试-POST") ;
//
//        Map<String,String> headerMap = Maps.newHashMap() ;
//        headerMap.put("Content-Type", "application/x-www-form-urlencoded") ;
//
//        String result = clientSourceAppApi.executePostRequest( "/api/client/list" , queryParam , headerMap );
//
//        result = StringEscapeUtils.unescapeJson( result ) ;
//
//        System.out.println( result );
//    }

}
