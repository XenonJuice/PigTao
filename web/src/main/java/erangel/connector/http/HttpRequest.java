package erangel.connector.http;

import erangel.log.BaseLogger;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.security.Principal;
import java.util.*;

/**
 * 自定义 HttpRequest 类，实现 HttpServletRequest 接口。
 * 负责解析HTTP请求，并提供访问请求数据的方法。
 */
public class HttpRequest extends BaseLogger implements HttpServletRequest {
    // HTTP请求方法（例如：GET, POST）
    private String method;
    // 请求URI（例如：/index.html）
    private String uri;
    // 协议版本（例如：HTTP/1.1）
    private String protocol;
    // 存储HTTP头的映射
    private Map<String, List<String>> headers;
    // 存储请求参数的映射
    private Map<String, List<String>> parameters;
    // 请求体内容（字节数组形式）
    private byte[] body = null;
    // 封装后的 ServletInputStream
    private ServletInputStream servletInputStream;
    // 存储请求属性
    private final Map<String, Object> attributes = new HashMap<>();
    // 请求的远程地址和主机名
    private String remoteAddr;
    private String remoteHost;
    // 存储请求的 Locale
    private Locale locale;
    // 字符编码
    private String characterEncoding = "UTF-8";

    /**
     * 构造函数，初始化并解析HTTP请求。
     *
     * @param inputStream 客户端发送的 InputStream
     * @param remoteAddr  客户端的IP地址
     * @param remoteHost  客户端的主机名
     */
    public HttpRequest(InputStream inputStream, String remoteAddr, String remoteHost) {
        this.remoteAddr = remoteAddr;
        this.remoteHost = remoteHost;
        this.servletInputStream = new HttpRequestStream(inputStream);
        logger.info("收到来自 {} 的新请求", remoteAddr);
    }

    /**
     * 回收对象，清理资源
     */
    public void recycle() {
        method = null;
        uri = null;
        protocol = null;
        headers.clear();
        parameters.clear();
        body = null;
        attributes.clear();
        servletInputStream = null;
        remoteAddr = null;
        remoteHost = null;
        locale = null;
        characterEncoding = "UTF-8";
    }

    @Override
    public String getAuthType() {
        return null;
    }

    @Override
    public Cookie[] getCookies() {
        String cookieHeader = getHeader("Cookie");
        if (cookieHeader == null || cookieHeader.trim().isEmpty()) {
            logger.info("没有找到Cookie头部，返回空数组");
            return new Cookie[0];
        }

        String[] cookiePairs = cookieHeader.split(";");
        List<Cookie> cookieList = new ArrayList<>();
        for (String pair : cookiePairs) {
            String[] keyValue = pair.trim().split("=", 2);
            if (keyValue.length == 2) {
                String name = keyValue[0].trim();
                String value = keyValue[1].trim();
                Cookie cookie = new Cookie(name, value);
                cookieList.add(cookie);
                logger.info("解析到Cookie: {}={}", name, value);
            } else {
                logger.warn("无效的Cookie格式: {}", pair);
            }
        }
        logger.info("解析到的总Cookie数量: {}", cookieList.size());
        return cookieList.toArray(new Cookie[0]);
    }

    @Override
    public long getDateHeader(String name) {
        String value = getHeader(name);
        if (value == null) return -1;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public String getHeader(String name) {
        List<String> values = headers.get(name);
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        List<String> values = headers.get(name);
        return Collections.enumeration(values != null ? values : Collections.emptyList());
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headers.keySet());
    }

    @Override
    public int getIntHeader(String name) {
        String value = getHeader(name);
        if (value == null) return -1;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public String getPathInfo() {
        // 简单实现，根据需求调整
        return null;
    }

    @Override
    public String getPathTranslated() {
        // 简单实现，根据需求调整
        return null;
    }

    @Override
    public String getContextPath() {
        // 简单实现，根据需求调整
        return "";
    }

    @Override
    public String getQueryString() {
        // 将参数映射转换为查询字符串
        if (parameters.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                if (!sb.isEmpty()) sb.append("&");
                sb.append(key).append("=").append(value);
            }
        }
        return sb.toString();
    }

    @Override
    public String getRemoteUser() {
        // 未实现认证逻辑
        return null;
    }

    @Override
    public boolean isUserInRole(String role) {
        // 未实现认证逻辑
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        // 未实现认证逻辑
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        // 未实现会话管理
        return null;
    }

    @Override
    public String getRequestURI() {
        return uri;
    }

    @Override
    public StringBuffer getRequestURL() {
        // 未实现完整URL构建逻辑
        return new StringBuffer(uri);
    }

    @Override
    public String getServletPath() {
        // 简单实现，根据需求调整
        return "";
    }

    @Override
    public HttpSession getSession(boolean create) {
        // 未实现会话管理
        return null;
    }

    @Override
    public HttpSession getSession() {
        // 未实现会话管理
        return null;
    }

    @Override
    public String changeSessionId() {
        // 未实现会话管理
        return null;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        // 未实现会话管理
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        // 未实现会话管理
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        // 未实现会话管理
        return false;
    }

    @Override
    @Deprecated
    public boolean isRequestedSessionIdFromUrl() {
        // 未实现会话管理
        return false;
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws ServletException {
        // 未实现认证逻辑
        return false;
    }

    @Override
    public void login(String username, String password) throws ServletException {
        // 未实现认证逻辑
    }

    @Override
    public void logout() throws ServletException {
        // 未实现认证逻辑
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        // 未实现多部分请求解析
        return Collections.emptyList();
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        // 未实现多部分请求解析
        return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        // 未实现升级协议（如WebSocket）
        return null;
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        if (env == null || env.isEmpty()) {
            throw new UnsupportedEncodingException("Character encoding is null or empty");
        }
        this.characterEncoding = env;
    }

    @Override
    public int getContentLength() {
        String contentLength = getHeader("Content-Length");
        return contentLength != null ? Integer.parseInt(contentLength) : -1;
    }

    @Override
    public long getContentLengthLong() {
        String contentLength = getHeader("Content-Length");
        return contentLength != null ? Long.parseLong(contentLength) : -1L;
    }

    @Override
    public String getContentType() {
        return getHeader("Content-Type");
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return servletInputStream;
    }

    @Override
    public String getParameter(String name) {
        List<String> values = parameters.get(name);
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }

    public void setParameters(Map<String, List<String>> parameters) {
        this.parameters = parameters;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameters.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        List<String> values = parameters.get(name);
        if (values == null || values.isEmpty()) return null;
        return values.toArray(new String[0]);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> map = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            map.put(entry.getKey(), entry.getValue().toArray(new String[0]));
        }
        return map;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public String getScheme() {
        // 简单实现，根据实际情况调整
        return "http";
    }

    @Override
    public String getServerName() {
        // 简单实现，根据实际情况调整
        return "localhost";
    }

    @Override
    public int getServerPort() {
        // 简单实现，根据实际情况调整
        return 80;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (body == null) {
            return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(new byte[0]), characterEncoding));
        }
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body), characterEncoding));
    }

    @Override
    public String getRemoteAddr() {
        return remoteAddr;
    }

    @Override
    public String getRemoteHost() {
        return remoteHost;
    }

    @Override
    public void setAttribute(String name, Object o) {
        attributes.put(name, o);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public Locale getLocale() {
        if (locale != null) return locale;
        String acceptLanguage = getHeader("Accept-Language");
        if (acceptLanguage != null && !acceptLanguage.isEmpty()) {
            String[] locales = acceptLanguage.split(",");
            if (locales.length > 0) {
                String[] parts = locales[0].trim().split(";");
                try {
                    locale = Locale.forLanguageTag(parts[0]);
                } catch (Exception e) {
                    locale = Locale.getDefault();
                }
                return locale;
            }
        }
        return Locale.getDefault();
    }

    @Override
    public Enumeration<Locale> getLocales() {
        String acceptLanguage = getHeader("Accept-Language");
        if (acceptLanguage == null || acceptLanguage.isEmpty()) {
            return Collections.enumeration(Collections.singletonList(Locale.getDefault()));
        }
        String[] locales = acceptLanguage.split(",");
        List<Locale> localeList = new ArrayList<>();
        for (String loc : locales) {
            String[] parts = loc.trim().split(";");
            try {
                localeList.add(Locale.forLanguageTag(parts[0]));
            } catch (Exception e) {
                // 忽略无效的Locale
            }
        }
        if (localeList.isEmpty()) {
            localeList.add(Locale.getDefault());
        }
        return Collections.enumeration(localeList);
    }

    @Override
    public boolean isSecure() {
        // 简单实现，根据实际情况调整
        return "https".equalsIgnoreCase(getScheme());
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        // 未实现请求转发逻辑
        return null;
    }

    @Override
    @Deprecated
    public String getRealPath(String path) {
        // 已弃用方法，返回null
        return null;
    }

    @Override
    public int getRemotePort() {
        // 简单实现，根据实际情况调整
        return 0;
    }

    @Override
    public String getLocalName() {
        // 简单实现，根据实际情况调整
        return "localhost";
    }

    @Override
    public String getLocalAddr() {
        // 简单实现，根据实际情况调整
        return "127.0.0.1";
    }

    @Override
    public int getLocalPort() {
        // 简单实现，根据实际情况调整
        return 80;
    }

    @Override
    public ServletContext getServletContext() {
        // 未实现ServletContext相关逻辑
        return null;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        // 未实现异步处理逻辑
        throw new IllegalStateException("Async not supported.");
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        // 未实现异步处理逻辑
        throw new IllegalStateException("Async not supported.");
    }

    @Override
    public boolean isAsyncStarted() {
        // 未实现异步处理逻辑
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        // 未实现异步处理逻辑
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        // 未实现异步处理逻辑
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        // 简单实现，根据需求调整
        return DispatcherType.REQUEST;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }


}
