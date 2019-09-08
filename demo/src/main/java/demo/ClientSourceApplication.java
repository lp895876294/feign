package demo;

import com.google.common.collect.Maps;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfigKey;
import com.netflix.config.ConfigurationManager;
import com.netflix.loadbalancer.DynamicServerListLoadBalancer;
import demo.api.ClientSourceAppApi;
import demo.log.ConsoleLogger;
import demo.stats.AppLoadBalancerStats;
import feign.Feign;
import feign.Logger;
import feign.Response;
import feign.Util;
import feign.codec.StringDecoder;
import feign.form.FormEncoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import feign.ribbon.RibbonClient;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

public class ClientSourceApplication {

    public static final int executeNum = 1 ;

    static {
        // 覆盖ribbon默认的配置属性
        Map<IClientConfigKey,String> ribbonDefaultProperties = Maps.newHashMap() ;
        ribbonDefaultProperties.put(CommonClientConfigKey.NFLoadBalancerStatsClassName , AppLoadBalancerStats.class.getName() ) ;
        // 默认的负责均衡类，默认使用 DynamicServerListLoadBalancer
        ribbonDefaultProperties.put(CommonClientConfigKey.NFLoadBalancerClassName , DynamicServerListLoadBalancer.class.getName() ) ;

        for (Map.Entry<IClientConfigKey, String> entry : ribbonDefaultProperties.entrySet()) {
            // 设置配置的key
            String configKey = DefaultClientConfigImpl.DEFAULT_PROPERTY_NAME_SPACE + "." + entry.getKey().key() ;
            System.setProperty( configKey , entry.getValue() ) ;

            System.out.println( configKey + " = " + entry.getValue() );
        }

    }

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

        for (int i = 0; i < executeNum; i++) {
            ClientSourceAppApi clientSourceAppApi = Feign.builder()
                    .logger(new ConsoleLogger())
                    .logLevel(Logger.Level.FULL)
                    .client(new OkHttpClient())
                    .encoder(new JacksonEncoder())
                    .decoder(new StringDecoder())
                    .target(ClientSourceAppApi.class, "http://dyjy.dtdjzx.gov.cn");

            Map<String, Object> queryParam = Maps.newHashMap();
            queryParam.put("courseId", "2982960448340992");
            queryParam.put("specialId", "2982776468030464");

            Map<String, String> headerMap = Maps.newHashMap();
            headerMap.put("Content-Type", "application/json");
            headerMap.put("Cache-Control", "no-cache");
            headerMap.put("Connection", "keep-alive");

            Response response = clientSourceAppApi.executePostJsonRequest("/bintang/findCourseDetails", queryParam, headerMap);

            String resultText = Util.toString(response.body().asReader());

            System.out.println(StringEscapeUtils.unescapeJson(resultText));
        }
    }

    @Test
    public void ribbonExecutePostJsonRequest() throws IOException {
        String ribbionName = "dyjy";

        String listServersProperty = ribbionName + ".ribbon.listOfServers";

        // ribbon全局配置属性，可以使用全局的配置文件
        // 设置ribbon连接的服务端地址列表，配置属性的加载接口使用commons-configuration配置
        AbstractConfiguration abstractConfiguration = ConfigurationManager.getConfigInstance();
        abstractConfiguration.setProperty( listServersProperty, "dyjy.dtdjzx.gov.cn");
//        abstractConfiguration.setProperty( listServersProperty , "10.254.23.134:6311,10.254.23.135:6311,10.254.23.136:6311,10.254.23.137:6311" ) ;

        for (int i = 0; i < executeNum; i++) {

            RibbonClient ribbonClient = RibbonClient.builder().delegate(new OkHttpClient()).build();

            ClientSourceAppApi clientSourceAppApi = Feign.builder()
                    .logger(new ConsoleLogger())
                    .logLevel(Logger.Level.FULL)
                    .client(ribbonClient)
                    .encoder(new JacksonEncoder())
                    .decoder(new StringDecoder())
                    .target(ClientSourceAppApi.class, "http://" + ribbionName);

            Map<String, Object> queryParam = Maps.newHashMap();
            queryParam.put("courseId", "2982960448340992");
            queryParam.put("specialId", "2982776468030464");

            Map<String, String> headerMap = Maps.newHashMap();
            headerMap.put("Content-Type", "application/json");
            headerMap.put("Cache-Control", "no-cache");
            headerMap.put("Connection", "keep-alive");

            Response response = clientSourceAppApi.executePostJsonRequest("/bintang/findCourseDetails", queryParam, headerMap);

//        String resultText = Util.toString( response.body().asReader() ) ;
//
//        System.out.println( StringEscapeUtils.unescapeJson( resultText ) ) ;

        }
    }

    @Test
    public void executePostFormRequest() {

        ClientSourceAppApi clientSourceAppApi = Feign.builder()
                .logger(new ConsoleLogger())
                .logLevel(Logger.Level.FULL)
                .encoder(new FormEncoder())
                .decoder(new StringDecoder())
                .target(ClientSourceAppApi.class, "http://localhost:8081");

        Map<String, Object> queryParam = Maps.newHashMap();
        queryParam.put("name", "post");

        Map<String, String> headerMap = Maps.newHashMap();
        headerMap.put("Content-Type", "application/x-www-form-urlencoded");
        headerMap.put("Cache-Control", "no-cache");
        headerMap.put("Connection", "keep-alive");

        Response result = clientSourceAppApi.executePostFormRequest("/api/client/list", queryParam, headerMap);

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
