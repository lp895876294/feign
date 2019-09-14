package demo;

import com.google.common.collect.Maps;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfigKey;
import com.netflix.config.ConfigurationManager;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.strategy.properties.HystrixDynamicProperties;
import com.netflix.hystrix.strategy.properties.HystrixDynamicPropertiesSystemProperties;
import com.netflix.loadbalancer.DynamicServerListLoadBalancer;
import demo.api.ClientSourceAppApi;
import demo.api.ClientSourceAppApiImpl;
import demo.log.ConsoleLogger;
import demo.stats.AppLoadBalancerStats;
import feign.*;
import feign.codec.StringDecoder;
import feign.form.FormEncoder;
import feign.hystrix.FallbackFactory;
import feign.hystrix.HystrixFeign;
import feign.hystrix.SetterFactory;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import feign.ribbon.RibbonClient;
import okhttp3.ConnectionPool;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.Test;
import rx.Observable;
import rx.Subscriber;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientSourceApplication {

    public static final int executeNum = 100000 ;

    public static volatile AtomicInteger num = new AtomicInteger(0) ;

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
//        abstractConfiguration.setProperty( listServersProperty, "localhost,dyjy.dtdjzx.gov.cn");
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

            try{
                Response response = clientSourceAppApi.executePostJsonRequest("/bintang/findCourseDetails", queryParam, headerMap);

                String resultText = Util.toString( response.body().asReader() ) ;

                System.out.println( StringEscapeUtils.unescapeJson( resultText ) ) ;
            }catch( Exception e ){
                System.out.println("异常");
                e.printStackTrace();
            }

        }
    }

    private static AtomicInteger atomicInteger = new AtomicInteger(0) ;

    @Test
    public void hystrixRibbonExecutePostJsonRequest() throws IOException {
        String ribbionName = "dyjy";

        String listServersProperty = ribbionName + ".ribbon.listOfServers";

        // ribbon全局配置属性，可以使用全局的配置文件
        // 设置ribbon连接的服务端地址列表，配置属性的加载接口使用commons-configuration配置
        AbstractConfiguration abstractConfiguration = ConfigurationManager.getConfigInstance();
        abstractConfiguration.setProperty( listServersProperty, "dyjy.dtdjzx.gov.cn");
//        abstractConfiguration.setProperty( listServersProperty, "localhost,dyjy.dtdjzx.gov.cn");
//        abstractConfiguration.setProperty( listServersProperty , "10.254.23.134:6311,10.254.23.135:6311,10.254.23.136:6311,10.254.23.137:6311" ) ;
//        abstractConfiguration.setProperty( listServersProperty , "10.254.23.41:6310" ) ;

        // 使用系统属性加载配置
        System.setProperty("hystrix.plugin."+HystrixDynamicProperties.class.getSimpleName() +".implementation" ,
                HystrixDynamicPropertiesSystemProperties.class.getName()) ;

        System.setProperty("hystrix.threadpool.default.allowMaximumSizeToDivergeFromCoreSize" , "true") ;
        System.setProperty("hystrix.threadpool.default.coreSize" , "200") ;
        System.setProperty("hystrix.threadpool.default.maximumSize" , "200") ;
        System.setProperty("hystrix.threadpool.default.maxQueueSize" , "100000") ;
        System.setProperty("hystrix.threadpool.default.queueSizeRejectionThreshold" , "100000") ;
//        System.setProperty("hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds" , "10000") ;
        System.setProperty("hystrix.command.default.execution.timeout.enabled" , "false") ;

        ConnectionPool pool = new ConnectionPool(500, 5, TimeUnit.MINUTES);

        okhttp3.OkHttpClient okHttpClient = new okhttp3.OkHttpClient.Builder()
                .connectionPool( pool )
                .build() ;

        //使用单例连接客户端
        OkHttpClient feignHttpClient = new OkHttpClient( okHttpClient ) ;

        RibbonClient ribbonClient = RibbonClient.builder()
                .delegate( feignHttpClient )
                .build() ;

        Long startTime = System.currentTimeMillis() ;

        for (int i = 0; i < executeNum; i++) {

            ClientSourceAppApi clientSourceAppApi = HystrixFeign.builder()
                    .options( new Request.Options(10 * 1000, 0) )
                    .logger(new ConsoleLogger())
                    .logLevel(Logger.Level.FULL)
                    .client(ribbonClient)
                    .encoder(new JacksonEncoder())
                    .decoder(new StringDecoder())
                    .setterFactory(new SetterFactory() {
                        @Override
                        public HystrixCommand.Setter create(Target<?> target, Method method) {

//                            System.out.println( "target.url() = " + target.url() );

                            return HystrixCommand.Setter
                                    .withGroupKey(HystrixCommandGroupKey.Factory.asKey("testgroup"))
                                    .andCommandKey(HystrixCommandKey.Factory.asKey("testkey"));
                        }
                    })
                    .target(new AppTarget<ClientSourceAppApi>("dtdj", ClientSourceAppApi.class, "http://" + ribbionName),
                            new FallbackFactory<ClientSourceAppApi>() {
                                @Override
                                public ClientSourceAppApi create(Throwable cause) {
                                    System.out.println("exception -> " + cause.getMessage()) ;
                                    return new ClientSourceAppApiImpl() ;
                                }
                            }) ;

            Map<String, Object> queryParam = Maps.newHashMap();
            queryParam.put("courseId", "2982960448340992");
            queryParam.put("specialId", "2982776468030464");

            Map<String, String> headerMap = Maps.newHashMap();
            headerMap.put("Content-Type", "application/json");
            headerMap.put("Cache-Control", "no-cache");
            headerMap.put("Connection", "keep-alive");

            try{

                Observable<Response> observable = clientSourceAppApi.executeHystrixPostJsonRequest(
                        "/bintang/findCourseDetails", queryParam, headerMap ) ;

                observable.subscribe(new Subscriber<Response>() {
                    @Override
                    public void onCompleted() {
                        System.out.println("complete============" + num.incrementAndGet());
                        if( num.get() >= executeNum - 1 ){
                            System.out.println( "耗时: " + ( (System.currentTimeMillis() - startTime)/1000 ) +"s ");
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Response response) {
                        String resultText = null;
                        try {
                            resultText = Util.toString( response.body().asReader() );
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
//                        System.out.println( resultText );
                    }
                }) ;
            }catch( Exception e ){
                System.out.println("异常");
                e.printStackTrace();
            }
        }

        System.out.println( "after submit" );

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void executePostFormRequest() throws IOException {

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

        Response response = clientSourceAppApi.executePostFormRequest("/api/client/list", queryParam, headerMap);

        String resultText = Util.toString( response.body().asReader() ) ;

        System.out.println( StringEscapeUtils.unescapeJson( resultText ) ) ;

    }

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
