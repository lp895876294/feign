package demo.log;

import feign.Logger;
import feign.Request;
import feign.Response;

import java.io.IOException;

/**
 * 扩展feigin的日志，记录每个请求的响应时间
 */
public class ConsoleLogger extends Logger {

    @Override
    protected void logRequest(String configKey, Level logLevel, Request request) {
//        super.logRequest( configKey , logLevel , request );
    }

    @Override
    protected Response logAndRebufferResponse(String configKey, Level logLevel, Response response, long elapsedTime) throws IOException {
        System.out.println( response.request().url() + " , 响应时间: " + Long.valueOf(elapsedTime).intValue() +"ms");
        return response ;
    }

    @Override
    protected void log(String configKey, String format, Object... args) {
        System.out.printf(methodTag(configKey) + format + "%n", args);
    }
}