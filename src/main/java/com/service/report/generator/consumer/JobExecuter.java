package com.service.report.generator.consumer;

import com.service.report.generator.entity.JobModel;
import com.service.report.generator.experimental.ImplProvider;
import com.service.report.generator.repository.JobRepository;
import com.service.report.generator.service.ReportGeneratorServiceImpl;
import com.service.report.generator.tag.JobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Service
@Slf4j
@RequiredArgsConstructor
public class JobExecuter {

    private final ReportGeneratorServiceImpl reportGeneratorService;
    private final JobRepository jobRepository;

    private static final String DEFAULT_JOB_LOGGER_MESSAGE = "Executing job ({}) --- {}";

    private static void info(Long jobId, JobStatus status) {
        log.info(DEFAULT_JOB_LOGGER_MESSAGE, jobId, status);
    }

    private static void info(Long jobId, String message) {
        log.info(DEFAULT_JOB_LOGGER_MESSAGE + " --- {}", jobId, JobStatus.RUNNING, message);
    }

    private static void error(Long jobId, String message) {
        log.error(DEFAULT_JOB_LOGGER_MESSAGE, jobId, message);
    }


    @RabbitListener(queues = "#{standardQueue.getName()}")
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRED)
    public void executeStandardJobs(Long jobId) {

        executor(jobId, (job) ->{
            log.info("Started job("+job.getJobUid()+") execution");
            String outputFileName = reportGeneratorService.executeReportGeneration(job);
            log.info("Job("+job.getJobUid()+") executed successfully.");
        });

        // TODO
    }




    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRED)
    public void executor(Long jobId, ImplProvider<JobModel> implProvider) {
        JobModel job = jobRepository.findById(jobId).orElse(null);
        if (null ==  job) {
            error(jobId, "Job is missing while executing.");
            return;
        }

        info(jobId, JobStatus.RUNNING);
        try {
            implProvider.execute(job);// EXECUTION
            job.setStatus(JobStatus.SUCCESS);
            info(job.getId(), JobStatus.SUCCESS);
        } catch (Exception exception) {
            job.setStatus(JobStatus.FAILED);
            job.setAttempts(job.getAttempts() + 1);
            error(job.getId(), JobStatus.FAILED.name() + "\n" + ExceptionUtils.getStackTrace(exception));
        } finally {
            jobRepository.save(job);
        }


    }



}
