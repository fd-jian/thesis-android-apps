from .schedulerhelper import *
from .models import M1_Question
from apscheduler.events import EVENT_JOB_ERROR, EVENT_JOB_EXECUTED
from django_apscheduler.jobstores import DjangoJobStore, register_events, register_job
from channels.layers import get_channel_layer
from asgiref.sync import async_to_sync
from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.executors.pool import ThreadPoolExecutor, ProcessPoolExecutor
from django.conf import settings


def my_listener(event):
    print("EventListener registered Event for: " + str(event.job_id))

def registerQuestionnaireSequence(groupname):

    for obj in M1_Question.objects.all():
        tmp = transfromM1QuestiontoMsg(obj)

        if (obj.schedule == "Begin"):
            exec = getExecutionTimeCronSec(2)
        elif(obj.schedule == "End"):
            exec = getExecutionTimeCronMin(1)
        else:
            cur = int(obj.schedule)
            exec = getExecutionTimeCronSec(cur)

        scheduler.add_job(
            executeQuestionnaireSequence,
            trigger='cron',
            year=str(int(exec[3])),
            month=str(int(exec[4])),
            day=str(int(exec[5])),
            hour=str(int(exec[0])),
            minute=str(int(exec[1])),
            second=str(int(exec[2])),
            args=[groupname, tmp[0]],
            id=tmp[1])

def executeQuestionnaireSequence(group_name, message):
    channel_layer = get_channel_layer()
    try:
        async_to_sync(channel_layer.group_send)(
            group_name,
            {
                'type': 'chat_message',
                'message': message
            }
        )
    except:
        pass


def test_hello():
    print("hallo")

scheduler = BackgroundScheduler(settings.SCHEDULER_CONFIG)
scheduler.add_listener(my_listener, EVENT_JOB_EXECUTED | EVENT_JOB_ERROR)
#register_events(scheduler)
scheduler.start()
print("Scheduler started!")

#scheduler.add_jobstore(jobStore, 'djangojobstore')

#executeQuestionnaireSequence("123","123")
"""jobstores = {
    'default': RedisJobStore(jobs_key='dispatched_trips_jobs', run_times_key='dispatched_trips_running', host='redis', port=6379)
}

executors = {
    'default': ThreadPoolExecutor(20),
    #'processpool': ProcessPoolExecutor(5)
}
job_defaults = {
    'coalesce': False,
    'max_instances': 3
}

jobStore = DjangoJobStore()
#jobStore.remove_all_jobs()
scheduler = BackgroundScheduler(jobstores=jobstores, executors=executors, job_defaults=job_defaults)"""