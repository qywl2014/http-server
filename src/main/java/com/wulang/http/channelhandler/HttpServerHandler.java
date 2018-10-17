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

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.Values;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpServerHandler extends ChannelInboundHandlerAdapter {
    private static final byte[] CONTENT = { 'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd' };

    private String rootDir;

    public HttpServerHandler(String rootDir){
        this.rootDir=rootDir;
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
            String url=req.getUri();
            System.out.println(url);
            String accept=req.headers().get("accept");
            System.out.println(accept);
            byte[] content=readBinaryFileToByteArray(url);
            System.out.println("长度："+content.length);
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(content));
            response.headers().set(CONTENT_TYPE, getContentType(accept));
            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());

            if (!keepAlive) {
                ctx.write(response).addListener(ChannelFutureListener.CLOSE);
            } else {
                response.headers().set(CONNECTION, Values.KEEP_ALIVE);
                ctx.write(response);
            }
        }
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
            while ((s =bReader.readLine()) != null) {
                sb.append(s + "\n");
            }
            bReader.close();
            str = sb.toString();
        }catch (Exception e){
            str="error";
            e.printStackTrace();
        }
        return str.getBytes();
    }

    private byte[] readBinaryFileToByteArray(String url) {
        byte[] byteAray=null;
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

    private String convertToFilePath(String url){
        String path;
        if("/".equals(url)){
            path=rootDir+"/index.html";
        }else{
            path=rootDir+url;
        }
        return path;
    }

    private String getContentType(String accept){
        String[] strArray=accept.split(",");
        if(strArray.length>0){
            if(strArray[0].equals("image/webp")){
                return "image/jpeg";
            }
        }
        return "text/html";
    }
}
