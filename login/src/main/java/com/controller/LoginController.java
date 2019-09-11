package com.controller;


import co.paralleluniverse.fibers.Suspendable;
import com.annotation.Controllor;
import com.annotation.Rpc;
import com.controller.interceptor.HandlerExecutionChain;
import com.entry.PlayerEntry;
import com.entry.UserEntry;
import com.exception.StatusException;
import com.net.msg.LOGIN_MSG;
import com.pojo.Packet;
import com.rpc.interfaces.gameToBus.GameToBus;
import com.service.LoginService1;
import com.template.templates.type.TipType;
import com.util.CountUtil;
import com.util.ExceptionUtil;
import com.util.IdCreator;
import com.util.TipStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Controller
@Slf4j
public class LoginController extends BaseController implements GameToBus {
    @Autowired
    private LoginService1 loginService;

    @Controllor
    public Object login(UidContext context, LOGIN_MSG.CTG_LOGIN req) throws StatusException {
        String username = req.getUsername();
        String password = req.getPassword();
        String sessionId = req.getSessionId();

        LOGIN_MSG.GTC_LOGIN.Builder builder = LOGIN_MSG.GTC_LOGIN.newBuilder();
        CompletableFuture<UserEntry> user = loginService.login(username, password);

        builder.setSessionId(sessionId);
        user.whenCompleteAsync((userEntry, throwable) -> {

            if(!Objects.isNull(throwable)){
                ExceptionUtil.doThrow(throwable);
            }

            if (!Objects.isNull(userEntry)) {
                builder.setUid(userEntry.getId());
                builder.setResult(TipStatus.suc());
            } else {
                builder.setResult(TipStatus.fail(TipType.AccountError));
            }
            HandlerExecutionChain.applyPostHandle(
                    new Packet(context.getUid(),context.getId(),null,context.getFrom(),context.getGate(),context.getRpc())
                    ,builder.build());
        });
        return null;
    }


    @Controllor
    public void login(UidContext context, LOGIN_MSG.TEST_TIME req) {
        CountUtil.count();
        String username = req.getMsg();


    }

    @Controllor
    @Rpc(needResponse = true)
    @Suspendable
    @Override
    public String needResponse(String a) {
        //log.info("neeResponse : "+ a);
        return a;
    }

    @Controllor
    @Rpc(needResponse = false)
    @Suspendable
    @Override
    public Object noNeedResponse(String a) {
        log.info("异步");
        return null;
    }

    @Controllor
    @Rpc(needResponse = false)
    @Suspendable
    @Override
    public Object noNeedResponse0() {
        log.info("异步0");
        return null;
    }

    @Controllor
    @Rpc(needResponse = false)
    @Suspendable
    @Override
    public PlayerEntry aaa(String a) {
        System.out.println("有点慢啊");
        return new PlayerEntry(IdCreator.nextId(PlayerEntry.class));
    }
}
