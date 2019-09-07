package demo.log;

import feign.Logger;
import feign.Request;
import feign.Response;

import java.io.IOException;

public class ConsoleLogger extends Logger {

    @Override
    protected void logRequest(String configKey, Level logLevel, Request request) {
//        log(configKey, "---> %s %s HTTP/1.1", request.httpMethod().name(), request.url());
        super.logRequest( configKey , logLevel , request );
    }

    @Override
    protected Response logAndRebufferResponse(String configKey, Level logLevel, Response response, long elapsedTime) throws IOException {
        return super.logAndRebufferResponse( configKey , logLevel , response , elapsedTime ) ;
    }

    @Override
    protected void log(String configKey, String format, Object... args) {
        System.out.printf(methodTag(configKey) + format + "%n", args);
    }
}