package com.ezy.approval.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ezy.approval.entity.MessageGroupChat;
import com.ezy.approval.entity.MessageGroupChatUser;
import com.ezy.approval.mapper.MessageGroupChatMapper;
import com.ezy.approval.model.message.GroupChatCreateDTO;
import com.ezy.approval.service.IMessageGroupChatService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ezy.approval.service.IMessageGroupChatUserService;
import com.ezy.approval.service.IMessageService;
import com.ezy.common.model.CommonResult;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 群聊 服务实现类
 * </p>
 *
 * @author CaiXiaowei
 * @since 2020-07-27
 */
@Service
public class MessageGroupChatServiceImpl extends ServiceImpl<MessageGroupChatMapper, MessageGroupChat> implements IMessageGroupChatService {

    @Autowired
    private IMessageService messageService;
    @Autowired
    private IMessageGroupChatUserService messageGroupChatUserService;

    /**
     * 创建群聊
     *
     * @param groupChatCreateDTO
     * @return CommonResult
     * @author Caixiaowei
     * @updateTime 2020/9/15 15:04
     */
    @Override
    public CommonResult create(GroupChatCreateDTO groupChatCreateDTO) {
        String chatid = messageService.createGroupChat(groupChatCreateDTO);
        if (StrUtil.isNotEmpty(chatid)) {
            // db 群聊入库
            MessageGroupChat groupChat = new MessageGroupChat();
            groupChat.setChatid(chatid);
            groupChat.setName(groupChatCreateDTO.getName());
            groupChat.setOwner(groupChatCreateDTO.getOwner());
            groupChat.setCreateTime(LocalDateTime.now());
            groupChat.setUpdateTime(LocalDateTime.now());

            this.save(groupChat);

            // 聊成员入库
            List<MessageGroupChatUser> groupChatUserList = Lists.newArrayList();
            List<String> userlist = groupChatCreateDTO.getUserlist();
            for (String userid : userlist) {
                MessageGroupChatUser groupChatUser = new MessageGroupChatUser();
                groupChatUser.setChatid(chatid);
                groupChatUser.setUserid(userid);

                groupChatUserList.add(groupChatUser);
            }
            messageGroupChatUserService.saveBatch(groupChatUserList);

        }
        return null;
    }

    /**
     * 更新群聊
     *
     * @param groupChatCreateDTO@return
     * @author Caixiaowei
     * @updateTime 2020/9/15 18:05
     */
    @Override
    public CommonResult updateGroupChat(GroupChatCreateDTO groupChatCreateDTO) {
        String chatid = groupChatCreateDTO.getChatid();
        QueryWrapper<MessageGroupChat> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("chaid", chatid);
        MessageGroupChat groupChat = this.getOne(queryWrapper);

        return null;
    }
}
