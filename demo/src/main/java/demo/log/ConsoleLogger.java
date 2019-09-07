package demo.log;

import com.google.common.util.concurrent.AtomicDouble;
import feign.Logger;
import feign.Request;
import feign.Response;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class ConsoleLogger extends Logger {

//    public static final AtomicInteger totalNum = new AtomicInteger(0) ;
//
//    public static final AtomicInteger totalAvg = new AtomicInteger(0) ;

    public static int totalNum = 0 ;

    public static int totalAvg = 0 ;

    @Override
    protected void logRequest(String configKey, Level logLevel, Request request) {
//        log(configKey, "---> %s %s HTTP/1.1", request.httpMethod().name(), request.url());
//        super.logRequest( configKey , logLevel , request );
    }

    @Override
    protected Response logAndRebufferResponse(String configKey, Level logLevel, Response response, long elapsedTime) throws IOException {
//        return super.logAndRebufferResponse( configKey , logLevel , response , elapsedTime ) ;
//        int num = totalNum.incrementAndGet() ;
//
//        int avg = totalAvg.addAndGet( Long.valueOf(elapsedTime).intValue() ) ;

        int num = ++totalNum ;

        totalAvg += Long.valueOf(elapsedTime).intValue() ;

        int avg = totalAvg ;

        System.out.println( response.request().url() + "  num=  " + num +"  " + avg/num );

        return response ;
    }

    @Override
    protected void log(String configKey, String format, Object... args) {
        System.out.printf(methodTag(configKey) + format + "%n", args);
    }
}