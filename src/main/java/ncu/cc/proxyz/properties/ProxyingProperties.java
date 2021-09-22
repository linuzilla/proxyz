package ncu.cc.proxyz.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("application.proxying")
public class ProxyingProperties {
    private String headerName;
    private String host;
    private Integer port;
    private String scheme;

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }
}
