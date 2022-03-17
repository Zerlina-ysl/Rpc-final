package com.atcompany.rpc.provider;

import com.atcompany.rpc.provider.Server.RpcServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/15
 * @author luna
 */
@SpringBootApplication
public class ServerBootStrapApplication implements CommandLineRunner {

    public static  final int port = ThreadLocalRandom.current().nextInt(8000, 9000);

    public static void main(String[] args) {
    SpringApplication.run(ServerBootStrapApplication.class,args);


    }

    @Autowired
    RpcServer rpcServer;

    /**
     * 模拟多个服务提供者
     * @param args
     * @throws Exception
     */
    @Override
    public void run(String... args) throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                rpcServer.startServer("127.0.0.1",port);
            }
        }).start();

    }
}
