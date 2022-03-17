package com.center.rpc.pojo;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/16
 */
@Getter
@Setter
public class ZKConfig {


    private String connectStr;

    private int sessionTimeoutMs;

    private int connectionTimeoutMs;

    private String namespace;
}
