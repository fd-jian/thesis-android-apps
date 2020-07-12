from datetime import datetime
from datetime import timedelta
from datetime import timezone
from .models import M1_Question
import uuid

def getExecutionTimeMin(toaddMin):
    execution_time = datetime.utcnow() + timedelta(minutes=toaddMin)
    return execution_time.strftime("%Y/%m/%d %H:%M:%S")


def getExecutionTimeSec(toaddSec):
    execution_time =  datetime.utcnow() + timedelta(seconds=toaddSec)
    return str(execution_time)

def getExecutionTimeCronMin(toaddMin):
    execution_time = datetime.utcnow() + timedelta(minutes=toaddMin)
    return (execution_time.strftime("%H"), execution_time.strftime("%M"), execution_time.strftime("%S"),
            execution_time.strftime("%Y"), execution_time.strftime("%m"), execution_time.strftime("%d"),)

def getExecutionTimeCronSec(toaddSec):
    execution_time =  datetime.utcnow() + timedelta(seconds=toaddSec)
    return (execution_time.strftime("%H"), execution_time.strftime("%M"), execution_time.strftime("%S"),
            execution_time.strftime("%Y"), execution_time.strftime("%m"), execution_time.strftime("%d"),)


def transfromM1QuestiontoMsg(question):
    id  = str(uuid.uuid4())
    msg = ""
    msg += "False;"
    msg += id + ";"
    msg += "M1Django" + ";"

    if question.question_type == "TxtAns":
        msg += str(len(question.text_answer.split(","))) + ";"
    else:
        msg += str(question.numerical_answer_upper - question.numerical_answer_lower + 1) + ";"

    msg += question.question + ";"
    msg += ";"

    if question.question_type == "TxtAns":
        msg += question.text_answer + ";"
    else:
        for i in range(question.numerical_answer_lower, question.numerical_answer_upper + 1 ):
            msg += str(i) + ","
            msg = msg[:-1] + ";"

    return (msg, id)



