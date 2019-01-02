package com.ruoyi.quartz.util;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.ScheduleConstants;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.bean.BeanUtils;
import com.ruoyi.quartz.domain.SysJob;
import com.ruoyi.quartz.domain.SysJobLog;
import com.ruoyi.quartz.service.ISysJobLogService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.Date;
import java.util.concurrent.Future;

/**
 * 定时任务处理
 *
 * @author ruoyi
 */
@DisallowConcurrentExecution
@Slf4j
public class ScheduleJob extends QuartzJobBean {

    private static final ThreadPoolTaskExecutor SERVICE = (ThreadPoolTaskExecutor)SpringContextUtil.getBean("publicThreadPool");

    private static final ISysJobLogService jobLogService = (ISysJobLogService) SpringContextUtil.getBean("sysJobLogServiceImpl");

    @Override
    protected void executeInternal(JobExecutionContext context){
        SysJob job = new SysJob();
        BeanUtils.copyBeanProp(job, context.getMergedJobDataMap().get(ScheduleConstants.TASK_PROPERTIES));

        SysJobLog jobLog = new SysJobLog();
        jobLog.setJobName(job.getJobName());
        jobLog.setJobGroup(job.getJobGroup());
        jobLog.setMethodName(job.getMethodName());
        jobLog.setMethodParams(job.getMethodParams());
        jobLog.setCreateTime(new Date());

        long startTime = System.currentTimeMillis();

        try {
            // 执行任务
            log.info("任务开始执行 - 名称：{} 方法：{}" , job.getJobName(), job.getMethodName());
            ScheduleRunnable task = new ScheduleRunnable(job.getJobName(), job.getMethodName(), job.getMethodParams());
            Future<?> future = SERVICE.submit(task);
            future.get();
            long times = System.currentTimeMillis() - startTime;
            // 任务状态 0：成功 1：失败
            jobLog.setStatus(Constants.SUCCESS);
            jobLog.setJobMessage(job.getJobName() + " 总共耗时：" + times + "毫秒");

            log.info("任务执行结束 - 名称：{} 耗时：{} 毫秒" , job.getJobName(), times);
        } catch (Exception e) {
            log.info("任务执行失败 - 名称：{} 方法：{}" , job.getJobName(), job.getMethodName());
            log.error("任务执行异常  - ：" , e);
            long times = System.currentTimeMillis() - startTime;
            jobLog.setJobMessage(job.getJobName() + " 总共耗时：" + times + "毫秒");
            // 任务状态 0：成功 1：失败
            jobLog.setStatus(Constants.FAIL);
            jobLog.setExceptionInfo(StringUtils.substring(e.getMessage(), 0, 2000));
        } finally {
            jobLogService.addJobLog(jobLog);
        }
    }
}
