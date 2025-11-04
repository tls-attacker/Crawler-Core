# Crawler Core

## Inner workings

### Controller

The following sequence diagrams show how the controller is started from `CommonMain.main`.
To aid readability, we split the inner workings of the Controller into two diagrams. The first one depicts how `PublishBulkScanJob` is triggered, which includes the scheduling feature.
The second diagram shows how `PublishBulkScanJob` then publishes the bulk scan.

```mermaid
%%{init: { 'sequence': {'mirrorActors':false} } }%%
sequenceDiagram
    participant main as CommonMain.main
    participant cmdConfig as :ControllerCommandConfig
    participant controller as :Controller
    participant quartz as Quartz Scheduler

    activate main
    alt Parse arguments using JCommander
        main ->> cmdConfig: setVariable(...)
    end

    create participant mq as :RabbitMqOrchestrationProvider
    main ->> mq: create
    create participant mongo as :MongoPersistenceProvider
    main ->> mongo: create

    main ->> controller: start()
    deactivate main
    activate controller
    controller ->>+ cmdConfig: getCronInterval
    cmdConfig -->>- controller: interval


    controller ->>+ cmdConfig: getTargetListProvider()
    create participant targetListProvider as targetListProvider:ITargetListProvider
    cmdConfig ->> targetListProvider: create
    cmdConfig -->>- controller: targetListProvider

    controller ->>+ cmdConfig: isMonitored
    cmdConfig -->>- controller: monitored
    opt if monitored
        create participant monitor as :ProgressMonitor
        controller ->> monitor: create
    end

    controller -) quartz: scheduleJob(interval, PublishScanJob.class)
    controller ->>+ quartz: start
    deactivate controller
    loop as per schedule (may repeat)
        create participant publishbulkscan as :PublishBulkScanJob
        quartz ->> publishbulkscan: create
        quartz ->>+ publishbulkscan: execute
    end

```

```mermaid
%%{init: { 'sequence': {'mirrorActors':false} } }%%
sequenceDiagram
    participant publishbulkscan as :PublishBulkScanJob.execute()
    participant mq as :RabbitMqOrchestrationProvider
    participant mongo as :MongoPersistenceProvider
    participant monitor as :ProgressMonitor

    activate publishbulkscan
    create participant bulkscan as bulkScan:BulkScan
    publishbulkscan ->> bulkscan: create
    publishbulkscan ->>+ bulkscan: setTargetsGiven(int)

    publishbulkscan ->> mongo: insertBulkScan(bulkScan)

    opt if monitored
        publishbulkscan ->>+ monitor: startMonitoringBulkScanProgress(bulkScan)
        create participant bmonitor as :BulkscanMonitor
        monitor ->> bmonitor: create
        monitor ->> mq: registerDoneNotificationConsumer(BulkScanMonitor)
    end
    
    create participant submitter as :JobSubmitter
    publishbulkscan ->> submitter: create

    loop for each targetString
        publishbulkscan ->>+ submitter: apply(targetString)
        %%create participant target as :ScanTarget
        %%submitter ->> target: fromTargetString(targetString)
        submitter ->> submitter: create ScanTarget and JobDescription
        alt parseable and resolvable
            submitter ->> mq: submitScanJob(jobDescription)
        else error
            submitter ->> mongo: insertScanResult(error, jobDescription)
        end
        submitter -->>- publishbulkscan: JobStatus

        alt if monitored
        mq -)+ bmonitor: consumeDoneNotification
        bmonitor ->>- bmonitor: print stats
        end
    end

    opt if monitored
        publishbulkscan -) monitor: stopMonitoringAndFinalizeBulkScan(bulkScan.get_id())
        deactivate monitor
    end
    deactivate publishbulkscan
```

### Worker

The following sequence diagrams show how the worker is started from `CommonMain.main`.
To aid readability, we split the inner workings of the Worker into two diagrams. The first one depicts how the `Worker` is triggered for each job.
The second diagram shows how each job is handled internally.

```mermaid
%%{init: { 'sequence': {'mirrorActors':false} } }%%
sequenceDiagram
    participant main as CommonMain.main
    participant cmdConfig as :WorkerCommandConfig

    activate main
    alt Parse arguments using JCommander
        main ->> cmdConfig: setVariable(...)
    end

    create participant mq as :RabbitMqOrchestrationProvider
    main ->> mq: create
    create participant mongo as :MongoPersistenceProvider
    main ->> mongo: create

    create participant worker as :Worker
    main ->> worker: create

    main ->> worker: start()
    deactivate main
    activate worker
    worker ->>- mq: registerScanJobConsumer(this::handleScanJob)

    loop for each scan job (possibly in parallel)
        mq ->>+ worker: handleScanJob(ScanJobDescription)
    end
```

```mermaid
%%{init: { 'sequence': {'mirrorActors':false} } }%%
sequenceDiagram
participant mq as :RabbitMqOrchestrationProvider
participant mongo as :MongoPersistenceProvider
participant worker as :Worker
participant workermanager as :BulkScanWorkerManager

        mq ->>+ worker: handleScanJob(ScanJobDescription)

        worker -)+ workermanager: handle(ScanJobDescription)


        worker -) worker: waitForScanResult
            note right of worker: Uses Thread pool<br>"crawler-worker: result handler"<br> to wait for result

        workermanager ->>+ workermanager: getBulkScanWorker(ScanJobDescription)

        opt if not exists
            create participant bulkScanWorker as :BulkScanWorker
            workermanager ->> bulkScanWorker: create(bulkScanId)
        end
        workermanager -->>- workermanager: BulkScanWorker


        workermanager ->>+ bulkScanWorker: handle(scanJobDescription.getScanTarget())
        bulkScanWorker ->>+ bulkScanWorker: scan(ScanTarget)
                note right of bulkScanWorker: Uses thread pool<br>"crawler-worker: scan executor"
                note right of bulkScanWorker: scan is abstract and is implemented by the concrete project
        bulkScanWorker -->>- bulkScanWorker: result
        bulkScanWorker -->>- workermanager: result
        workermanager -->>- worker: result
        alt success
            worker ->> mongo: insertScanResult(scanResult)
        else error
            worker ->> mongo: insertScanResult(errorResult)
                note right of worker: some error handling is performed <br> to store as much info as possible in the DB
        end
        worker -) mq: notifyOfDoneScanJob(scanJobDescription)

        deactivate worker
```

