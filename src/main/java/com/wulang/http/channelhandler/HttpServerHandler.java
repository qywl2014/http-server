/*
 * Copyright 2013 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.wulang.http.channelhandler;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.Values;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpServerHandler extends ChannelInboundHandlerAdapter {
    private static final byte[] CONTENT = {'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'};

    private String rootDir;

    public HttpServerHandler(String rootDir) {
        this.rootDir = rootDir;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;

            if (HttpHeaders.is100ContinueExpected(req)) {
                ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
            }
            boolean keepAlive = HttpHeaders.isKeepAlive(req);
            String url = req.getUri();
            if(req.headers().get("Host").contains("google")){
                return;
            }
            System.out.println("uri:" + url);
//            String accept = req.headers().get("accept");
//            System.out.println(accept);
//            byte[] content=readBinaryFileToByteArray(url);
//            System.out.println("长度："+content.length);
//            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer("hello".getBytes()));
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(httpClient(req).getBytes()));
//            response.headers().set(CONTENT_TYPE, getContentType(accept));
            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());

            if (!keepAlive) {
                ctx.write(response).addListener(ChannelFutureListener.CLOSE);
            } else {
                response.headers().set(CONNECTION, Values.KEEP_ALIVE);
                ctx.write(response);
            }
            for (Map.Entry<String, String> head : req.headers()) {
                System.out.println(head.getKey() + "::" + head.getValue());
            }

        }
    }

    private String httpClient(HttpRequest req) {
        try {
//            String finalUrlStr=encodeParameters(req, false);
//            System.out.println("urlStr::" + finalUrlStr);
            URL targetUrl = new URL(req.getUri());
            HttpURLConnection httpURLConnection = (HttpURLConnection) targetUrl.openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setConnectTimeout(10000);
            httpURLConnection.setReadTimeout(5000);
            httpURLConnection.setDoInput(true);

            InputStream in = httpURLConnection.getInputStream();
//        httpURLConnection.setDoInput(true);
            BufferedReader rd = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            StringBuilder sB = new StringBuilder();
            String tempLine = null;
            while ((tempLine = rd.readLine()) != null) {
                sB.append(tempLine);
            }
            System.out.println(sB);
            rd.close();
            return sB.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String encodeParameters(HttpRequest req, boolean ifNeedEncode) {
        try {
            String host = "10.0.3.173:9090";
//            String host=req.headers().get("Host");
            if (!ifNeedEncode) {
                return "http://" + host + req.getUri();
            }
            String[] urlSplitArray = req.getUri().split("\\?");
            String path = urlSplitArray[0];
            String finalUrlStr = "http://" + host + path;
            String querySP = null;
            if (urlSplitArray.length > 1) {
                finalUrlStr = finalUrlStr + "?";
                querySP = urlSplitArray[1];
                String[] querySPArray = querySP.split("&");
                for (String sP : querySPArray) {
                    String[] array = sP.split("=");
                    finalUrlStr = finalUrlStr + array[0] + "=" + URLEncoder.encode(array[1], "UTF-8") + "&";
                }
                finalUrlStr = finalUrlStr.substring(0, finalUrlStr.length() - 1);
            }
            return finalUrlStr;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private byte[] readFileToByteArray(String url) {
        String str;
        try {
            File file = new File(convertToFilePath(url));
            FileReader reader = new FileReader(file);
            BufferedReader bReader = new BufferedReader(reader);
            StringBuilder sb = new StringBuilder();
            String s;
            while ((s = bReader.readLine()) != null) {
                sb.append(s + "\n");
            }
            bReader.close();
            str = sb.toString();
        } catch (Exception e) {
            str = "error";
            e.printStackTrace();
        }
        return str.getBytes();
    }

    private byte[] readBinaryFileToByteArray(String url) {
        byte[] byteAray = null;
        try {
            File inFile = new File(convertToFilePath(url));
            FileInputStream fileInputStream = new FileInputStream(inFile);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int i;
            //转化为字节数组流
            while ((i = fileInputStream.read()) != -1) {
                byteArrayOutputStream.write(i);
            }
            // 把文件存在一个字节数组中
            byteAray = byteArrayOutputStream.toByteArray();
            fileInputStream.close();
            byteArrayOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return byteAray;
    }

    private String convertToFilePath(String url) {
        String path;
        if ("/".equals(url)) {
            path = rootDir + "/index.html";
        } else {
            path = rootDir + url;
        }
        return path;
    }

    private String getContentType(String accept) {
        String[] strArray = accept.split(",");
        if (strArray.length > 0) {
            if (strArray[0].equals("image/webp")) {
                return "image/jpeg";
            }
        }
        return "text/html";
    }
}
