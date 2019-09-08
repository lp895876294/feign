package demo.stats;

import com.netflix.loadbalancer.LoadBalancerStats;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerStats;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 扩展ribbon的服务端统计信息，记录每台服务器的响应时间
 */
public class AppLoadBalancerStats extends LoadBalancerStats {

    public AppLoadBalancerStats(){
        super() ;
        System.out.println("初始化负载均衡统计类");

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                // 获取服务端统计信息
                Map<Server,ServerStats> serverStatsMap = getServerStats() ;
                if( serverStatsMap==null || serverStatsMap.size()<1 ){
                    return ;
                }
                for (Map.Entry<Server, ServerStats> statsEntry : serverStatsMap.entrySet()) {
                    Server server = statsEntry.getKey() ;
                    ServerStats serverStats = statsEntry.getValue() ;
                    System.out.println( server.getHostPort() + "   " + serverStats.toString() );
                }
            }
        } , 1000 , 1000 );
    }




}
