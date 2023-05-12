/**
 * Botaoyx.com Inc.
 * Copyright (c) 2021-2023 All Rights Reserved.
 */
package martin.example.ws;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API/Writing_a_WebSocket_server_in_Java
 *
 * @author Martin.C
 * @version 2023/04/19 19:17
 */
public class EchoWsServer {
    static final String  WS_MAGIC = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    static final Charset U8 = StandardCharsets.UTF_8;

    public static void main(String[] args){
        int port = 8083;
        //System.out.println(UUID.fromString(WS_MAGIC).version());
        try (ServerSocket server = new ServerSocket(port)){
            System.out.println("Server has started on 127.0.0.1:"+port+".\r\nWaiting for a connection…");
            Socket client = server.accept();
            System.out.println("A client connected.");
            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();
            //Scanner s = new Scanner(in, U8);
            //var response = handshaking(in);
            out.write(handshaking(in));
            out.flush();

            jsendFrame(out,"Welcome to Java WS Server!".getBytes(U8),OpCode.TEXT);
            while (true){
                var txt = readTextFrameBlock(in);
                System.out.println("get txt data length:" + txt.length());
                var resb = ("[Java Server] Got You,你好 :" + txt).getBytes(U8);
                jsendFrame(out, resb, OpCode.TEXT);
            }
            //client.close();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    static String readTextFrameBlock(InputStream in) throws IOException{
        // https://datatracker.ietf.org/doc/html/rfc6455#section-5.2
        // wait a Frame.
        int b0 = in.read();
        int b1 = in.read();

        var fin = (b0 & 0b10000000) != 0;
        int opcode = b0 & 0b00001111;

        long msglen = checkExtendedPayloadLength(b1,in);
        var payload =   readPayload ( msglen,in, (b1 & 0b10000000) != 0);

        //if(opcode == OpCode.TEXT.code) {
            return new String(payload, U8);
        //}
    }

    static byte[] readPayload(long msglen ,InputStream in , boolean mask) throws IOException {
        byte[] maskKey = null;
        if(mask) {
            maskKey = new byte[4];
            in.read(maskKey);
        }
        var dataLen = in.available();
        System.out.println("available == msglen ?  " + dataLen +" == "+ msglen +"  : "+ (dataLen == msglen) );
        var data = new byte[dataLen];
        in.read(data);
        if(mask){
            xorDecode(data,maskKey);
        }else {
            System.out.println("found unmask data !");
        }
        return  data;
    }

    static final int FINAL_FIN_RSV   =   1<<7;//128;
    enum  OpCode{
        //CONTINUATION(0),//%x0 ; frame continuation
        TEXT(1),//%x1 ; text frame
        BINARY(2),//%x2 ; binary frame
        CLOSE(8),// ; connection close
        PING(9),
        PONG(0xA);
        final int code;

        OpCode(int code) {this.code = code;}
    }

    static long checkExtendedPayloadLength(int byteLen,InputStream in) throws IOException {
        long msglen = byteLen & 0b01111111;

        if (msglen == 126) {
            // max 65535
            // next two bytes;  Big-Endian, whereas
            msglen = (in.read() << 8 ) + in.read();
            //System.out.println("126 short msglen ?  " +msglen );
        } else if (msglen == 127) {
            var lbits = new byte[Long.BYTES];
            // 2^63 -1
            in.read(lbits);
            //System.out.println("longLen?  " +Arrays.toString(longLen) );
            msglen = ByteBuffer.wrap(lbits).getLong();
            //System.out.println("127 long msglen ?  " +msglen );
        }
        return msglen;
    }

    static void jsendFrame(OutputStream out,byte[] data,OpCode opCode) throws IOException {
        out.write(FINAL_FIN_RSV | opCode.code);
        int len  = data.length;
        if(len<126){
            out.write(len);
        } else if (len <= 65535) {
            out.write(126);
            out.write(len>>8);
            out.write(len & 0xFF);
        }else{
            out.write(127);
            ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
            buf.putLong(len);
            out.write(buf.array());
        }
        out.write(data);
        out.flush();
    }

    //https://github.com/Theldus/wsServer/blob/master/src/ws.c
    //static void sendFrame(OutputStream out,byte[] data,int frameType) throws IOException {
    //
    //
    //    var frame = new byte[10];
    //    byte b0 = (byte) (WS_FIN | frameType);
    //    frame[0] = b0;
    //    /* Split the size between octets. */
    //    int idx_first_rData;
    //    var length = data.length;
    //    if (length <= 125)
    //    {
    //        frame[1] = (byte) (length & 0x7F);
    //        idx_first_rData = 2;
    //    }
    //
    //    /* Size between 126 and 65535 bytes. */
    //    else if (length <= 65535)
    //    {
    //        frame[1] = 126;
    //        frame[2] = (byte) ((length >> 8) & 255);
    //        frame[3] = (byte) (length & 255);
    //        idx_first_rData = 4;
    //    }
    //
    //    /* More than 65535 bytes. */
    //    else
    //    {
    //        frame[1] = 127;
    //        long len  = length;
    //        frame[2] = (byte)((len >> 56) & 255);
    //        frame[3] = (byte)((len >> 48) & 255);
    //        frame[4] = (byte)((len >> 40) & 255);
    //        frame[5] = (byte)((len >> 32) & 255);
    //        frame[6] = (byte)((len >> 24) & 255);
    //        frame[7] = (byte)((len >> 16) & 255);
    //        frame[8] = (byte)((len >> 8) & 255);
    //        frame[9] = (byte)(len & 255);
    //
    //        //By default, the order of a ByteBuffer object is BIG_ENDIAN.
    //
    //        //Allocates a new byte buffer.
    //        //    The new buffer's position will be zero, its limit will be its capacity, its mark will be undefined,
    //        //each of its elements will be initialized to zero, and its byte order will be BIG_ENDIAN.
    //
    //        //ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
    //        //buf.putLong(length);
    //        //var res = buf.array();
    //
    //        idx_first_rData = 10;
    //    }
    //    out.write(frame,0,idx_first_rData);
    //    out.write(data);
    //    System.out.println("send to client , type :" + frameType +", with length: "+length);
    //    out.flush();
    //}

    static  byte[] handshaking(InputStream in ) throws NoSuchAlgorithmException, IOException {
        while (in.available() < 3);// match against "get"
        byte[] bytes = new byte[in.available()];
        in.read(bytes, 0, in.available());
        var data = new String(bytes,U8);

        //GET /anyway HTTP/1.1
        //Sec-WebSocket-Version: 13
        //Sec-WebSocket-Key: 9UDsV5rQ/3mzsPGlsJJc8Q==
        //Connection: Upgrade
        //Upgrade: websocket
        //Host: 127.0.0.1:8083
        //\r\n

        //String data = s.useDelimiter("\\r\\n\\r\\n").next();
        //Matcher get = Pattern.compile("^GET").matcher(data);
        if (data.startsWith("GET")) {
            Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);

            if(match.find()) {
                //System.out.println(match.group(1));
                var accept = Base64.getEncoder().encodeToString(
                        MessageDigest.getInstance("SHA-1").digest((match.group(1)
                                + WS_MAGIC).getBytes(U8)));
                byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n"
                        + "Connection: Upgrade\r\n"
                        + "Upgrade: websocket\r\n"
                        + "Sec-WebSocket-Accept: "
                        + accept
                        + "\r\n\r\n").getBytes(U8);
                return response;
            }
        }
        return new byte[0];
    }


    static  void xorDecode ( byte[] encoded, byte[] maskingKey ){
        assert  maskingKey.length == 4;
        for (int i = 0; i < encoded.length; i++) {
            encoded[i] = (byte) (encoded[i] ^ maskingKey[i & 0x3]);
        }
    }

    // A utility function that returns
    // long value from a byte array
    //static long convertToLongBigEnd(byte[] b)
    //{
    //    ByteBuffer buffer = ByteBuffer.wrap(b);
    //    //buffer.put(b);
    //    //buffer.flip();//need flip
    //    return buffer.getLong();
    //
    //    //
    //    //long result = 0;
    //    //for (int i = 0; i < Long.BYTES; i++) {
    //    //    result <<= Byte.SIZE;
    //    //    result |= (b[i] & 0xFF);
    //    //}
    //    //return  result;
    //    //long value = 0l;
    //    //// Iterating through for loop
    //    //for (byte ec : b) {
    //    //    // Shifting previous value 8 bits to right and
    //    //    // add it with next value
    //    //    value = (value << 8) + (ec & 255);
    //    //}
    //    //return value;
    //
    //    //
    //
    //}
}