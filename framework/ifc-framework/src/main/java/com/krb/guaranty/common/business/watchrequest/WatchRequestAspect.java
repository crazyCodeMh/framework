package com.krb.guaranty.common.business.watchrequest;


import com.krb.guaranty.common.constant.AppConstant;
import com.krb.guaranty.common.context.AppContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * @author owell
 * @date 2019/6/26 15:25
 */
@Slf4j
@Aspect
@Component
public class WatchRequestAspect {
    @Pointcut("@annotation(com.krb.guaranty.common.business.watchrequest.WatchRequest)")
    public void logPointCut() {}

    @Around("logPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        boolean writeResponseBody = false;
        WatchRequestEvent event = null;
        try{
            HttpServletRequest request = AppContext.getRequest();
            if(request != null){
                MethodSignature signature = (MethodSignature) point.getSignature();
                Method method = signature.getMethod();
                WatchRequest watchRequest= method.getAnnotation(WatchRequest.class);
                writeResponseBody = watchRequest.writeResponseBody();

                event = watchRequest.event().getConstructor(Object.class).newInstance(point.getThis());
                event.setType(watchRequest.type());
                event.setMethod(request.getMethod());
                event.setUrl(request.getRequestURI());
                event.setQueryString(request.getQueryString());
                if(watchRequest.writeHeaders() != null && watchRequest.writeHeaders().length>0){
                    event.setHeaders(new HashMap<>());
                    for (String headerName : watchRequest.writeHeaders()) {
                        event.getHeaders().put(headerName,request.getHeader(headerName));
                    }
                }
                event.setParameterMap(request.getParameterMap());
                event.setMethodArgs(point.getArgs());
                event.setRequestBody(AppContext.getThreadContext(AppConstant.REQUEST_BODY));

                //?????????????????????
                if(!writeResponseBody){
                    AppContext.publishEvent(event,false);
                }
            }else{
                log.error("??????????????????,??????????????????request,this:{}",point.getThis());
            }
        }catch (Exception e){
            log.error("error ... ",e);
        }

        //?????????????????????
        if(!writeResponseBody) {
            return point.proceed();
        } else {
            //??????????????????
            Object result = null;
            try{
                result = point.proceed();
            }finally {
                try{
                    if(event != null){
                        event.setResponseBody(result);
                        AppContext.publishEvent(event,false);
                    }
                }catch (Exception e){
                    log.error("error ... ",e);
                }
            }
            return result;
        }
    }
}
