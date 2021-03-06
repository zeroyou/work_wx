/*
 * work_wx
 * wuhen 2020/1/16.
 * Copyright (c) 2020  jianfengwuhen@126.com All Rights Reserved.
 */

package com.work.wx.controller.api.chat;

import com.google.gson.JsonObject;
import com.work.wx.config.CustomConfig;
import com.work.wx.config.RequestUtil;
import com.work.wx.controller.api.token.MsgAuditAccessToken;
import com.work.wx.controller.modle.ChatModel;
import com.work.wx.server.ChatServer;
import com.work.wx.server.CorpServer;
import com.work.wx.server.TokenServer;
import com.work.wx.tips.SuccessTip;
import com.work.wx.tips.Tip;
import com.work.wx.util.LangUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

@RestController
@Api(tags = "chat")
public class ChatAPI {
    private final static Logger logger = LoggerFactory.getLogger(ChatAPI.class);

    private TokenServer tokenServer;
    private ChatServer chatServer;
    private CorpServer corpServer;

    @Autowired
    public void setCorpServer(CorpServer corpServer) {
        this.corpServer = corpServer;
    }

    @Autowired()
    public void setChatServer(ChatServer chatServer) {
        this.chatServer = chatServer;
    }

    @Autowired()
    public void setTokenServer(TokenServer tokenServer) {
        this.tokenServer = tokenServer;
    }


    private String getToken(String corpId) {
        return new MsgAuditAccessToken().getMSGAUDITAccessToken(tokenServer,corpServer.getCorpModel(corpId));
    }


    @ApiOperation("群聊获取会话同意情况")
    @ResponseBody
    @RequestMapping(value = "/checkRoomAgree",method = RequestMethod.POST)
    public Tip checkRoomAgree(@RequestParam("cropId") String corpId,@RequestParam("roomid") String roomid){
        String BASE_ADDRESS = "https://qyapi.weixin.qq.com/cgi-bin/msgaudit/check_room_agree";
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("roomid",roomid);
        return new RequestUtil().requestJsonPostDone(BASE_ADDRESS, getToken(corpId), jsonObject.toString());
    }


    @ApiOperation("获取会话人员列表")
    @ResponseBody
    @RequestMapping(value = "/getChatUsers",method = RequestMethod.POST)
    public Tip getChatUsers(@RequestParam("corpId") String corpId){
        ChatModel chatModel = new ChatModel(corpId);
        chatModel.setRoomid("");
        chatModel.setMark(null);
        List chatModelList = chatServer.getUserList(chatModel,"from","seq");
        return new SuccessTip(chatModelList);
    }


    @ApiOperation("获取群聊人员列表")
    @ResponseBody
    @RequestMapping(value = "/getChatRooms",method = RequestMethod.POST)
    public Tip getChatRooms(@RequestParam("corpId") String corpId){
        ChatModel chatModel = new ChatModel(corpId);
        chatModel.setMark(null);
        List chatModelList = chatServer.getRoomList(chatModel,"from","seq","roomid");
        return new SuccessTip(chatModelList);
    }


    @ApiOperation("获取会话人员聊天详情")
    @ResponseBody
    @RequestMapping(value = "/getChatUserRecord",method = RequestMethod.POST)
    public Tip getChatUserRecord(@RequestParam("corpId") String corpId,@RequestParam("userId") String userId,
                                 @RequestParam("chatUser") String chatUser){
        List chatModelList = chatServer.getChatList(corpId, userId, chatUser);
        return new SuccessTip(chatModelList);
    }


    @ApiOperation("获取群会话详情")
    @ResponseBody
    @RequestMapping(value = "/getChatRoom",method = RequestMethod.POST)
    public Tip getChatRoom(@RequestParam("corpId") String corpId,@RequestParam("roomId") String roomId){
        ChatModel chatModel = new ChatModel(corpId);
        chatModel.setRoomid(roomId);
        List<ChatModel> chatModels = chatServer.getChatList(chatModel);
        return new SuccessTip(chatModels);
    }



    @ApiOperation("获取会话存档图片")
    @ResponseBody
    @RequestMapping(value = "/getAuditImageFile",method = RequestMethod.GET,produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] getAuditFile(@RequestParam("msgId") String msgId){
        try {
            InputStream inputStream = chatServer.getChatFile(msgId);
            if (null != inputStream) {
                byte[] bytes = LangUtil.getBytes(inputStream);
                inputStream.close();
                return bytes;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



    @ApiOperation("获取会话存文件")
    @ResponseBody
    @RequestMapping(value = "/getAuditFileFile",method = RequestMethod.GET)
    public ResponseEntity<String> getAuditFileFile(HttpServletResponse response, @RequestParam("msgId") String msgId){
        try {
            response.setContentType("application/vnd.ms-excel;charset=utf-8");
            response.setHeader("Content-Disposition","attachment;filename="+msgId);
            ServletOutputStream outputStream = response.getOutputStream();
            chatServer.getChatFile(msgId, outputStream);
            outputStream.flush();
            outputStream.close();
            return ResponseEntity.ok().body("SUCCESS");
        }catch (Exception e){
            logger.error(e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }






}
