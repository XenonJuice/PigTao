package erangel.connector.http;

import erangel.log.BaseLogger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 自定义的 HttpServletResponse 实现，用于处理 HTTP 响应。
 */
public class HttpResponse extends BaseLogger implements HttpServletResponse {

    // 状态码和消息
    private int status = SC_OK;
    private String statusMessage = "OK";
    // 响应头部
    private final Map<String, List<String>> headers = new HashMap<>();
    // Cookie 列表
    private final List<Cookie> cookies = new ArrayList<>();

    // 内容类型和字符编码
    private String contentType;
    private String characterEncoding = "ISO-8859-1";

    // 输出流和缓冲区
    private final OutputStream clientOutputStream;
    private final ByteArrayOutputStream bufferStream; // 临时缓冲区
    private final HttpResponseStream servletOutputStream;
    private final PrintWriter writer;

    // 是否已提交？
    private boolean isCommitted = false;
    // 标志，防止同时使用 Writer 和 OutputStream
    private boolean writerUsed = false;
    private boolean outputStreamUsed = false;
    // 缓冲区大小
    private int bufferSize = 8192;

    /**
     * 构造函数，初始化输出流和缓冲区。
     *
     * @param outputStream 客户端的输出流
     * @throws UnsupportedEncodingException 如果指定的字符编码不支持
     */
    public HttpResponse(OutputStream outputStream) throws UnsupportedEncodingException {
        this.clientOutputStream = outputStream;
        this.bufferStream = new ByteArrayOutputStream();
        this.servletOutputStream = new HttpResponseStream(this.bufferStream);
        this.writer = new PrintWriter(new OutputStreamWriter(this.bufferStream, this.characterEncoding));
    }

    /**
     * 回收对象，清理资源
     */
    public void recycle() {
        this.status = SC_OK;
        this.statusMessage = "OK";
        this.headers.clear();
        this.cookies.clear();
        this.contentType = null;
        this.characterEncoding = "ISO-8859-1";
        // 重置缓冲区
        this.bufferStream.reset();
        this.isCommitted = false;
        this.writerUsed = false;
        this.outputStreamUsed = false;
    }

    // 设置状态码
    @Override
    public void setStatus(int sc) {
        this.status = sc;
        this.statusMessage = getReasonPhrase(sc);
    }

    @Override
    public void setStatus(int sc, String sm) {
        this.status = sc;
        this.statusMessage = sm;
    }

    @Override
    public int getStatus() {
        return this.status;
    }

    // 获取状态码对应的原因短语
    private String getReasonPhrase(int statusCode) {
        return switch (statusCode) {
            case SC_OK -> "OK";
            case SC_NOT_FOUND -> "Not Found";
            case SC_INTERNAL_SERVER_ERROR -> "Internal Server Error";
            case SC_BAD_REQUEST -> "Bad Request";
            case SC_FOUND -> "Found";
            // case SC_UNAUTHORIZED -> "Unauthorized";
            // 添加其他状态码
            default -> "";
        };
    }

    // 设置头部信息
    @Override
    public void setHeader(String name, String value) {
        List<String> values = new ArrayList<>();
        values.add(value);
        headers.put(name, values);
    }

    @Override
    public void addHeader(String name, String value) {
        headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
    }

    @Override
    public String getHeader(String name) {
        List<String> values = headers.get(name);
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return headers.getOrDefault(name, Collections.emptyList());
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headers.keySet();
    }

    // 设置内容类型
    @Override
    public void setContentType(String type) {
        this.contentType = type;
        setHeader("Content-Type", type);
    }

    @Override
    public String getContentType() {
        return this.contentType;
    }

    // 设置字符编码
    @Override
    public void setCharacterEncoding(String charset) {
        this.characterEncoding = charset;
    }

    @Override
    public String getCharacterEncoding() {
        return this.characterEncoding;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (writerUsed) {
            throw new IllegalStateException("getWriter() has already been called on this response.");
        }
        outputStreamUsed = true;
        return this.servletOutputStream;
    }

    // 获取 PrintWriter
    @Override
    public PrintWriter getWriter() throws IOException {
        if (outputStreamUsed) {
            throw new IllegalStateException("getOutputStream() has already been called on this response.");
        }
        writerUsed = true;
        return this.writer;
    }

    // 添加 Cookie
    @Override
    public void addCookie(Cookie cookie) {
        if (isCommitted) {
            throw new IllegalStateException("Cannot add cookie after response has been committed.");
        }
        if (cookie != null) {
            this.cookies.add(cookie);
        }
    }

    @Override
    public boolean containsHeader(String name) {
        return headers.containsKey(name);
    }

    // 发送错误
    @Override
    public void sendError(int sc) throws IOException {
        sendError(sc, getReasonPhrase(sc));
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        if (isCommitted) {
            throw new IllegalStateException("Cannot send error after response has been committed.");
        }
        setStatus(sc, msg);
        // 清空头部和 Cookies
        headers.clear();
        cookies.clear();
        // 设置内容类型
        setContentType("text/html; charset=UTF-8");
        // 构建错误页面
        String errorPage = "<html><head><title>Error</title></head><body>"
                + "<h1>HTTP Error " + sc + " - " + msg + "</h1>"
                + "</body></html>";
        // 写入错误页面到缓冲区
        writer.write(errorPage);
        writer.flush();
        flushBuffer();
    }

    // 发送重定向
    @Override
    public void sendRedirect(String location) throws IOException {
        if (isCommitted) {
            throw new IllegalStateException("Cannot send redirect after response has been committed.");
        }
        setStatus(SC_FOUND, getReasonPhrase(SC_FOUND));
        setHeader("Location", location);
        // 设置内容类型
        setContentType("text/html; charset=UTF-8");
        // 构建重定向页面
        String redirectPage = "<html><head><title>Redirect</title></head><body>"
                + "<h1>Redirecting to <a href=\"" + location + "\">" + location + "</a></h1>"
                + "</body></html>";
        // 写入重定向页面到缓冲区
        writer.write(redirectPage);
        writer.flush();
        flushBuffer();
    }

    // 缓冲区管理
    @Override
    public void flushBuffer() throws IOException {
        if (isCommitted) return;
        if (writerUsed) writer.flush();
        if (outputStreamUsed) servletOutputStream.flush();
        // 获取缓冲区中的数据
        byte[] body = bufferStream.toByteArray();
        // 设置 Content-Length
        setContentLength(body.length);
        // 写入状态行和头部
        writeStatusLineAndHeaders();
        // 将主体写入客户端输出流
        clientOutputStream.write(body, 0, body.length);
        clientOutputStream.flush();
        // 标记响应已提交
        isCommitted = true;
    }

    /**
     * 写入状态行和头部到实际输出流。
     *
     * @throws IOException 如果发生 I/O 错误
     */
    private void writeStatusLineAndHeaders() throws IOException {
        StringBuilder responseBuilder = new StringBuilder();

        // 构建状态行
        responseBuilder.append("HTTP/1.1 ").append(status).append(" ").append(statusMessage).append("\r\n");

        // 优先输出 Content-Type
        if (headers.containsKey("Content-Type")) {
            for (String value : headers.get("Content-Type")) {
                responseBuilder.append("Content-Type: ").append(value).append("\r\n");
            }
        }

        // 然后输出其他头部，包括 Content-Length
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            String name = header.getKey();
            if ("Content-Type".equalsIgnoreCase(name)) continue; // 已经处理
            for (String value : header.getValue()) {
                responseBuilder.append(name).append(": ").append(value).append("\r\n");
            }
        }

        // 构建每个 Set-Cookie 头部
        for (Cookie cookie : cookies) {
            responseBuilder.append("Set-Cookie: ").append(formatCookie(cookie)).append("\r\n");
        }

        // 添加日期头部
        responseBuilder.append("Date: ").append(formatDate(System.currentTimeMillis())).append("\r\n");
        responseBuilder.append("Server: CustomJavaServer/1.0\r\n");

        // 空行，分隔头部和主体
        responseBuilder.append("\r\n");

        // 写入状态行和头部到实际输出流
        clientOutputStream.write(responseBuilder.toString().getBytes(this.characterEncoding));
    }


    // 格式化 Cookie
    public String formatCookie(Cookie cookie) {
        StringBuilder cookieBuilder = new StringBuilder();
        // 基本的 name=value
        cookieBuilder.append(cookie.getName()).append("=").append(cookie.getValue());
        // 可选属性
        if (cookie.getMaxAge() >= 0) cookieBuilder.append("; Max-Age=").append(cookie.getMaxAge());
        if (cookie.getPath() != null) {
            cookieBuilder.append("; Path=").append(cookie.getPath());
        } else {
            cookieBuilder.append("; Path=/"); // 默认路径
        }
        if (cookie.getDomain() != null) cookieBuilder.append("; Domain=").append(cookie.getDomain());
        if (cookie.getSecure()) cookieBuilder.append("; Secure");
        if (cookie.isHttpOnly()) cookieBuilder.append("; HttpOnly");

        return cookieBuilder.toString();
    }

    @Override
    public int getBufferSize() {
        return this.bufferSize;
    }

    @Override
    public void setBufferSize(int size) {
        if (isCommitted) {
            throw new IllegalStateException("Cannot set buffer size after response has been committed");
        }
        this.bufferSize = size;
    }

    @Override
    public boolean isCommitted() {
        return this.isCommitted;
    }

    @Override
    public void resetBuffer() {
        if (isCommitted) {
            throw new IllegalStateException("Cannot reset buffer after response has been committed.");
        }
        headers.clear();
        cookies.clear();
        bufferStream.reset();
    }

    @Override
    public void reset() {
        if (isCommitted) {
            throw new IllegalStateException("Cannot reset after response has been committed.");
        }
        status = SC_OK;
        statusMessage = "OK";
        headers.clear();
        cookies.clear();
        contentType = null;
        characterEncoding = "ISO-8859-1";
        writerUsed = false;
        outputStreamUsed = false;
        bufferStream.reset();
    }

    // 设置内容长度
    @Override
    public void setContentLength(int len) {
        setIntHeader("Content-Length", len);
    }

    @Override
    public void setContentLengthLong(long len) {
        setHeader("Content-Length", Long.toString(len));
    }

    // 设置日期头部
    @Override
    public void setDateHeader(String name, long date) {
        setHeader(name, formatDate(date));
    }

    @Override
    public void addDateHeader(String name, long date) {
        addHeader(name, formatDate(date));
    }

    // 设置整数头部
    @Override
    public void setIntHeader(String name, int value) {
        setHeader(name, Integer.toString(value));
    }

    @Override
    public void addIntHeader(String name, int value) {
        addHeader(name, Integer.toString(value));
    }

    // 日期格式化
    public String formatDate(long date) {
        // 格式化为 HTTP 日期格式，例如：Sun, 06 Nov 1994 08:49:37 GMT
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(new Date(date));
    }

    // 设置是否使用字符编码
    @Override
    public void setLocale(Locale loc) {
        // 可根据需要实现
    }

    @Override
    public Locale getLocale() {
        return Locale.getDefault();
    }

    public List<Cookie> getCookies() {
        return this.cookies;
    }

    public Map<String, List<String>> getHeadersMap() {
        return this.headers;
    }

    public String getStatusMessage() {
        return this.statusMessage;
    }

    // HttpServletResponse 接口中被弃用的方法
    @Deprecated
    @Override
    public String encodeUrl(String url) {
        return encodeURL(url);
    }

    @Deprecated
    @Override
    public String encodeRedirectUrl(String url) {
        return encodeRedirectURL(url);
    }

    // 实现 URL 编码方法
    @Override
    public String encodeURL(String url) {
        return url;
    }

    @Override
    public String encodeRedirectURL(String url) {
        return url;
    }
}
