package com.chenry.cherrysharebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chenry.cherrysharebackend.mapper.MessageMapper;
import com.chenry.cherrysharebackend.model.dto.message.AddMessage;
import com.chenry.cherrysharebackend.model.entity.Message;
import com.chenry.cherrysharebackend.model.vo.MessageVO;
import com.chenry.cherrysharebackend.service.MessageService;

import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author 鹿梦
* @description 针对表【message(留言板表)】的数据库操作Service实现
* @createDate 2025-01-03 16:28:14
*/
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message>
    implements MessageService {


    @Override
    public Boolean addMessage(AddMessage addMessage) {
        Message message = new Message();
        message.setContent(addMessage.getContent());
        message.setIp(addMessage.getIp());
        return this.save(message);
    }

    @Override
    public List<MessageVO> getTop500() {
        return this.baseMapper.getTop500();
    }
}




