import random
import time
import datetime
from apscheduler.schedulers.background import BackgroundScheduler
from django_apscheduler.jobstores import DjangoJobStore, register_events, register_job
""""
scheduler = BackgroundScheduler()
scheduler.add_jobstore(DjangoJobStore(), "default")


def test_job1(text):
    print(text)

# Current Time
curr_time_sec = time.time() + 5;
curr_time = datetime.datetime.fromtimestamp(curr_time_sec)
scheduler.register_job(test_job1, 'date', run_date=curr_time, args=[curr_time_sec])


#@register_job(scheduler, "interval", seconds=1, replace_existing=True)
register_events(scheduler)
scheduler.start()



print("Scheduler started!")
"""
