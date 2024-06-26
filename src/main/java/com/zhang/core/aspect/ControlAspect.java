package com.zhang.core.aspect;

import com.zhang.core.strategy.ControlManager;
import com.zhang.core.ControlProperties;
import com.zhang.core.ControlRule;
import com.zhang.exception.ControlException;
import com.zhang.utils.WebUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;


@Aspect
@Component
@Slf4j
public class ControlAspect {
   // private final Logger logger = LoggerFactory.getLogger(ControlAspect.class);
    @Autowired
    private ControlProperties controlProperties;
    @Autowired
    private ControlManager controlManager;


    @Pointcut("@annotation(frequencyControl)")
    public void controllerAspect(FrequencyControl frequencyControl) {}


    @Around(value = "controllerAspect(frequencyControl)", argNames = "joinPoint,frequencyControl")
    public Object aroundRateLimit(ProceedingJoinPoint joinPoint, FrequencyControl frequencyControl) throws Throwable {
        if (!controlProperties.isEnabled()){
            // 如果拒绝执行频率控制，执行被切入的方法
            return joinPoint.proceed();
        }
        String ipAddress = WebUtil.getIpAddress();
        String uri = WebUtil.getUri();
        ControlRule controlRule = finishControlRule(frequencyControl, uri, ipAddress);
        // 执行频率控制策略
        Long count = controlManager.controlStrategy(controlRule);
        if (count == 0) {
            log.info("接口拦截：{} 请求超过限制频率【{}次/{}s】,IP为{}", uri, controlRule.getMaxCount(), controlRule.getTimeRange(), ipAddress);
            throw new ControlException("请求过于频繁，请稍后重试");
        }
        // 执行被切入的方法
        return joinPoint.proceed();
    }



    /**
     * 整理频率控制的参数
     * @param frequencyControl
     * @return
     */
    public ControlRule finishControlRule(FrequencyControl frequencyControl, String uri, String ipAddress){
        // 获取注解中的属性值
        //参数可能为空，需要读取配置文件，如果都为空则用默认值
        ControlRule controlRule = ControlRule.builder()
                .timeRange(frequencyControl.timeRange())
                .timeUnit(frequencyControl.timeUnit())
                .maxCount(frequencyControl.maxCount())
                .build();

        //判断整理参数
        if (StringUtils.isBlank(frequencyControl.prefix())){
            //定义前缀，如果用户定义为空，则使用uri作为前缀
            if (StringUtils.isBlank(controlProperties.getControlRule().getPrefix())){
                controlRule.setPrefix(uri + "_" + ipAddress);
            }else{
                controlRule.setPrefix(controlRule.getPrefix() + "_" + ipAddress);
            }
        }
        if (frequencyControl.timeRange() <= 0){
            controlRule.setTimeRange(controlProperties.getControlRule().getTimeRange());
        }
        if (frequencyControl.timeUnit() == null){
            controlRule.setTimeUnit(controlProperties.getControlRule().getTimeUnit());
        }
        if (frequencyControl.maxCount() <= 0){
            controlRule.setMaxCount(controlProperties.getControlRule().getMaxCount());
        }
        return controlRule;
    }

}
