package com.center.rpc.handler;

import com.center.rpc.pojo.ServiceInfo;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/16
 */
public interface ServerListener {




    void serverAdded(ServiceInfo service);

    void serverRemoved(ServiceInfo service);

    void serverUpdated(ServiceInfo service,String data);



}
