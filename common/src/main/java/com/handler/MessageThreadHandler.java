package com.handler;

import com.Constant;
import com.controller.ControllerFactory;
import com.controller.ControllerHandler;
import com.controller.fun.Fun0;
import com.controller.fun.Fun1;
import com.controller.fun.Fun2;
import com.controller.fun.Fun3;
import com.controller.fun.Fun4;
import com.controller.interceptor.HandlerExecutionChain;
import com.exception.StatusException;
import com.exception.exceptionNeedSendToClient.ServerBusinessException;
import com.pojo.Packet;
import com.rpc.RpcHolder;
import com.rpc.RpcRequest;
import com.rpc.RpcResponse;
import com.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;

import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
public class MessageThreadHandler implements Runnable {
    // 执行器ID
    protected String handlerId;
    // 心跳频率10毫秒
    private int interval = 10;

    private StopWatch stopWatch = new StopWatch();

    protected final ConcurrentLinkedQueue<Packet> pulseQueues = new ConcurrentLinkedQueue<>();

    @Override
    public void run() {
        for (; ; ) {
            stopWatch.start();

            // 执行心跳
            pulse();

            stopWatch.stop();

            try {
                if (stopWatch.getTime() < interval) {
                    Thread.sleep(interval - stopWatch.getTime());
                } else {
                    Thread.sleep(1);
                }
            } catch (InterruptedException e) {
                log.error("线程中断",e);
            } finally {
                stopWatch.reset();
            }
        }
    }

    public void messageReceived(Packet msg) {
        pulseQueues.add(msg);
    }


    public void pulse() {
        while (!pulseQueues.isEmpty()) {
            ControllerHandler handler = null;
            Packet packet = null;
            String gate = null;
            boolean isRpc;
            RpcRequest rpcRequest = null;
            try {
                packet = pulseQueues.poll();
                final int cmdId = packet.getId();
                gate = packet.getGate();
                if(Constant.RPC_RESPONSE.equals(packet.getRpc())){
                    //log.info("收到 RPC 返回时间  "+ System.currentTimeMillis());
                    SpringUtils.getBean(RpcHolder.class).receiveResponse(ProtostuffUtil.deserializeObject(packet.getData(),RpcResponse.class));
                    return;
                }
                isRpc = StringUtil.contains(packet.getRpc(),Constant.RPC_REQUEST);
    
                if(!isRpc){
                    handler = ControllerFactory.getControllerMap().get(cmdId);
                    if (handler == null) {
                        throw new IllegalStateException("收到不存在的消息，消息ID=" + cmdId);
                    }
                }else {
                    rpcRequest=ProtostuffUtil.deserializeObject(packet.getData(),RpcRequest.class);
                    String key = rpcRequest.getClassName()+"_"+rpcRequest.getMethodName();
                    handler=ControllerFactory.getRpcControllerMap().get(key);
                    //log.info("收到 RPC 请求时间  "+ System.currentTimeMillis());
                    if (handler == null) {
                        throw new IllegalStateException("收到不存在的Rpc消息，消息KEY=" + key );
                    }
                }

                //拦截器前
                if (!HandlerExecutionChain.applyPreHandle(packet, handler)) {
                    continue;
                }
                Object result = null;
                Object[] m ;
                if(!isRpc){
                    m = handler.getMethodArgumentValues(packet);
                }else {
                    m = rpcRequest.getParameters();
                }
                
                //
                switch (handler.getFunType()) {
                    case Fun0:
                        result = (((Fun0) handler.getFun()).apply(handler.getAction()));
                        break;
                    case Fun1:
                        result = (((Fun1) handler.getFun()).apply(handler.getAction(), m[0]));
                        break;
                    case Fun2:
                        result = (((Fun2) handler.getFun()).apply(handler.getAction(), m[0], m[1]));
                        break;
                    case Fun3:
                        result = (((Fun3) handler.getFun()).apply(handler.getAction(), m[0], m[1], m[2]));
                        break;
                    case Fun4:
                        result = (((Fun4) handler.getFun()).apply(handler.getAction(), m[0], m[1], m[2], m[3]));
                        break;
                    default:
                        System.out.println("default");
                        break;
                }


                ////针对method的每个参数进行处理， 处理多参数,返回result（这是老的invoke执行controller 暂时废弃）
                //Message result = (Message) com.handler.invokeForController(packet);

                ////拦截器后
                if (!Objects.isNull(result)) {
                    HandlerExecutionChain.applyPostHandle(packet,result);
                }
            }

            catch (StatusException se) {
                ExceptionUtil.sendStatusExceptionToClient(handler,packet,gate,se);
            } catch (ServerBusinessException sbe) {
                // 业务报错，
                log.error("", sbe);
            } catch (Exception e) {
                // 系统报错
                log.error("", e);
            }
        }
    }



    public String getHandlerId() {
        return handlerId;
    }

    public void setHandlerId(String handlerId) {
        this.handlerId = handlerId;
    }


}